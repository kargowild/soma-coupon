package com.coupon_hw.demo.global;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.HandlerInterceptor;

import com.coupon_hw.demo.member.domain.Member;
import com.coupon_hw.demo.member.domain.MemberType;
import com.coupon_hw.demo.member.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AdminInterceptor implements HandlerInterceptor {

    private final MemberRepository memberRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        long memberId = Long.parseLong(request.getHeader("memberId"));
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
        if (member.getMemberType() != MemberType.ADMIN) {
            throw new IllegalAccessException("관리자 권한이 없는 회원입니다.");
        }
        return true;
    }
}
