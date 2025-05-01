package com.coupon_hw.demo.global;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.coupon_hw.demo.member.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
//@EnableWebMvc
public class WebMvcConfig implements WebMvcConfigurer {

    private final MemberRepository memberRepository;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AdminInterceptor(memberRepository))
                .order(1)
                .addPathPatterns("/coupons");
    }
}
