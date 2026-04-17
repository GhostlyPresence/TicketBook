package com.ticketbook.exception;

public class BookingPolicyViolationException extends RuntimeException {

    public BookingPolicyViolationException(String message) {
        super(message);
    }
}
