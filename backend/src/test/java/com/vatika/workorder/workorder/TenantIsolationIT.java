package com.vatika.workorder.workorder;

import com.vatika.workorder.AbstractIntegrationTest;
import com.vatika.workorder.client.model.Client;
import com.vatika.workorder.client.repository.ClientRepository;
import com.vatika.workorder.identity.dto.LoginRequest;
import com.vatika.workorder.identity.dto.TokenResponse;
import com.vatika.workorder.identity.model.Role;
import com.vatika.workorder.identity.model.User;
import com.vatika.workorder.identity.repository.UserRepository;
import com.vatika.workorder.workorder.dto.CreateWorkOrderRequest;
import com.vatika.workorder.workorder.dto.WorkOrderResponse;
import com.vatika.workorder.workorder.repository.WorkOrderRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class TenantIsolationIT extends AbstractIntegrationTest{

    @Autowired
    TestRestTemplate rest;

    private UUID acmeId;
    private UUID globexId;

    @Autowired
    ClientRepository clientRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    WorkOrderRepository workOrderRepository;
    @Autowired
    PasswordEncoder passwordEncoder;

    @BeforeEach
    void seed() {
        acmeId = UUID.randomUUID();
        globexId = UUID.randomUUID();

        String hash = passwordEncoder.encode("password123");

        acmeId = clientRepository.save(new Client("Acme Corp")).getId();
        globexId = clientRepository.save(new Client("Globex Inc")).getId();

        userRepository.save(new User("alice@acme.com", hash, "Alice", Role.CLIENT_USER, acmeId));
        userRepository.save(new User("bob@globex.com", hash, "Bob", Role.CLIENT_USER, globexId));

    }

    @AfterEach
    void cleanup() {
        workOrderRepository.deleteAll();
        userRepository.deleteAll();
        clientRepository.deleteAll();
    }

    private String login(String email){
        LoginRequest request =
                new LoginRequest(email, "password123");

        ResponseEntity<TokenResponse> response =
                rest.postForEntity(
                        "/auth/login",
                        request,
                        TokenResponse.class
                );

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.OK);

        return response.getBody().accessToken();
    }

    private UUID createWorkOrder(String token, String title){
        CreateWorkOrderRequest request = new CreateWorkOrderRequest(title,
                "Update bracket for v2 housing");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        ResponseEntity<WorkOrderResponse> response =
                rest.postForEntity(
                        "/work-orders",
                        new HttpEntity<>(request, headers),
                        WorkOrderResponse.class
                );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return  response.getBody().id();
    }

    private ResponseEntity<String> get(String path, String token){
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return rest.exchange(
                path,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);
    }

    @Test
    void clientCannotReadAnotherClientsWorkOrder(){
        String tokenA = login("alice@acme.com");
        String tokenB = login("bob@globex.com");

        UUID id = createWorkOrder(tokenA, "Bracket redesign");
        ResponseEntity<String> response = get("/work-orders/" + id, tokenB);

        assertThat(get("/work-orders/" + id, tokenA).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
