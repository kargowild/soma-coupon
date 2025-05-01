package com.coupon_hw.demo.coupon.controller.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;

import com.coupon_hw.demo.coupon.domain.CouponType;

public record CouponRequest(
        @NotNull(message = "쿠폰 타입을 입력해주세요.")
        CouponType couponType,
        @NotNull(message = "쿠폰 발급 개수를 입력해주세요.")
        int availableCount,
        LocalDateTime expiresAt
) {
}
