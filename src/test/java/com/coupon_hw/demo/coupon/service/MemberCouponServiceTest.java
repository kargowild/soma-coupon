package com.coupon_hw.demo.coupon.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import com.coupon_hw.demo.coupon.domain.Coupon;
import com.coupon_hw.demo.coupon.domain.CouponType;
import com.coupon_hw.demo.coupon.domain.MemberCoupon;
import com.coupon_hw.demo.coupon.repository.CouponRepository;
import com.coupon_hw.demo.coupon.repository.MemberCouponRepository;
import com.coupon_hw.demo.member.domain.Member;
import com.coupon_hw.demo.member.domain.MemberType;
import com.coupon_hw.demo.member.repository.MemberRepository;

@SpringBootTest(webEnvironment = WebEnvironment.NONE)
class MemberCouponServiceTest {

    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private CouponRepository couponRepository;
    @Autowired
    private MemberCouponRepository memberCouponRepository;
    @Autowired
    private MemberCouponService memberCouponService;

    @Test
    @DisplayName("회원이 자신의 쿠폰을 동시에 여러 번 사용하려고 시도하면 예외가 발생한다.")
    void useMemberCouponConcurrently() throws InterruptedException {
        // given
        Member member = new Member(MemberType.USER, "kargo");
        memberRepository.save(member);
        Coupon coupon = new Coupon(CouponType.CHICKEN, 10, LocalDateTime.now().plusDays(1));
        couponRepository.save(coupon);
        MemberCoupon memberCoupon = new MemberCoupon(member, coupon);
        memberCouponRepository.save(memberCoupon);

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        // when
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    memberCouponService.useMemberCoupon(member.getId(), memberCoupon.getId());
                } catch (Exception e) {
                    exceptions.add(e);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        //
        for (Exception exception : exceptions) {
            System.out.println(exception.getClass());
        }
        //

        // then
        assertEquals(threadCount - 1, exceptions.size(), "1개 성공, 나머지는 실패해야 함");
        assertTrue(exceptions.stream().allMatch(e ->
                e instanceof org.hibernate.StaleObjectStateException ||
                        (e.getCause() != null && e.getCause() instanceof org.hibernate.StaleObjectStateException)
        ), "모든 실패는 StaleObjectStateException 이어야 함");
    }
}
