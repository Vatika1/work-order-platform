package com.vatika.workorder.workorder.model;

import com.vatika.workorder.shared.exception.InvalidStateTransitionException;

import java.util.Map;
import java.util.Set;

import static com.vatika.workorder.workorder.model.WorkOrderStatus.*;

public class WorkOrderTransitions {

    private static final Map<WorkOrderStatus, Set<WorkOrderStatus>> TRANSITIONS =
            Map.of(
                    DRAFT,             Set.of(SUBMITTED, CANCELLED),
                    SUBMITTED,         Set.of(IN_PROGRESS, CANCELLED),
                    IN_PROGRESS,       Set.of(IN_REVIEW),
                    IN_REVIEW,         Set.of(APPROVED, CHANGES_REQUESTED),
                    CHANGES_REQUESTED, Set.of(IN_PROGRESS),
                    APPROVED,          Set.of(COMPLETED),
                    COMPLETED,         Set.of(),
                    CANCELLED,         Set.of()
            );

    public static void validateTransition(WorkOrderStatus from, WorkOrderStatus to){

        Set<WorkOrderStatus> allowed = TRANSITIONS.get(from);

        if(!allowed.contains(to)){
            throw new InvalidStateTransitionException(from, to);
        }
    }
}
