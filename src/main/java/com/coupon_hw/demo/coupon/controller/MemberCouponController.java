package com.coupon_hw.demo.coupon.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coupon_hw.demo.coupon.controller.dto.MemberCouponCreateResponse;
import com.coupon_hw.demo.coupon.controller.dto.MemberCouponRequest;
import com.coupon_hw.demo.coupon.service.MemberCouponService;
import com.coupon_hw.demo.global.ResponseDto;
import com.coupon_hw.demo.global.ResponseStatus;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/member-coupons")
@RequiredArgsConstructor
public class MemberCouponController {

    private final MemberCouponService memberCouponService;

    @PostMapping("/no-x-lock")
    public ResponseDto<MemberCouponCreateResponse> createMemberCouponNoXLock(@RequestHeader("memberId") long memberId, @RequestBody MemberCouponRequest request) {
        long memberCouponId = memberCouponService.createMemberCouponNoXLock(memberId, request.couponId());
        return new ResponseDto<>(ResponseStatus.CREATED, new MemberCouponCreateResponse(memberCouponId));
    }

    @PostMapping("/x-lock")
    public ResponseDto<MemberCouponCreateResponse> createMemberCouponXLock(@RequestHeader("member_id") long memberId, @RequestBody MemberCouponRequest request) {
        long memberCouponId = memberCouponService.createMemberCouponXLock(memberId, request.couponId());
        return new ResponseDto<>(ResponseStatus.CREATED, new MemberCouponCreateResponse(memberCouponId));
    }

    @PostMapping("/redis-async")
    public ResponseDto<MemberCouponCreateResponse> createMemberRedisAsync(@RequestHeader("memberId") long memberId, @RequestBody MemberCouponRequest request) {
        long memberCouponId = memberCouponService.createMemberCouponRedisAsync(memberId, request.couponId());
        return new ResponseDto<>(ResponseStatus.CREATED, new MemberCouponCreateResponse(memberCouponId));
    }

    @PostMapping("/{memberCouponId}")
    public ResponseDto<Void> useMemberCoupon(@RequestHeader("member_id") long memberId, @PathVariable("memberCouponId") long memberCouponId) {
        memberCouponService.useMemberCoupon(memberId, memberCouponId);
        return new ResponseDto<>();
    }
}
