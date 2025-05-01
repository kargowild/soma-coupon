package com.coupon_hw.demo.member.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coupon_hw.demo.member.domain.Member;
import com.coupon_hw.demo.member.domain.MemberType;
import com.coupon_hw.demo.member.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;

    @Transactional
    public long join(MemberType memberType, String name) {
        Member member = new Member(memberType, name);
        memberRepository.save(member);

        return member.getId();
    }
}
