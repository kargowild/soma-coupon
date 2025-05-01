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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.coupon_hw.demo.coupon.domain.Coupon;
import com.coupon_hw.demo.coupon.domain.CouponType;
import com.coupon_hw.demo.coupon.domain.MemberCoupon;
import com.coupon_hw.demo.coupon.repository.CouponRepository;
import com.coupon_hw.demo.coupon.repository.MemberCouponRepository;
import com.coupon_hw.demo.member.domain.Member;
import com.coupon_hw.demo.member.domain.MemberType;
import com.coupon_hw.demo.member.repository.MemberRepository;

@ActiveProfiles("test")
@Testcontainers
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
class MemberCouponServiceTest {

    @Container
    static MySQLContainer<?> mysqlContainer = new MySQLContainer<>("mysql:8.0.28")
            .withDatabaseName("coupon")
            .withUsername("root")
            .withPassword("root");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysqlContainer::getJdbcUrl);
        registry.add("spring.datasource.username", mysqlContainer::getUsername);
        registry.add("spring.datasource.password", mysqlContainer::getPassword);
        registry.add("spring.datasource.driver-class-name", mysqlContainer::getDriverClassName);
    }

    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private CouponRepository couponRepository;
    @Autowired
    private MemberCouponRepository memberCouponRepository;
    @Autowired
    private MemberCouponService memberCouponService;

    @Test
    @DisplayName("여러 명의 회원이 쿠폰 발급을 동시에 신청해도, 발급 갯수만큼만 발급되어야 한다.")
    void createMemberCouponByManyUsers() throws InterruptedException {
        // given
        List<Member> members = new ArrayList<>();
        int memberCount = 1000;
        int couponCount = 100;
        for (int i = 0; i < memberCount; i++) {
            Member member = memberRepository.save(new Member(MemberType.USER, "member" + i));
            members.add(member);
        }
        Coupon coupon = new Coupon(CouponType.CHICKEN, couponCount, LocalDateTime.now().plusDays(1));
        couponRepository.save(coupon);

        ExecutorService executor = Executors.newFixedThreadPool(memberCount);
        CountDownLatch latch = new CountDownLatch(memberCount);

        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        // when
        for (int i = 0; i < memberCount; i++) {
            Member member = members.get(i);
            executor.submit(() -> {
                long start = System.currentTimeMillis();  // 시작 시간 기록
                try {
                    memberCouponService.createMemberCoupon(member.getId(), coupon.getId());
                } catch (Exception e) {
                    exceptions.add(e);
                } finally {
                    long end = System.currentTimeMillis();  // 종료 시간 기록
                    System.out.printf("Thread for member %d took %d ms%n", member.getId(), (end - start));
                    latch.countDown();
                }
            });
        }

        latch.await();

        // then
        Coupon usedCoupon = couponRepository.findById(coupon.getId()).get();
        assertEquals(memberCount - couponCount, exceptions.size(), "100개 성공, 900개 실패");
        assertEquals(usedCoupon.getAvailableCount(), 0, "쿠폰은 모두 소진되어 0개 남음");
        assertTrue(exceptions.stream().allMatch(e -> e.getMessage().equals("모두 소진된 쿠폰입니다.")), "모든 예외는 쿠폰 소진에 의해 발생");
    }

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

        // then
        assertEquals(threadCount - 1, exceptions.size(), "1개 성공, 나머지는 실패해야 함");
        assertTrue(exceptions.stream().allMatch(e ->
                e instanceof IllegalStateException
        ), "모든 실패는 IllegalStateException 이어야 함");
    }
}
