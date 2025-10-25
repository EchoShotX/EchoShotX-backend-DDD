package com.example.echoshotx.video.infrastructure.persistence;

import com.example.echoshotx.video.domain.entity.Video;
import com.example.echoshotx.video.domain.entity.VideoStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VideoRepository extends JpaRepository<Video, Long> {
    
    /**
     * 사용자별 영상 목록 조회 (생성일 기준 내림차순)
     */
    List<Video> findByMemberIdOrderByCreatedDateDesc(Long memberId);
    
    /**
     * 사용자별 특정 상태 영상 목록 조회 (생성일 기준 내림차순)
     */
    List<Video> findByMemberIdAndStatusOrderByCreatedDateDesc(Long memberId, VideoStatus status);
    
    /**
     * 사용자별 영상 목록 조회
     */
    List<Video> findByMemberId(Long memberId);
    
    /**
     * 처리 상태별 영상 목록 조회
     */
    List<Video> findByStatus(VideoStatus status);
}
