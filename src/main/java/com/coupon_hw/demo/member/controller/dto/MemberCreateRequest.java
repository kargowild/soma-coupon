package com.coupon_hw.demo.member.controller.dto;

import jakarta.validation.constraints.NotNull;

import com.coupon_hw.demo.member.domain.MemberType;

public record MemberCreateRequest(
        @NotNull(message = "회원 타입을 입력해주세요.")
        MemberType memberType,
        @NotNull(message = "회원명을 입력해주세요.")
        String name
) {
}
