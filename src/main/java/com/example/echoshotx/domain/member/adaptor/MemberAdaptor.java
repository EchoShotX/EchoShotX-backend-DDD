package com.example.echoshotx.domain.member.adaptor;


import com.example.echoshotx.domain.member.entity.Member;
import com.example.echoshotx.domain.member.exception.MemberErrorStatus;
import com.example.echoshotx.domain.member.repository.MemberRepository;
import com.example.echoshotx.infrastructure.annotation.adaptor.Adaptor;
import com.example.echoshotx.domain.member.exception.MemberHandler;
import com.example.echoshotx.infrastructure.exception.payload.code.ErrorStatus;
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
