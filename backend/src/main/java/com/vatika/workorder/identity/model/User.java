package com.vatika.workorder.identity.model;

import com.vatika.workorder.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Column(nullable = false, unique = true, length = 320)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Role role;

    @Column(name = "client_id")
    private UUID clientId;

    @Column(nullable = false)
    private boolean enabled = true;

    public User(String email, String passwordHash, String displayName, Role role, UUID clientId) {
        if (role.isClientRole() && clientId == null) {
            throw new IllegalArgumentException("Client roles require a clientId");
        }
        if (!role.isClientRole() && clientId != null) {
            throw new IllegalArgumentException("Staff roles must not have a clientId");
        }
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.role = role;
        this.clientId = clientId;
    }
}