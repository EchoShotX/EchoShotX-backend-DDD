package com.example.echoshotx.infrastructure.security.service;

import com.example.echoshotx.domain.member.adaptor.MemberAdaptor;
import com.example.echoshotx.infrastructure.security.vo.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final MemberAdaptor memberAdaptor;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return new CustomUserDetails(memberAdaptor.queryByUsername(username));
    }
}