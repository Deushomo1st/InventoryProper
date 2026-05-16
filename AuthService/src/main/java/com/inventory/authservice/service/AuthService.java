package com.inventory.authservice.service;

import com.inventory.authservice.dto.LoginRequest;
import com.inventory.authservice.dto.LoginResponse;
import com.inventory.authservice.dto.RegisterRequest;
import com.inventory.authservice.model.*;
import com.inventory.authservice.repository.*;
import com.inventory.authservice.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final AppUserRepository userRepo;
    private final RoleRepository roleRepo;
    private final PermissionRepository permissionRepo;
    private final WarehouseAccessRepository warehouseAccessRepo;
    private final UserRoleRepository userRoleRepo;
    private final RolePermissionRepository rolePermissionRepo;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthService(AppUserRepository userRepo,
                       RoleRepository roleRepo,
                       PermissionRepository permissionRepo,
                       WarehouseAccessRepository warehouseAccessRepo,
                       UserRoleRepository userRoleRepo,
                       RolePermissionRepository rolePermissionRepo,
                       JwtUtil jwtUtil) {
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.permissionRepo = permissionRepo;
        this.warehouseAccessRepo = warehouseAccessRepo;
        this.userRoleRepo = userRoleRepo;
        this.rolePermissionRepo = rolePermissionRepo;
        this.jwtUtil = jwtUtil;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    public LoginResponse login(LoginRequest request) {
        // Unified credential error — don't leak which field is wrong (prevents email enumeration).
        AppUser user = userRepo.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Wrong email or password"));

        if (!user.getActive()) {
            throw new RuntimeException("Account is disabled");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Wrong email or password");
        }

        // Single-session enforcement — reject if another tab/device is currently active.
        // Relies on the 1s frontend heartbeat keeping lastSeenAt fresh; 3s threshold
        // gives two missed-pings of grace. A clean logout clears lastSeenAt so re-login
        // is instant; closing a tab without logging out incurs up to ~3s of lockout.
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(3);
        if (user.getLastSeenAt() != null && user.getLastSeenAt().isAfter(threshold)) {
            throw new RuntimeException("Already signed in from another tab or device. Log out there first or wait a few seconds and retry.");
        }

        List<String> roles = roleRepo.findRoleNamesByUserId(user.getId());
        List<Long> roleIds = getRoleIdsForUser(user.getId());

        List<String> permissions = permissionRepo.findPermissionCodesByRoleIds(roleIds);

        List<Long> warehouseIds = warehouseAccessRepo.findWarehouseIdsByUserId(user.getId());
        List<Integer> whs = warehouseIds.stream()
                .map(Long::intValue)
                .toList();

        String token = jwtUtil.generateToken(
                user.getId(),
                user.getEmail(),
                roles,
                permissions,
                whs,
                Boolean.TRUE.equals(user.getManager())
        );

        return new LoginResponse(token,
                new LoginResponse.UserDto(user.getId(), user.getEmail()));
    }

    public void register(RegisterRequest request) {
        if (userRepo.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        String hash = passwordEncoder.encode(request.getPassword());

        AppUser user = new AppUser(request.getEmail(), hash);
        userRepo.save(user);
    }

    public Map<String, Object> getUserInfoFromToken(String token) {
        Claims claims = jwtUtil.parseToken(token);

        Long userId = jwtUtil.getUserId(claims);
        String email = jwtUtil.getEmail(claims);
        List<String> roles = jwtUtil.getRoles(claims);
        List<String> permissions = jwtUtil.getPermissions(claims);
        List<Integer> warehouses = jwtUtil.getWarehouses(claims);
        boolean manager = jwtUtil.getManager(claims);

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userId", userId);
        userInfo.put("email", email);
        userInfo.put("roles", roles);
        userInfo.put("permissions", permissions);
        userInfo.put("warehouses", warehouses);
        userInfo.put("manager", manager);

        return userInfo;
    }

    public List<Map<String, Object>> getAllUsers() {
        return userRepo.findAll().stream()
                .map(this::mapUserToDto)
                .collect(Collectors.toList());
    }

    public Map<String, Object> getUserById(Long id) {
        AppUser user = userRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));

        Map<String, Object> userDto = mapUserToDto(user);

        List<String> roles = roleRepo.findRoleNamesByUserId(user.getId());
        List<Long> warehouseIds = warehouseAccessRepo.findWarehouseIdsByUserId(user.getId());

        userDto.put("roles", roles);
        userDto.put("warehouses", warehouseIds);

        return userDto;
    }

    private Map<String, Object> mapUserToDto(AppUser user) {
        // Tight window — frontend heartbeats every 1s, so 3s gives two missed
        // pings of grace before flipping to offline. Stale lastSeenAt = offline.
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(3);
        boolean isOnline = user.getLastSeenAt() != null
                && user.getLastSeenAt().isAfter(threshold);

        Map<String, Object> userDto = new HashMap<>();
        userDto.put("id", user.getId());
        userDto.put("email", user.getEmail());
        userDto.put("active", user.getActive());
        userDto.put("createdAt", user.getCreatedAt());
        userDto.put("lastSeenAt", user.getLastSeenAt());
        userDto.put("online", isOnline);
        userDto.put("manager", Boolean.TRUE.equals(user.getManager()));

        return userDto;
    }

    @Transactional
    public void updateUser(Long id, Map<String, Object> updates) {
        AppUser user = userRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));

        if (updates.containsKey("email")) {
            user.setEmail((String) updates.get("email"));
        }
        if (updates.containsKey("active")) {
            user.setActive((Boolean) updates.get("active"));
        }
        if (updates.containsKey("manager")) {
            user.setManager((Boolean) updates.get("manager"));
        }
        if (updates.containsKey("password")) {
            String newPassword = (String) updates.get("password");
            user.setPasswordHash(passwordEncoder.encode(newPassword));
        }

        userRepo.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        AppUser user = userRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));

        userRepo.delete(user);
    }

    // Role CRUD

    public List<Map<String, Object>> getAllRoles() {
        return roleRepo.findAll().stream()
                .map(this::mapRoleToDto)
                .collect(Collectors.toList());
    }

    public Map<String, Object> getRoleById(Long id) {
        Role role = roleRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Role not found: " + id));
        return mapRoleToDto(role);
    }

    private Map<String, Object> mapRoleToDto(Role role) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", role.getId());
        dto.put("name", role.getName());
        dto.put("active", Boolean.TRUE.equals(role.getActive()));
        return dto;
    }

    @Transactional
    public void createRole(String name, boolean active) {
        if (roleRepo.existsByName(name)) {
            throw new RuntimeException("Role already exists: " + name);
        }
        Role role = new Role(name);
        role.setActive(active);
        roleRepo.save(role);
    }

    @Transactional
    public void updateRole(Long id, String name, Boolean active) {
        Role role = roleRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Role not found: " + id));
        if (!role.getName().equals(name) && roleRepo.existsByName(name)) {
            throw new RuntimeException("Role already exists: " + name);
        }
        role.setName(name);
        if (active != null) role.setActive(active);
        roleRepo.save(role);
    }

    @Transactional
    public void deleteRole(Long id) {
        Role role = roleRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Role not found: " + id));
        // Cascade: remove join rows so FK constraints don't fail
        userRoleRepo.deleteAllByRoleId(id);
        rolePermissionRepo.deleteAllByRoleId(id);
        roleRepo.delete(role);
    }

    public List<Map<String, Object>> getRolesForUser(Long userId) {
        List<Long> roleIds = roleRepo.findRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) return List.of();
        return roleRepo.findAllById(roleIds).stream()
                .map(this::mapRoleToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void assignRoleToUser(Long userId, Long roleId) {
        if (!userRepo.existsById(userId)) {
            throw new RuntimeException("User not found: " + userId);
        }
        if (!roleRepo.existsById(roleId)) {
            throw new RuntimeException("Role not found: " + roleId);
        }

        if (userRoleRepo.findByUserIdAndRoleId(userId, roleId).isPresent()) {
            throw new RuntimeException("Role already assigned to user");
        }

        UserRole userRole = new UserRole(userId, roleId);
        userRoleRepo.save(userRole);
    }

    @Transactional
    public void removeRoleFromUser(Long userId, Long roleId) {
        userRoleRepo.deleteByUserIdAndRoleId(userId, roleId);
    }

    // Permission CRUD

    public List<Map<String, Object>> getAllPermissions() {
        return permissionRepo.findAll().stream()
                .map(this::mapPermissionToDto)
                .collect(Collectors.toList());
    }

    public Map<String, Object> getPermissionById(Long id) {
        Permission permission = permissionRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Permission not found: " + id));
        return mapPermissionToDto(permission);
    }

    private Map<String, Object> mapPermissionToDto(Permission permission) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", permission.getId());
        dto.put("code", permission.getCode());
        dto.put("active", Boolean.TRUE.equals(permission.getActive()));
        return dto;
    }

    @Transactional
    public void createPermission(String code, boolean active) {
        if (permissionRepo.existsByCode(code)) {
            throw new RuntimeException("Permission already exists: " + code);
        }
        Permission p = new Permission(code);
        p.setActive(active);
        permissionRepo.save(p);
    }

    @Transactional
    public void updatePermission(Long id, String code, Boolean active) {
        Permission permission = permissionRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Permission not found: " + id));
        if (!permission.getCode().equals(code) && permissionRepo.existsByCode(code)) {
            throw new RuntimeException("Permission already exists: " + code);
        }
        permission.setCode(code);
        if (active != null) permission.setActive(active);
        permissionRepo.save(permission);
    }

    @Transactional
    public void deletePermission(Long id) {
        Permission permission = permissionRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Permission not found: " + id));
        rolePermissionRepo.deleteAllByPermissionId(id);
        permissionRepo.delete(permission);
    }

    public List<Map<String, Object>> getPermissionsForRole(Long roleId) {
        return permissionRepo.findByRoleId(roleId).stream()
                .map(this::mapPermissionToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void assignPermissionToRole(Long roleId, Long permissionId) {
        if (!roleRepo.existsById(roleId)) {
            throw new RuntimeException("Role not found: " + roleId);
        }
        if (!permissionRepo.existsById(permissionId)) {
            throw new RuntimeException("Permission not found: " + permissionId);
        }

        if (rolePermissionRepo.findByRoleIdAndPermissionId(roleId, permissionId).isPresent()) {
            throw new RuntimeException("Permission already assigned to role");
        }

        RolePermission rolePermission = new RolePermission(roleId, permissionId);
        rolePermissionRepo.save(rolePermission);
    }

    @Transactional
    public void removePermissionFromRole(Long roleId, Long permissionId) {
        rolePermissionRepo.deleteByRoleIdAndPermissionId(roleId, permissionId);
    }

    @Transactional
    public void grantWarehouseAccess(Long userId, Long warehouseId) {
        if (!userRepo.existsById(userId)) {
            throw new RuntimeException("User not found: " + userId);
        }

        WarehouseAccess access = new WarehouseAccess(userId, warehouseId);
        warehouseAccessRepo.save(access);
    }

    @Transactional
    public void revokeWarehouseAccess(Long userId, Long warehouseId) {
        warehouseAccessRepo.deleteByUserIdAndWarehouseId(userId, warehouseId);
    }

    @Transactional
    public void updateLastSeenAt(Long userId) {
        userRepo.findById(userId).ifPresent(user -> {
            user.setLastSeenAt(LocalDateTime.now());
            userRepo.save(user);
        });
    }

    @Transactional
    public void endSession(Long userId) {
        // Called on explicit logout. Clears lastSeenAt so the user can immediately
        // re-login from a different tab/device without waiting out the 90s threshold.
        userRepo.findById(userId).ifPresent(user -> {
            user.setLastSeenAt(null);
            userRepo.save(user);
        });
    }

    private List<Long> getRoleIdsForUser(Long userId) {
        return roleRepo.findRoleIdsByUserId(userId);
    }
}