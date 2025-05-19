package com.coupon_hw.demo.coupon.controller.dto;

import java.time.LocalDateTime;

import com.coupon_hw.demo.coupon.domain.Coupon;
import com.coupon_hw.demo.coupon.domain.CouponType;

public record CouponResponse(
        long couponId,
        CouponType couponType,
        int availableCount,
        LocalDateTime expiresAt
) {
    public CouponResponse(Coupon coupon) {
        this(coupon.getId(), coupon.getCouponType(), coupon.getAvailableCount(), coupon.getExpiresAt());
    }
}
