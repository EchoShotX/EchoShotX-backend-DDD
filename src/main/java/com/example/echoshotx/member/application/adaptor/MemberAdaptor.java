package com.example.echoshotx.member.application.adaptor;


import com.example.echoshotx.member.domain.entity.Member;
import com.example.echoshotx.member.domain.exception.MemberErrorStatus;
import com.example.echoshotx.member.infrastructure.persistence.MemberRepository;
import com.example.echoshotx.shared.annotation.adaptor.Adaptor;
import com.example.echoshotx.member.presentation.exception.MemberHandler;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@Adaptor
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MemberAdaptor {

    private final MemberRepository repository;

    public Member queryById(Long memberId) {
        return repository.findById(memberId).orElseThrow(
                () -> new MemberHandler(MemberErrorStatus.MEMBER_NOT_FOUND)
        );
    }

    public Member queryByUsername(String username) {
        return repository.findByUsername(username).orElseThrow(
                () -> new MemberHandler(MemberErrorStatus.MEMBER_NOT_FOUND)
        );
    }
}
