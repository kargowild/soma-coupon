package com.coupon_hw.demo.coupon.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.coupon_hw.demo.coupon.controller.dto.CouponCreateResponse;
import com.coupon_hw.demo.coupon.controller.dto.CouponRequest;
import com.coupon_hw.demo.coupon.controller.dto.CouponResponse;
import com.coupon_hw.demo.coupon.domain.Coupon;
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

    @GetMapping
    public ResponseDto<CouponResponse> readCoupon(@PathVariable long couponId) {
        Coupon coupon = couponService.readCoupon(couponId);
        return new ResponseDto<>(new CouponResponse(coupon));
    }
}
