package com.coupon_hw.demo.member.domain;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum MemberType {
    ADMIN,
    USER;

    @JsonCreator
    public static MemberType from(String input) {
        return Arrays.stream(MemberType.values())
                .filter(mt -> mt.name().equals(input))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원 타입입니다."));
    }

    @JsonValue
    public String toJson() {
        return this.name();
    }
}
