package com.vatika.workorder.shared.exception;

import com.vatika.workorder.workorder.model.WorkOrderStatus;

public class InvalidStateTransitionException extends RuntimeException {
    public InvalidStateTransitionException(WorkOrderStatus from, WorkOrderStatus  to) {
        super("Cannot transition from " + from + " to " + to);
    }
}
