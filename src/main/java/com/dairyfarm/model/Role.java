package com.dairyfarm.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "roles", uniqueConstraints = @UniqueConstraint(columnNames = "name"))
public class Role extends BaseEntity {
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private UserRole name;
    public Role() {}
    public Role(UserRole name) { this.name = name; }
    public UserRole getName() { return name; }
    public void setName(UserRole name) { this.name = name; }
}
