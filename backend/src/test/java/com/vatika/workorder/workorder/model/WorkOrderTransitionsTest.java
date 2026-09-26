package com.vatika.workorder.workorder.model;

import com.vatika.workorder.shared.exception.InvalidStateTransitionException;
import org.junit.jupiter.api.Test;

import static com.vatika.workorder.workorder.model.WorkOrderStatus.*;
import static org.junit.jupiter.api.Assertions.*;

public class WorkOrderTransitionsTest {


    @Test
    void allowsDraftToSubmitted(){
        assertDoesNotThrow(() -> WorkOrderTransitions.canTransition(DRAFT, SUBMITTED));
    }

    @Test
    void rejectsDraftToCompleted(){
        InvalidStateTransitionException exception = assertThrows(InvalidStateTransitionException.class,
                () -> WorkOrderTransitions.canTransition(
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
                    () -> WorkOrderTransitions.canTransition(COMPLETED, target));
        }
    }

    @Test
    void rejectsAnyTransitionFromCancelled(){
        for (WorkOrderStatus target : WorkOrderStatus.values()) {
            assertThrows(InvalidStateTransitionException.class,
                    () -> WorkOrderTransitions.canTransition(CANCELLED, target));
        }
    }
}
