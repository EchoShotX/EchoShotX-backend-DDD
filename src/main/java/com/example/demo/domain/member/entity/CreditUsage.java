package com.example.demo.domain.member.entity;

import com.example.demo.domain.auditing.entity.BaseTimeEntity;
import com.example.demo.domain.video.entity.ProcessingType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@Entity
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CreditUsage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "video_id", nullable = false)
    private Long videoId;

    @Column(nullable = false)
    private Integer creditsUsed;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProcessingType processingType;

    @Column(length = 500)
    private String description;

}
