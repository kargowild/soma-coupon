package com.coupon_hw.demo.coupon.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.coupon_hw.demo.coupon.domain.Coupon;
import com.coupon_hw.demo.coupon.domain.CouponType;
import com.coupon_hw.demo.coupon.domain.MemberCoupon;
import com.coupon_hw.demo.coupon.repository.CouponRedisRepository;
import com.coupon_hw.demo.coupon.repository.CouponRepository;
import com.coupon_hw.demo.coupon.repository.MemberCouponRepository;
import com.coupon_hw.demo.member.domain.Member;
import com.coupon_hw.demo.member.domain.MemberType;
import com.coupon_hw.demo.member.repository.MemberRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Testcontainers
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
class MemberCouponServiceTest {

    @Container
    static MySQLContainer<?> mysqlContainer = new MySQLContainer<>("mysql:8.0.28")
            .withDatabaseName("coupon")
            .withUsername("root")
            .withPassword("root");

    @Container
    static GenericContainer<?> redisContainer = new GenericContainer<>("redis:7.2")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysqlContainer::getJdbcUrl);
        registry.add("spring.datasource.username", mysqlContainer::getUsername);
        registry.add("spring.datasource.password", mysqlContainer::getPassword);
        registry.add("spring.datasource.driver-class-name", mysqlContainer::getDriverClassName);

        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379));
        registry.add("spring.data.redis.password", () -> "1234");
    }

    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private CouponRepository couponRepository;
    @Autowired
    private MemberCouponRepository memberCouponRepository;
    @Autowired
    private MemberCouponService memberCouponService;
    @Autowired
    private CouponRedisRepository couponRedisRepository;
    @Autowired
    @Qualifier("asyncExecutor")
    private ThreadPoolTaskExecutor asyncExecutor;

    @Test
    void X락_없이_테스트() throws InterruptedException {
        // given
        List<Member> members = new ArrayList<>();
        int memberCount = 20;
        int couponCount = 10;
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
                try {
                    memberCouponService.createMemberCouponNoXLock(member.getId(), coupon.getId());
                } catch (Exception e) {
                    exceptions.add(e);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        // then
        Coupon usedCoupon = couponRepository.findById(coupon.getId()).get();
        assertAll(
                () -> assertThat(usedCoupon.getAvailableCount()).isNotEqualTo(0),
                () -> assertThat(exceptions).isEmpty()
        );
    }

    @Test
    void X락으로_인한_지연_테스트() throws InterruptedException {
        // given
        List<Member> members = new ArrayList<>();
        int memberCount = 20;
        int couponCount = 10;
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
                    memberCouponService.createMemberCouponXLock(member.getId(), coupon.getId());
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
        assertAll(
                () -> assertThat(usedCoupon.getAvailableCount()).isEqualTo(0),
                () -> assertThat(exceptions.size()).isEqualTo(memberCount - couponCount)
        );
    }

    @Test
    void redis와_async를_활용하여_API응답_속도개선() throws InterruptedException {
        // given
        List<Member> members = new ArrayList<>();
        int memberCount = 20;
        int couponCount = 10;
        for (int i = 0; i < memberCount; i++) {
            Member member = memberRepository.save(new Member(MemberType.USER, "member" + i));
            members.add(member);
        }
        Coupon coupon = new Coupon(CouponType.CHICKEN, couponCount, LocalDateTime.now().plusDays(1));
        couponRepository.save(coupon);
        couponRedisRepository.save(coupon);

        ExecutorService executor = Executors.newFixedThreadPool(memberCount);
        CountDownLatch latch = new CountDownLatch(memberCount);

        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        // when
        for (int i = 0; i < memberCount; i++) {
            Member member = members.get(i);
            executor.submit(() -> {
                log.info("member" + member.getId() + " 요청");
                try {
                    memberCouponService.createMemberCouponRedisAsync(member.getId(), coupon.getId());
                } catch (Exception e) {
                    exceptions.add(e);
                } finally {
                    log.info("member" + member.getId() + " 응답");
                    latch.countDown();
                }
            });
        }

        latch.await();
        Coupon usedCoupon = couponRepository.findById(coupon.getId()).get();
        int createdMemberCouponCount = memberCouponRepository.findAll().size();
        // then1 (Async 완료 전 상태)
        assertAll(
                () -> assertThat(usedCoupon.getAvailableCount()).isNotEqualTo(0),
                () -> assertThat(createdMemberCouponCount).isEqualTo(couponCount),
                () -> assertThat(exceptions.size()).isEqualTo(memberCount - couponCount)
        );

        while (asyncExecutor.getActiveCount() > 0) {
            Thread.sleep(100);
        }

        //
        for (Exception exception : exceptions) {
            exception.printStackTrace();
        }
        //

        // then2 (Async 완료 후 상태)
        Coupon usedCouponAfterAsync = couponRepository.findById(coupon.getId()).get();
        assertThat(usedCouponAfterAsync.getAvailableCount()).isEqualTo(0);
    }

    @Test
    void 쿠폰은_한_번만_사용가능() throws InterruptedException {
        // given
        Member member = new Member(MemberType.USER, "kargo");
        memberRepository.save(member);
        Coupon coupon = new Coupon(CouponType.CHICKEN, 10, LocalDateTime.now().plusDays(1));
        couponRepository.save(coupon);
        MemberCoupon memberCoupon = new MemberCoupon(member, coupon);
        memberCouponRepository.save(memberCoupon);

        int tryCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(tryCount);
        CountDownLatch latch = new CountDownLatch(tryCount);

        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        // when
        for (int i = 0; i < tryCount; i++) {
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
        assertThat(exceptions.size()).isEqualTo(tryCount - 1);
        assertThat(exceptions.stream().allMatch(e -> e instanceof IllegalStateException)).isTrue();
    }
}
