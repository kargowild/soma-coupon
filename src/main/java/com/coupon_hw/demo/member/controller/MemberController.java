package com.coupon_hw.demo.member.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.coupon_hw.demo.global.ResponseDto;
import com.coupon_hw.demo.member.controller.dto.MemberCreateRequest;
import com.coupon_hw.demo.member.controller.dto.MemberCreateResponse;
import com.coupon_hw.demo.member.service.MemberService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseDto<MemberCreateResponse> join(@RequestBody MemberCreateRequest memberCreateRequest) {
        long memberId = memberService.join(memberCreateRequest.memberType(), memberCreateRequest.name());
        return new ResponseDto<>(new MemberCreateResponse(memberId));
    }
}
