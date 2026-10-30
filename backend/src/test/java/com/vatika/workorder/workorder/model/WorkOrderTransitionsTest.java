package com.vatika.workorder.workorder.model;

import com.vatika.workorder.shared.exception.InvalidStateTransitionException;
import org.junit.jupiter.api.Test;

import static com.vatika.workorder.workorder.model.WorkOrderStatus.*;
import static org.junit.jupiter.api.Assertions.*;

public class WorkOrderTransitionsTest {


    @Test
    void allowsDraftToSubmitted(){
        assertDoesNotThrow(() -> WorkOrderTransitions.validateTransition(DRAFT, SUBMITTED));
    }

    @Test
    void rejectsDraftToCompleted(){
        InvalidStateTransitionException exception = assertThrows(InvalidStateTransitionException.class,
                () -> WorkOrderTransitions.validateTransition(
                        WorkOrderStatus.DRAFT,
                        COMPLETED
                ));

        assertEquals("Cannot transition from " + WorkOrderStatus.DRAFT + " to " + COMPLETED,
                exception.getMessage());
    }

    @Test
    void rejectsAnyTransitionFromCompleted(){
        for (WorkOrderStatus target : WorkOrderStatus.values()) {
            assertThrows(InvalidStateTransitionException.class,
                    () -> WorkOrderTransitions.validateTransition(COMPLETED, target));
        }
    }

    @Test
    void rejectsAnyTransitionFromCancelled(){
        for (WorkOrderStatus target : WorkOrderStatus.values()) {
            assertThrows(InvalidStateTransitionException.class,
                    () -> WorkOrderTransitions.validateTransition(CANCELLED, target));
        }
    }

    @Test
    void approved_toCompleted_isAllowed() {
        assertDoesNotThrow(() -> WorkOrderTransitions.validateTransition(APPROVED, COMPLETED));
    }

    @Test
    void approved_toAnyStatusExceptCompleted_throwsInvalidStateTransition() {
        for(WorkOrderStatus target: WorkOrderStatus.values()){
            if(target != COMPLETED){
                assertThrows(InvalidStateTransitionException.class,
                        () -> WorkOrderTransitions.validateTransition(APPROVED, target),
                        "APPROVED -> " + target + " should be rejected");
            }
        }
    }

    @Test
    void changesRequested_toInProgress_isAllowed() {
        assertDoesNotThrow(() -> WorkOrderTransitions.validateTransition(CHANGES_REQUESTED, IN_PROGRESS));
    }

    @Test
    void changesRequested_toAnyStatusExceptCompleted_throwsInvalidStateTransition() {
        for(WorkOrderStatus target: WorkOrderStatus.values()){
            if(target != IN_PROGRESS){
                assertThrows(InvalidStateTransitionException.class,
                        () -> WorkOrderTransitions.validateTransition(CHANGES_REQUESTED, target),
                        "CHANGES_REQUESTED -> " + target + " should be rejected");
            }
        }
    }

    @Test
    void inReview_toApproved_isAllowed() {
        assertDoesNotThrow(() -> WorkOrderTransitions.validateTransition(IN_REVIEW, APPROVED));
    }

    @Test
    void inReview_toChangesRequested_isAllowed() {
        assertDoesNotThrow(() -> WorkOrderTransitions.validateTransition(IN_REVIEW, CHANGES_REQUESTED));
    }

    @Test
    void inReview_toAnyStatusExceptCompleted_throwsInvalidStateTransition() {
        for(WorkOrderStatus target: WorkOrderStatus.values()){
            if(target != APPROVED && target != CHANGES_REQUESTED){
                assertThrows(InvalidStateTransitionException.class,
                        () -> WorkOrderTransitions.validateTransition(IN_REVIEW, target),
                        "IN_REVIEW -> " + target + " should be rejected");
            }
        }
    }
}
