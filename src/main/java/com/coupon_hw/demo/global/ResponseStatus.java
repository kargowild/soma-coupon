package com.coupon_hw.demo.global;

public enum ResponseStatus {
    SUCCESS(200),
    CREATED(201),
    ;

    private final int statusCode;

    ResponseStatus(int statusCode) {
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
