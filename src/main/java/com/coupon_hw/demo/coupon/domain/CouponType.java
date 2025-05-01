package com.coupon_hw.demo.coupon.domain;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum CouponType {
    CHICKEN,
    HAMBURGER,
    PIZZA,
    ;

    @JsonCreator
    public static CouponType from(String input) {
        return Arrays.stream(CouponType.values())
                .filter(ct -> ct.name().equals(input))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰 타입입니다."));
    }

    @JsonValue
    public String toJson() {
        return this.name();
    }
}
