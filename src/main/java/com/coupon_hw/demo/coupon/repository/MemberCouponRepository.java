package com.coupon_hw.demo.coupon.repository;

import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.coupon_hw.demo.coupon.domain.MemberCoupon;

@Repository
public interface MemberCouponRepository extends JpaRepository<MemberCoupon, Long> {

    boolean existsByMemberIdAndCouponId(long memberId, long couponId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT mc FROM MemberCoupon mc WHERE mc.id = :id")
    Optional<MemberCoupon> findByIdForUpdate(@Param("id") long id);
}
