package com.electdept.protectioncoordinationservice.service;

import com.electdept.protectioncoordinationservice.model.ProtectionScheme;
import com.electdept.protectioncoordinationservice.repository.ProtectionSchemeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProtectionSchemeService {
    private final ProtectionSchemeRepository repo;

    public ProtectionSchemeService(ProtectionSchemeRepository repo) {
        this.repo = repo;
    }

    public List<Map<String, Object>> getAll() {
        return repo.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public Map<String, Object> getById(Long id) {
        ProtectionScheme customer = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("ProtectionScheme not found: " + id));
        return toDto(customer);
    }

    @Transactional
    public Map<String, Object> create(Map<String, Object> body) {
        ProtectionScheme customer = new ProtectionScheme();
        customer.setCode((String) body.get("code"));
        customer.setName((String) body.get("name"));
        customer.setEmail((String) body.get("email"));
        customer.setPhone((String) body.get("phone"));
        customer.setAddress((String) body.get("address"));
        customer.setPaymentTerms((String) body.get("paymentTerms"));
        
        Object creditLimit = body.get("creditLimit");
        if (creditLimit != null) {
            customer.setCreditLimit(new java.math.BigDecimal(creditLimit.toString()));
        }
        
        Boolean active = (Boolean) body.get("active");
        customer.setActive(active != null ? active : true);
        customer.setCreatedAt(Instant.now());
        
        return toDto(repo.save(customer));
    }

    @Transactional
    public Map<String, Object> update(Long id, Map<String, Object> body) {
        ProtectionScheme customer = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("ProtectionScheme not found: " + id));
        
        if (body.containsKey("code")) customer.setCode((String) body.get("code"));
        if (body.containsKey("name")) customer.setName((String) body.get("name"));
        if (body.containsKey("email")) customer.setEmail((String) body.get("email"));
        if (body.containsKey("phone")) customer.setPhone((String) body.get("phone"));
        if (body.containsKey("address")) customer.setAddress((String) body.get("address"));
        if (body.containsKey("paymentTerms")) customer.setPaymentTerms((String) body.get("paymentTerms"));
        
        if (body.containsKey("creditLimit")) {
            Object creditLimit = body.get("creditLimit");
            customer.setCreditLimit(new java.math.BigDecimal(creditLimit.toString()));
        }
        
        if (body.containsKey("active")) customer.setActive((Boolean) body.get("active"));
        
        return toDto(repo.save(customer));
    }

    @Transactional
    public void delete(Long id) {
        ProtectionScheme customer = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("ProtectionScheme not found: " + id));
        repo.delete(customer);
    }

    private Map<String, Object> toDto(ProtectionScheme c) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("id", c.getId());
        dto.put("code", c.getCode());
        dto.put("name", c.getName());
        dto.put("email", c.getEmail());
        dto.put("phone", c.getPhone());
        dto.put("address", c.getAddress());
        dto.put("paymentTerms", c.getPaymentTerms());
        dto.put("creditLimit", c.getCreditLimit());
        dto.put("active", c.getActive());
        dto.put("createdAt", c.getCreatedAt());
        return dto;
    }
}
