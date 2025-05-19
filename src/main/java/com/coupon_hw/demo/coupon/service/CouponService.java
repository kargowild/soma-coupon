package com.coupon_hw.demo.coupon.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coupon_hw.demo.coupon.domain.Coupon;
import com.coupon_hw.demo.coupon.domain.CouponType;
import com.coupon_hw.demo.coupon.repository.CouponRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;

    @Transactional
    public long createCoupon(CouponType couponType, int availableCount, LocalDateTime expiresAt) {
        Coupon coupon = new Coupon(couponType, availableCount, expiresAt);
        couponRepository.save(coupon);
        return coupon.getId();
    }

    public Coupon readCoupon(long couponId) {
        return couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰입니다."));
    }
}
