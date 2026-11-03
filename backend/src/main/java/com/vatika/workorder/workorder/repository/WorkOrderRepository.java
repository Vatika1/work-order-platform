package com.vatika.workorder.workorder.repository;

import com.vatika.workorder.workorder.model.WorkOrder;
import com.vatika.workorder.workorder.model.WorkOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, UUID> {
    Optional<WorkOrder> findByIdAndClientId(UUID id, UUID clientId);

    Page<WorkOrder> findAllByClientId(UUID clientId, Pageable pageable);

    Page<WorkOrder> findAllByClientIdAndStatus(UUID clientId, WorkOrderStatus status, Pageable pageable);

    // staff only — no tenant scope
    Page<WorkOrder> findAllByStatus(WorkOrderStatus status, Pageable pageable);

    @Query(value = """
        SELECT * FROM work_orders
        WHERE status = 'SUBMITTED'
          AND assignee_id IS NULL
        ORDER BY created_at ASC
        LIMIT 1
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    Optional<WorkOrder> findNextUnassignedForUpdate();
}
