package com.inventory.authservice.controller;

import com.inventory.authservice.dto.LoginRequest;
import com.inventory.authservice.dto.LoginResponse;
import com.inventory.authservice.dto.RegisterRequest;
import com.inventory.authservice.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            LoginResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/register")
    @PreAuthorize("hasAuthority('MANAGER')")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            authService.register(request);
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(Map.of("message", "Registration successful"));
        } catch (RuntimeException e) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(org.springframework.security.core.Authentication auth) {
        if (auth != null && auth.getPrincipal() instanceof Long userId) {
            authService.endSession(userId);
        }
        return ResponseEntity.ok(Map.of("message", "Logged out"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            String token = authHeader.substring(7);
            Map<String, Object> userInfo = authService.getUserInfoFromToken(token);

            // Update last_seen_at
            Object userIdObj = userInfo.get("userId");
            Long userId = userIdObj instanceof Number
                    ? ((Number) userIdObj).longValue()
                    : Long.parseLong(userIdObj.toString());
            authService.updateLastSeenAt(userId);

            return ResponseEntity.ok(userInfo);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    // User CRUD endpoints
    
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        return ResponseEntity.ok(authService.getAllUsers());
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(authService.getUserById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id,
                                        @RequestBody Map<String, Object> updates,
                                        org.springframework.security.core.Authentication auth) {
        boolean callerIsManager = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "MANAGER".equals(a.getAuthority()));
        Long callerId = (auth != null && auth.getPrincipal() instanceof Long)
                ? (Long) auth.getPrincipal() : null;

        // Scope gate: non-managers can only edit their own account.
        if (!callerIsManager && !id.equals(callerId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "You can only edit your own account"));
        }

        // Field gate: only a manager can flip the manager flag — even on themselves.
        if (updates.containsKey("manager") && !callerIsManager) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Only managers can change manager status"));
        }

        try {
            authService.updateUser(id, updates);
            return ResponseEntity.ok(Map.of("message", "User updated successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasAuthority('MANAGER')")
    public ResponseEntity<?> deleteUser(@PathVariable Long id,
                                        org.springframework.security.core.Authentication auth) {
        // Self-delete guard — an admin can't delete the account they're logged in as.
        // Deactivating is the safe alternative; deletion is permanent and breaks FKs.
        if (auth != null && auth.getPrincipal() instanceof Long currentUserId
                && id.equals(currentUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "You cannot delete your own account. Deactivate it instead."));
        }
        try {
            authService.deleteUser(id);
            return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    // Role CRUD endpoints

    @GetMapping("/roles")
    public ResponseEntity<?> getAllRoles() {
        return ResponseEntity.ok(authService.getAllRoles());
    }

    @GetMapping("/roles/{id}")
    public ResponseEntity<?> getRoleById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(authService.getRoleById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/roles")
    @PreAuthorize("hasAuthority('MANAGER')")
    public ResponseEntity<?> createRole(@RequestBody Map<String, Object> body) {
        try {
            String name = (String) body.get("name");
            if (name == null || name.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Role name is required"));
            }
            boolean active = body.get("active") == null || (Boolean) body.get("active");
            authService.createRole(name.trim(), active);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("message", "Role created"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('MANAGER')")
    public ResponseEntity<?> updateRole(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            String name = (String) body.get("name");
            if (name == null || name.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Role name is required"));
            }
            Boolean active = body.containsKey("active") ? (Boolean) body.get("active") : null;
            authService.updateRole(id, name.trim(), active);
            return ResponseEntity.ok(Map.of("message", "Role updated"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('MANAGER')")
    public ResponseEntity<?> deleteRole(@PathVariable Long id) {
        try {
            authService.deleteRole(id);
            return ResponseEntity.ok(Map.of("message", "Role deleted"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    // Role assignment endpoints

    @GetMapping("/users/{userId}/roles")
    public ResponseEntity<?> getUserRoles(@PathVariable Long userId) {
        return ResponseEntity.ok(authService.getRolesForUser(userId));
    }

    @PostMapping("/users/{userId}/roles/{roleId}")
    @PreAuthorize("hasAuthority('MANAGER')")
    public ResponseEntity<?> assignRoleToUser(@PathVariable Long userId, @PathVariable Long roleId) {
        try {
            authService.assignRoleToUser(userId, roleId);
            return ResponseEntity.ok(Map.of("message", "Role assigned successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/users/{userId}/roles/{roleId}")
    @PreAuthorize("hasAuthority('MANAGER')")
    public ResponseEntity<?> removeRoleFromUser(@PathVariable Long userId, @PathVariable Long roleId) {
        try {
            authService.removeRoleFromUser(userId, roleId);
            return ResponseEntity.ok(Map.of("message", "Role removed successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    // Permission CRUD endpoints

    @GetMapping("/permissions")
    public ResponseEntity<?> getAllPermissions() {
        return ResponseEntity.ok(authService.getAllPermissions());
    }

    @GetMapping("/permissions/{id}")
    public ResponseEntity<?> getPermissionById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(authService.getPermissionById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/permissions")
    @PreAuthorize("hasAuthority('MANAGER')")
    public ResponseEntity<?> createPermission(@RequestBody Map<String, Object> body) {
        try {
            String code = (String) body.get("code");
            if (code == null || code.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Permission code is required"));
            }
            boolean active = body.get("active") == null || (Boolean) body.get("active");
            authService.createPermission(code.trim(), active);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("message", "Permission created"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/permissions/{id}")
    @PreAuthorize("hasAuthority('MANAGER')")
    public ResponseEntity<?> updatePermission(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            String code = (String) body.get("code");
            if (code == null || code.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Permission code is required"));
            }
            Boolean active = body.containsKey("active") ? (Boolean) body.get("active") : null;
            authService.updatePermission(id, code.trim(), active);
            return ResponseEntity.ok(Map.of("message", "Permission updated"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/permissions/{id}")
    @PreAuthorize("hasAuthority('MANAGER')")
    public ResponseEntity<?> deletePermission(@PathVariable Long id) {
        try {
            authService.deletePermission(id);
            return ResponseEntity.ok(Map.of("message", "Permission deleted"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    // Permission assignment endpoints

    @GetMapping("/roles/{roleId}/permissions")
    public ResponseEntity<?> getRolePermissions(@PathVariable Long roleId) {
        return ResponseEntity.ok(authService.getPermissionsForRole(roleId));
    }

    @PostMapping("/roles/{roleId}/permissions/{permissionId}")
    @PreAuthorize("hasAuthority('MANAGER')")
    public ResponseEntity<?> assignPermissionToRole(@PathVariable Long roleId, @PathVariable Long permissionId) {
        try {
            authService.assignPermissionToRole(roleId, permissionId);
            return ResponseEntity.ok(Map.of("message", "Permission assigned successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/roles/{roleId}/permissions/{permissionId}")
    @PreAuthorize("hasAuthority('MANAGER')")
    public ResponseEntity<?> removePermissionFromRole(@PathVariable Long roleId, @PathVariable Long permissionId) {
        try {
            authService.removePermissionFromRole(roleId, permissionId);
            return ResponseEntity.ok(Map.of("message", "Permission removed successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    // Warehouse access endpoints

    @PostMapping("/users/{userId}/warehouses/{warehouseId}")
    @PreAuthorize("hasAuthority('MANAGER')")
    public ResponseEntity<?> grantWarehouseAccess(@PathVariable Long userId, @PathVariable Long warehouseId) {
        try {
            authService.grantWarehouseAccess(userId, warehouseId);
            return ResponseEntity.ok(Map.of("message", "Warehouse access granted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/users/{userId}/warehouses/{warehouseId}")
    @PreAuthorize("hasAuthority('MANAGER')")
    public ResponseEntity<?> revokeWarehouseAccess(@PathVariable Long userId, @PathVariable Long warehouseId) {
        try {
            authService.revokeWarehouseAccess(userId, warehouseId);
            return ResponseEntity.ok(Map.of("message", "Warehouse access revoked successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }
}