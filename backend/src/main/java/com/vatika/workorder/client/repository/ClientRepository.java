package com.vatika.workorder.client.repository;

import com.vatika.workorder.client.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ClientRepository extends JpaRepository<Client, UUID> {
    Optional<Client> findByName(String name);
    boolean existsByName(String name);
}
