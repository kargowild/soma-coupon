package com.coupon_hw.demo.coupon.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Enumerated(value = EnumType.STRING)
    private CouponType couponType;

    private int availableCount;

    private LocalDateTime expiresAt;

    public Coupon(CouponType couponType, int availableCount, LocalDateTime expiresAt) {
        this.couponType = couponType;
        this.availableCount = availableCount;
        this.expiresAt = expiresAt;
    }

    public void setAvailableCount(int availableCount) {
        this.availableCount = availableCount;
    }
}
