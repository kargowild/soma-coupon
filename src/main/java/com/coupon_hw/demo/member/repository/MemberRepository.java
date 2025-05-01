package com.coupon_hw.demo.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.coupon_hw.demo.member.domain.Member;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
}
