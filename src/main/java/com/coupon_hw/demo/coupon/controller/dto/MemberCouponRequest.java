package com.coupon_hw.demo.coupon.controller.dto;

import jakarta.validation.constraints.NotNull;

public record MemberCouponRequest(
        @NotNull(message = "발급할 쿠폰을 선택해주세요.")
        long couponId
) {
}
