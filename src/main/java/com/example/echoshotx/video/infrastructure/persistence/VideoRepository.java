package com.example.echoshotx.video.infrastructure.persistence;

import com.example.echoshotx.video.domain.entity.Video;
import com.example.echoshotx.video.domain.entity.VideoStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Video v where v.id = :videoId")
    Optional<Video> findByIdWithLock(@Param("videoId") Long videoId);
}
