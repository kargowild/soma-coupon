package com.coupon_hw.demo.coupon.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coupon_hw.demo.coupon.repository.CouponRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponAsyncService {

    private final CouponRepository couponRepository;

    @Async
    @Transactional
    public void decreaseAvailableCountAsync(long couponId) {
        log.info("async 스레드 시작");
        couponRepository.decreaseAvailableCount(couponId);

        // X락으로 인한 지연 의도
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        log.info("async 스레드 끝");
    }
}
