package com.coupon_hw.demo.coupon.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.coupon_hw.demo.coupon.service.MemberCouponService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/member-coupons")
@RequiredArgsConstructor
public class MemberCouponController {

    private final MemberCouponService memberCouponService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public long createMemberCoupon(@RequestHeader("member_id") long memberId, @RequestBody MemberCouponRequest request) {
        return memberCouponService.createMemberCoupon(memberId, request.couponId());
    }

    @PostMapping("/{member_coupon_id}")
    public void useMemberCoupon(@RequestHeader("member_id") long memberId, @PathVariable("member_coupon_id") long memberCouponId) {
        memberCouponService.useMemberCoupon(memberId, memberCouponId);
    }
}
