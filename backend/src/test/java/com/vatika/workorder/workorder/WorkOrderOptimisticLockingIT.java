package com.vatika.workorder.workorder;

import com.vatika.workorder.AbstractIntegrationTest;
import com.vatika.workorder.client.model.Client;
import com.vatika.workorder.client.repository.ClientRepository;
import com.vatika.workorder.identity.model.Role;
import com.vatika.workorder.identity.model.User;
import com.vatika.workorder.identity.repository.UserRepository;
import com.vatika.workorder.workorder.model.WorkOrder;
import com.vatika.workorder.workorder.model.WorkOrderStatus;
import com.vatika.workorder.workorder.repository.WorkOrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WorkOrderOptimisticLockingIT extends AbstractIntegrationTest {

    @Autowired
    private WorkOrderRepository workOrderRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void staleUpdate_isRejected_andFirstUpdateWins() {
        UUID id = createWorkOrder();

        // two users open the same work order: both copies have version 0
        WorkOrder first = workOrderRepository.findById(id).orElseThrow();
        WorkOrder second = workOrderRepository.findById(id).orElseThrow();

        // user 1 saves first: version 0 -> 1
        first.setStatus(WorkOrderStatus.SUBMITTED);
        workOrderRepository.save(first);

        // user 2 saves a stale copy (still version 0): rejected
        second.setStatus(WorkOrderStatus.CANCELLED);
        assertThrows(ObjectOptimisticLockingFailureException.class,
                () -> workOrderRepository.save(second));

        // user 1's change survived, nothing was overwritten
        WorkOrder saved = workOrderRepository.findById(id).orElseThrow();
        assertEquals(WorkOrderStatus.SUBMITTED, saved.getStatus());
        assertEquals(1L, saved.getVersion());
    }

    private UUID createWorkOrder() {
        Client client = clientRepository.save(new Client("Locking Co " + UUID.randomUUID()));
        User user = userRepository.save(new User(
                "lock-" + UUID.randomUUID() + "@test.com",
                "not-a-real-hash",
                "Lock Tester",
                Role.CLIENT_ADMIN,
                client.getId()));

        Instant now = Instant.now();
        WorkOrder workOrder = workOrderRepository.save(WorkOrder.builder()
                .id(UUID.randomUUID())
                .clientId(client.getId())
                .title("Locking test")
                .status(WorkOrderStatus.DRAFT)
                .createdBy(user.getId())
                .createdAt(now)
                .updatedAt(now)
                .build());
        return workOrder.getId();
    }
}