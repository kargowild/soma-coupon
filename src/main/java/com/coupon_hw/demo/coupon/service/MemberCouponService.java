package com.coupon_hw.demo.coupon.service;

import java.time.LocalDateTime;

import jakarta.persistence.EntityManager;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coupon_hw.demo.coupon.domain.Coupon;
import com.coupon_hw.demo.coupon.domain.MemberCoupon;
import com.coupon_hw.demo.coupon.repository.CouponRedisRepository;
import com.coupon_hw.demo.coupon.repository.CouponRepository;
import com.coupon_hw.demo.coupon.repository.MemberCouponRepository;
import com.coupon_hw.demo.member.domain.Member;
import com.coupon_hw.demo.member.repository.MemberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberCouponService {

    private final CouponAsyncService couponAsyncService;
    private final MemberCouponRepository memberCouponRepository;
    private final MemberRepository memberRepository;
    private final CouponRepository couponRepository;
    private final CouponRedisRepository couponRedisRepository;
    private final EntityManager entityManager;

    @Transactional
    public long createMemberCouponNoXLock(long memberId, long couponId) {
        Member member = getMember(memberId);
        Coupon coupon = getCoupon(couponId);

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
        entityManager.flush();
        MemberCoupon memberCoupon = new MemberCoupon(member, coupon);
        memberCouponRepository.save(memberCoupon);

        return memberCoupon.getId();
    }

    @Transactional
    public long createMemberCouponXLock(long memberId, long couponId) {
        Member member = getMember(memberId);
        Coupon coupon = getCouponWithXLock(couponId);

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

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return memberCoupon.getId();
    }

    private Coupon getCouponWithXLock(long couponId) {
        return couponRepository.findByIdForUpdate(couponId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 쿠폰입니다."));
    }

    @Transactional
    public long createMemberCouponRedisAsync(long memberId, long couponId) {
        Member member = getMember(memberId);
        log.info("member" + member.getId() + " 첫 조회 쿼리 실행");
        Coupon coupon = getCoupon(couponId);

        // 만료기간 확인
        validateExpired(coupon);

        // 이미 발급 현황이 DB까지 기록되어 있는 경우
        if (memberCouponRepository.existsByMemberIdAndCouponId(memberId, couponId)) {
            throw new IllegalStateException("이미 발급 받은 쿠폰입니다.");
        }

        // 트랜잭션이 커밋되기 전에 중복 발급 신청 하는 경우 redis에서 setnx로 방지
        if (!couponRedisRepository.isFirstIssuing(couponId, memberId)) {
            throw new IllegalStateException("이미 발급 신청된 쿠폰입니다.");
        }

        // 남은 수량 확인과 동시에 쿠폰 수량 감소
        int decreaseResult = couponRedisRepository.decrementAvailableCount(couponId);
        if (decreaseResult < 0) {
            throw new IllegalStateException("이미 소진된 쿠폰입니다.");
        }

        // insert 작업 수행
        MemberCoupon memberCoupon = new MemberCoupon(member, coupon);
        try {
            memberCouponRepository.save(memberCoupon);
        } catch (DataAccessException e) {
            // DB insert 실패시 redis에 보상 작업(쿠폰갯수++, 회원마다 잡아둔 setnx 락 해제) 수행
            // 메시지 큐를 활용하여 무조건 성공시키도록 변경할 수 있다면 베스트겠다.
            couponRedisRepository.incrementAvailableCount(couponId);
            couponRedisRepository.resetToFirstIssuing(couponId, memberId);
            throw e;
        }

        // async로 coupon_count 차감
        couponAsyncService.decreaseAvailableCountAsync(couponId);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return memberCoupon.getId();
    }

    private Coupon getCoupon(long couponId) {
        return couponRepository.findById(couponId)
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
