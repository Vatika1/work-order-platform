package com.vatika.workorder.workorder.controller;

import com.vatika.workorder.AbstractIntegrationTest;
import com.vatika.workorder.client.model.Client;
import com.vatika.workorder.client.repository.ClientRepository;
import com.vatika.workorder.identity.dto.LoginRequest;
import com.vatika.workorder.identity.dto.TokenResponse;
import com.vatika.workorder.identity.model.Role;
import com.vatika.workorder.identity.model.User;
import com.vatika.workorder.identity.repository.UserRepository;
import com.vatika.workorder.shared.dto.PageResponse;
import com.vatika.workorder.workorder.dto.CreateWorkOrderRequest;
import com.vatika.workorder.workorder.dto.TransitionRequest;
import com.vatika.workorder.workorder.dto.WorkOrderResponse;
import com.vatika.workorder.workorder.model.WorkOrderStatus;
import com.vatika.workorder.workorder.repository.WorkOrderRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class WorkOrderControllerIT extends AbstractIntegrationTest {

    @Autowired
    TestRestTemplate rest;

    private UUID acmeId;

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
        String hash = passwordEncoder.encode("password123");
        acmeId = clientRepository.save(new Client("Acme Corp")).getId();
        userRepository.save(new User("alice@acme.com", hash, "Alice", Role.CLIENT_USER, acmeId));
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

    private ResponseEntity<String> transition(UUID id, WorkOrderStatus target, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return rest.postForEntity(
                "/work-orders/" + id + "/transitions",
                new HttpEntity<>(new TransitionRequest(target), headers),
                String.class);
    }

    @Test
    void transitionsDraftToSubmitted() {
        String token = login("alice@acme.com");
        UUID id = createWorkOrder(token, "Bracket redesign");

        ResponseEntity<String> response = transition(id, WorkOrderStatus.SUBMITTED, token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"status\":\"SUBMITTED\"");
    }

    @Test
    void rejectsIllegalTransition() {
        String token = login("alice@acme.com");
        UUID id = createWorkOrder(token, "Bracket redesign");
        ResponseEntity<String> response = transition(id, WorkOrderStatus.COMPLETED, token);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    private ResponseEntity<PageResponse<WorkOrderResponse>> list(String token, String query) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return rest.exchange(
                "/work-orders" + query,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {});
    }

    @Test
    void list_returnsOnlyOwnTenantsWorkOrders_newestFirst(){
        UUID globexId = clientRepository.save(new Client("Globex")).getId();
        userRepository.save(new User("bob@globex.com", passwordEncoder.encode("password123"),
                "Bob", Role.CLIENT_USER, globexId));
        String alice = login("alice@acme.com");
        String bob = login("bob@globex.com");
        createWorkOrder(alice, "First");
        createWorkOrder(alice, "Second");
        createWorkOrder(bob, "Globex order");
        ResponseEntity<PageResponse<WorkOrderResponse>> response = list(alice,"");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().totalElements()).isEqualTo(2);
        assertThat(response.getBody().content())
                .extracting(WorkOrderResponse::title)
                .containsExactly("Second", "First");
        assertThat(response.getBody().page()).isEqualTo(0);
        assertThat((response.getBody().size())).isEqualTo(20);
    }

    @Test
    void list_filtersByStatus(){
        String alice = login("alice@acme.com");
        UUID submitted = createWorkOrder(alice, "Submitted one");
        createWorkOrder(alice, "Draft one");
        transition(submitted, WorkOrderStatus.SUBMITTED, alice);
        ResponseEntity<PageResponse<WorkOrderResponse>> response = list(alice,"?status=SUBMITTED");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().totalElements()).isEqualTo(1);
        assertThat(response.getBody().content())
                .extracting(WorkOrderResponse::id)
                .containsExactly(submitted);
    }

    @Test
    void list_paginates(){
        String alice = login("alice@acme.com");
        createWorkOrder(alice, "A");
        createWorkOrder(alice, "B");
        createWorkOrder(alice, "C");
        ResponseEntity<PageResponse<WorkOrderResponse>> response = list(alice,"?page=0&size=2");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().content()).hasSize(2);
        assertThat(response.getBody().totalElements()).isEqualTo(3);
        assertThat(response.getBody().totalPages()).isEqualTo(2);
        assertThat(response.getBody().last()).isFalse();
    }

    private ResponseEntity<String> listRaw(String token, String query){
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return rest.exchange(
                "/work-orders" + query,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                String.class);
    }

    @Test
    void list_rejectsUnknownSortField(){
        String alice = login("alice@acme.com");
        ResponseEntity<String> response = listRaw(alice,"/work-orders?sort=password,asc");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void list_rejectsPageSizeOver100() {
        String token = login("alice@acme.com");

        ResponseEntity<String> response = listRaw(token, "?size=500");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

}
