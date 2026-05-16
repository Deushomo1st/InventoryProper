package com.inventory.authservice.model;

import jakarta.persistence.*;

@Entity
@Table(name = "permission", schema = "auth")
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Column(nullable = false)
    private Boolean active = true;

    public Permission() {}

    public Permission(String code) {
        this.code = code;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}