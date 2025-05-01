package com.coupon_hw.demo.coupon.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coupon_hw.demo.coupon.controller.dto.CouponCreateResponse;
import com.coupon_hw.demo.coupon.controller.dto.CouponRequest;
import com.coupon_hw.demo.coupon.service.CouponService;
import com.coupon_hw.demo.global.ResponseDto;
import com.coupon_hw.demo.global.ResponseStatus;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @PostMapping
    public ResponseDto<CouponCreateResponse> createCoupon(@RequestBody CouponRequest couponRequest) {
        long couponId = couponService.createCoupon(couponRequest.couponType(), couponRequest.availableCount(), couponRequest.expiresAt());
        return new ResponseDto<>(ResponseStatus.CREATED, new CouponCreateResponse(couponId));
    }
}
