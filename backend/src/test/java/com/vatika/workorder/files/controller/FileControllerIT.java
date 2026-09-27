package com.vatika.workorder.files.controller;

import com.vatika.workorder.AbstractIntegrationTest;
import com.vatika.workorder.client.model.Client;
import com.vatika.workorder.client.repository.ClientRepository;
import com.vatika.workorder.files.dto.UploadUrlRequest;
import com.vatika.workorder.files.dto.UploadUrlResponse;
import com.vatika.workorder.files.repository.FileAssetRepository;
import com.vatika.workorder.files.repository.FileVersionRepository;
import com.vatika.workorder.identity.dto.LoginRequest;
import com.vatika.workorder.identity.dto.TokenResponse;
import com.vatika.workorder.identity.model.Role;
import com.vatika.workorder.identity.model.User;
import com.vatika.workorder.identity.repository.UserRepository;
import com.vatika.workorder.workorder.dto.CreateWorkOrderRequest;
import com.vatika.workorder.workorder.dto.TransitionRequest;
import com.vatika.workorder.workorder.dto.WorkOrderResponse;
import com.vatika.workorder.workorder.repository.WorkOrderRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class FileControllerIT extends AbstractIntegrationTest {

    @Container
    static LocalStackContainer localstack = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:3"));

    @DynamicPropertySource
    static void awsProps(DynamicPropertyRegistry registry) {
        registry.add("app.aws.endpoint", localstack::getEndpoint);
        registry.add("app.aws.region", localstack::getRegion);
        registry.add("app.aws.access-key", localstack::getAccessKey);
        registry.add("app.aws.secret-key", localstack::getSecretKey);
    }

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
    @Autowired
    FileAssetRepository fileAssetRepository;
    @Autowired
    FileVersionRepository fileVersionRepository;
    @Autowired
    S3Client s3Client;

    @Value("${app.s3.bucket}")
    String bucket;

    @BeforeEach
    void seed() {
        String hash = passwordEncoder.encode("password123");
        acmeId = clientRepository.save(new Client("Acme Corp")).getId();
        userRepository.save(new User("alice@acme.com", hash, "Alice", Role.CLIENT_USER, acmeId));
        s3Client.createBucket(builder -> builder.bucket(bucket));
    }

    @AfterEach
    void cleanup() {
        fileVersionRepository.deleteAll();
        fileAssetRepository.deleteAll();
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

    private ResponseEntity<UploadUrlResponse> createUploadUrl(String token, UUID workOrderId, String fileName, String contentType){
        UploadUrlRequest request = new UploadUrlRequest(workOrderId, fileName, contentType);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        ResponseEntity<UploadUrlResponse> response =
                rest.postForEntity(
                        "/files/upload-url",
                        new HttpEntity<>(request, headers),
                        UploadUrlResponse.class
                );

        return  response;
    }

    @Test
    void shouldCreateUploadUrl(){
        String token = login("alice@acme.com");
        UUID workOrderId = createWorkOrder(token, "Bracket redesign");
        ResponseEntity<UploadUrlResponse> response =
                createUploadUrl(token, workOrderId, "drawing.pdf", "application/pdf");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        UploadUrlResponse body = response.getBody();
        assertThat(body.fileAssetId()).isNotNull();
        assertThat(body.uploadUrl()).isNotBlank();
        assertThat(body.expiresAt()).isNotNull();
    }

    @Test
    void shouldConfirmUpload(){
        String token = login("alice@acme.com");
        UUID workOrderId = createWorkOrder(token, "Bracket redesign");

        ResponseEntity<UploadUrlResponse> response =
                createUploadUrl(token, workOrderId, "drawing.pdf", "application/pdf");
        UploadUrlResponse body = response.getBody();

        //Upload file
        HttpHeaders uploadHeaders = new HttpHeaders();
        uploadHeaders.setContentType(MediaType.APPLICATION_PDF);

        HttpEntity<byte[]> uploadRequest =
                new HttpEntity<>(
                        "test file".getBytes(),
                        uploadHeaders
                );

        ResponseEntity<String> uploadResponse =
                rest.exchange(
                        URI.create(body.uploadUrl()),
                        HttpMethod.PUT,
                        uploadRequest,
                        String.class
                );

        //check whether the file has been uploaded
        assertThat(uploadResponse.getStatusCode())
                .isEqualTo(HttpStatus.OK);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<Void> voidResponseEntity =
                rest.postForEntity(
                        "/files/" + body.fileAssetId() + "/confirm",
                        new HttpEntity<>(headers),
                        Void.class
                );

        assertThat(voidResponseEntity.getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void shouldCreateDownloadUrl(){

    }

    @Test
    void shouldRejectUploadForAnotherTenantsWorkOrder(){

    }

    @Test
    void shouldRejectUnauthenticatedRequest(){

    }
}
