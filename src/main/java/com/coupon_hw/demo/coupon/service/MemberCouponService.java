package com.coupon_hw.demo.coupon.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coupon_hw.demo.coupon.domain.Coupon;
import com.coupon_hw.demo.coupon.domain.MemberCoupon;
import com.coupon_hw.demo.coupon.repository.CouponRepository;
import com.coupon_hw.demo.coupon.repository.MemberCouponRepository;
import com.coupon_hw.demo.member.domain.Member;
import com.coupon_hw.demo.member.repository.MemberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MemberCouponService {

    private final MemberCouponRepository memberCouponRepository;
    private final MemberRepository memberRepository;
    private final CouponRepository couponRepository;

    @Transactional
    public long createMemberCoupon(long memberId, long couponId) {
        Member member = getMember(memberId);
        log.info("member{} x락 획득 시도", memberId);
        Coupon coupon = getCouponWithXLock(couponId);
        log.info("member{} x락 획득 성공", memberId);

        // 만료기간 확인
        validateExpired(coupon);

        // 남은 수량 확인
        int availableCount = coupon.getAvailableCount();
        if (availableCount < 1) {
            throw new IllegalStateException("모두 소진된 쿠폰입니다.");
        }

        // 이미 발급 받은 회원인지 확인
        boolean isExisted = memberCouponRepository.existsByMemberIdAndCouponId(memberId, couponId);
        if (isExisted) {
            throw new IllegalStateException("이미 발급 받은 쿠폰입니다.");
        }

        // 쿠폰 발급 진행
        coupon.setAvailableCount(availableCount - 1);
        MemberCoupon memberCoupon = new MemberCoupon(member, coupon);
        memberCouponRepository.save(memberCoupon);

        // 의도적으로 1초 지연
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
        log.info("member{} 락 반납", memberId);
        return memberCoupon.getId();
    }

    private Coupon getCouponWithXLock(long couponId) {
        return couponRepository.findByIdForUpdate(couponId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰입니다."));
    }

    @Transactional
    public void useMemberCoupon(long memberId, long memberCouponId) {
        Member member = getMember(memberId);
        MemberCoupon memberCoupon = getMemberCouponWithXLock(memberCouponId);
        validateUsable(memberCoupon, member);
        memberCoupon.setUsable(false);
    }

    private void validateUsable(MemberCoupon memberCoupon, Member member) {
        if (!memberCoupon.getMember().equals(member)) {
            throw new IllegalStateException("자신의 쿠폰만 사용할 수 있습니다.");
        }
        if (!memberCoupon.isUsable()) {
            throw new IllegalStateException("이미 사용된 쿠폰입니다.");
        }
        validateExpired(memberCoupon.getCoupon());
    }

    private void validateExpired(Coupon coupon) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = coupon.getExpiresAt();
        if (expiresAt.isBefore(now)) {
            throw new IllegalStateException("만료 기간이 지난 쿠폰입니다.");
        }
    }

    private Member getMember(long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
    }

    private MemberCoupon getMemberCouponWithXLock(long memberCouponId) {
        return memberCouponRepository.findByIdForUpdate(memberCouponId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원 쿠폰입니다."));
    }
}
