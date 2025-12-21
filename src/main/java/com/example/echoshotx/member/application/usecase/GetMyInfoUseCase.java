package com.example.echoshotx.member.application.usecase;

import com.example.echoshotx.member.application.adaptor.MemberAdaptor;
import com.example.echoshotx.member.domain.entity.Member;
import com.example.echoshotx.member.presentation.dto.response.MemberResponse;
import com.example.echoshotx.shared.annotation.usecase.UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@UseCase
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GetMyInfoUseCase {

    public MemberResponse.MyInfo execute(Member member) {
        String email = member.getEmail();
        LocalDateTime joinedAt = member.getCreatedDate();
        Integer credit = member.getCurrentCredits();
        return MemberResponse.MyInfo.builder()
                .email(email)
                .joinedAt(joinedAt)
                .credit(credit)
                .build();
    }

}
