package com.vatika.workorder.workorder.controller;

import com.vatika.workorder.AbstractIntegrationTest;
import com.vatika.workorder.client.model.Client;
import com.vatika.workorder.client.repository.ClientRepository;
import com.vatika.workorder.identity.dto.LoginRequest;
import com.vatika.workorder.identity.dto.TokenResponse;
import com.vatika.workorder.identity.model.Role;
import com.vatika.workorder.identity.model.User;
import com.vatika.workorder.identity.repository.UserRepository;
import com.vatika.workorder.workorder.dto.WorkOrderResponse;
import com.vatika.workorder.workorder.model.WorkOrder;
import com.vatika.workorder.workorder.model.WorkOrderStatus;
import com.vatika.workorder.workorder.repository.WorkOrderRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

public class StaffWorkOrderControllerIT extends AbstractIntegrationTest {

    private static final String CLAIM_URL = "/staff/work-orders/claim-next";

    @Autowired
    TestRestTemplate rest;
    @Autowired
    WorkOrderRepository workOrderRepository;
    @Autowired
    UserRepository userRepository;
    @Autowired
    ClientRepository clientRepository;
    @Autowired
    PasswordEncoder passwordEncoder;

    private UUID acmeId;
    private UUID clientUserId;
    private UUID staffId;

    @BeforeEach
    void seed() {
        String hash = passwordEncoder.encode("password123");
        acmeId = clientRepository.save(new Client("Acme Corp")).getId();
        clientUserId = userRepository.save(new User("alice@acme.com", hash, "Alice", Role.CLIENT_USER, acmeId)).getId();
        staffId = userRepository.save(new User("staff@test.com", hash,"Staff", Role.STAFF, null)).getId();
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

    private UUID createSubmitted(String title, Instant createdAt) {
        return workOrderRepository.save(WorkOrder.builder()
                .id(UUID.randomUUID())
                .clientId(acmeId)
                .title(title)
                .status(WorkOrderStatus.SUBMITTED)
                .createdBy(clientUserId)
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build()).getId();
    }

    private <T> ResponseEntity<T> claim(String token, Class<T> type) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return rest.exchange(CLAIM_URL, HttpMethod.POST, new HttpEntity<>(headers), type);
    }

    @Test
    void claimNextReturnsOldestSubmittedOrderAndAssignsIt(){
        UUID oldest = createSubmitted("Older WO", Instant.now().minusSeconds(300));
        UUID newest = createSubmitted("Newest", Instant.now().minusSeconds(100));
        ResponseEntity<WorkOrderResponse> response = claim(login("staff@test.com"), WorkOrderResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().id()).isEqualTo(oldest);

        WorkOrder saved = workOrderRepository.findById(oldest).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(WorkOrderStatus.IN_PROGRESS);
        assertThat(saved.getAssigneeId()).isEqualTo(staffId);
    }

    @Test
    void concurrentClaimsNeverReturnTheSameOrder() throws Exception{
        Set<UUID> created = new HashSet<>();
        for (int i = 0; i < 5; i++) {
            created.add(createSubmitted("Order " + i, Instant.now().minusSeconds(100 - i)));
        }

        String token = login("staff@test.com");

        int threads = 20;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch startGate = new CountDownLatch(1);
        List<Future<ResponseEntity<String>>> futures = new ArrayList<>();

        for (int i = 0; i < 20; i++){
            futures.add(pool.submit(() -> {
                startGate.await();
                return claim(token, String.class);
                    })
            );
        }
        startGate.countDown();

        int ok = 0;
        int notFound = 0;

        for (Future<ResponseEntity<String>> f : futures){
            HttpStatusCode status = f.get(30, TimeUnit.SECONDS).getStatusCode();
            if (status == HttpStatus.OK) {
                ok++;
            } else if (status == HttpStatus.NOT_FOUND) {
                notFound++;
            }
        }
        pool.shutdown();

        assertThat(ok).isEqualTo(5);
        assertThat(notFound).isEqualTo(15);

        // every created order was claimed once, by the staff user
        for (UUID id : created) {
            WorkOrder wo = workOrderRepository.findById(id).orElseThrow();
            assertThat(wo.getStatus()).isEqualTo(WorkOrderStatus.IN_PROGRESS);
            assertThat(wo.getAssigneeId()).isEqualTo(staffId);
        }
    }

    @Test
    void claimNextReturns404WhenQueueIsEmpty() {
        ResponseEntity<String> response = claim(login("staff@test.com"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void clientUserGets403() {
        createSubmitted("Not yours", Instant.now());

        ResponseEntity<String> response = claim(login("alice@acme.com"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

}
