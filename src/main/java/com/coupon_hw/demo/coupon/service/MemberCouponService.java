package com.coupon_hw.demo.coupon.service;

import java.time.LocalDateTime;

import org.hibernate.StaleObjectStateException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coupon_hw.demo.coupon.domain.Coupon;
import com.coupon_hw.demo.coupon.domain.MemberCoupon;
import com.coupon_hw.demo.coupon.repository.CouponRepository;
import com.coupon_hw.demo.coupon.repository.MemberCouponRepository;
import com.coupon_hw.demo.member.domain.Member;
import com.coupon_hw.demo.member.repository.MemberRepository;
import com.coupon_hw.demo.member.service.MemberService;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MemberCouponService {

    private final MemberCouponRepository memberCouponRepository;
    private final MemberRepository memberRepository;
    private final CouponRepository couponRepository;

    @Transactional
    public long createMemberCoupon(long memberId, long couponId) {
        // TODO: 동시성 문제 처리
        Member member = getMember(memberId);
        Coupon coupon = getCoupon(couponId);

        boolean isExisted = memberCouponRepository.existsByMemberIdAndCouponId(memberId, couponId);
        if (isExisted) {
            throw new IllegalStateException("이미 발급 받은 쿠폰입니다.");
        }
        MemberCoupon memberCoupon = new MemberCoupon(member, coupon);
        memberCouponRepository.save(memberCoupon);

        return memberCoupon.getId();
    }

    private Coupon getCoupon(long couponId) {
        return couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰입니다."));
    }

    @Transactional
    public void useMemberCoupon(long memberId, long memberCouponId) {
        Member member = getMember(memberId);
        MemberCoupon memberCoupon = getMemberCoupon(memberCouponId);
        validateUsable(memberCoupon, member);
        try {
            memberCoupon.use();
        } catch (ObjectOptimisticLockingFailureException | StaleObjectStateException e) {
            // 하나의 회원 쿠폰을 여러 스레드가 사용하려고 하는 경우는 자주 발생하지 않는다.
            // 사용자에게는 다시 시도하라는 메시지를 보낸다.
            System.out.println("###");
            throw new IllegalStateException("이미 사용된 쿠폰입니다.", e);
        }
    }

    private void validateUsable(MemberCoupon memberCoupon, Member member) {
        if (!memberCoupon.getMember().equals(member)) {
            throw new IllegalStateException("자신의 쿠폰만 사용할 수 있습니다.");
        }
        if (!memberCoupon.isUsable()) {
            throw new IllegalStateException("이미 사용된 쿠폰입니다.");
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = memberCoupon.getCoupon().getExpiresAt();
        if (expiresAt.isBefore(now)) {
            throw new IllegalStateException("만료 기간이 지난 쿠폰입니다.");
        }
    }

    private Member getMember(long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
    }

    private MemberCoupon getMemberCoupon(long memberCouponId) {
        return memberCouponRepository.findById(memberCouponId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원 쿠폰입니다."));
    }
}
