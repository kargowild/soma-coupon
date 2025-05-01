package com.coupon_hw.demo.global;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResponseDto<T>(
        int responseCode,
        String responseMessage,
        T responseBody
) {
    public ResponseDto(ResponseStatus responseStatus, T responseBody) {
        this(responseStatus.getStatusCode(), responseStatus.name(), responseBody);
    }

    public ResponseDto(T responseBody) {
        this(ResponseStatus.SUCCESS.getStatusCode(), ResponseStatus.SUCCESS.name(), responseBody);
    }

    public ResponseDto() {
        this(ResponseStatus.SUCCESS.getStatusCode(), ResponseStatus.SUCCESS.name(), null);
    }
}
