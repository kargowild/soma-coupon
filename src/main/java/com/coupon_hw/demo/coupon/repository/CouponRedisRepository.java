package com.coupon_hw.demo.coupon.repository;

import java.time.Duration;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.coupon_hw.demo.coupon.domain.Coupon;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CouponRedisRepository {

    private final RedisTemplate<String, Object> redisTemplate;

    public void save(Coupon coupon) {
        redisTemplate.opsForValue().set("coupon:" + coupon.getId(), String.valueOf(coupon.getAvailableCount()));
    }

    public boolean isFirstIssuing(long couponId, long memberId) {
        String key = getFirstIssuingKey(couponId, memberId);
        Boolean result = redisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofMinutes(10));
        return Boolean.TRUE.equals(result); // null-safe 처리
    }

    public void resetToFirstIssuing(long couponId, long memberId) {
        redisTemplate.delete(getFirstIssuingKey(couponId, memberId));
    }

    private static String getFirstIssuingKey(long couponId, long memberId) {
        return "coupon:" + couponId + ":member:" + memberId;
    }

    public int decrementAvailableCount(long couponId) {
        return redisTemplate.opsForValue().decrement("coupon:" + couponId).intValue();
    }

    public void incrementAvailableCount(long couponId) {
        redisTemplate.opsForValue().increment("coupon:" + couponId);
    }
}
