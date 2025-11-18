package com.example.echoshotx.member.application.usecase;

import com.example.echoshotx.member.application.adaptor.MemberAdaptor;
import com.example.echoshotx.member.domain.entity.Member;
import com.example.echoshotx.member.infrastructure.persistence.MemberRepository;
import com.example.echoshotx.shared.annotation.usecase.UseCase;
import com.example.echoshotx.shared.security.dto.JwtToken;
import com.example.echoshotx.shared.security.service.TokenService;
import com.example.echoshotx.shared.security.vo.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@UseCase
@Transactional
@RequiredArgsConstructor
public class TestGenerateTokenUseCase {

    private final MemberRepository memberRepository;
    private final TokenService tokenService;

    public JwtToken execute(String username) {
        Member member = memberRepository.findByUsername(username)
                .orElseGet(() ->
                    memberRepository.save(Member.builder().username(username).build()));

        CustomUserDetails userDetails = new CustomUserDetails(member);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                List.of(new SimpleGrantedAuthority(
                        "ROLE_USER"))
        );
        return tokenService.generateToken(auth);
    }

}
