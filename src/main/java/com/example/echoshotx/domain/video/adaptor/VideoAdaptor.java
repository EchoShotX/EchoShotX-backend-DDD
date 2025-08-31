package com.example.echoshotx.domain.video.adaptor;

import com.example.echoshotx.domain.video.entity.Video;
import com.example.echoshotx.domain.video.entity.VideoStatus;
import com.example.echoshotx.domain.video.exception.VideoErrorStatus;
import com.example.echoshotx.domain.video.exception.VideoHandler;
import com.example.echoshotx.domain.video.repository.VideoRepository;
import com.example.echoshotx.infrastructure.annotation.adaptor.Adaptor;
import com.example.echoshotx.infrastructure.exception.object.domain.S3Handler;
import com.example.echoshotx.infrastructure.exception.payload.code.ErrorStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Adaptor
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class VideoAdaptor {

    private final VideoRepository videoRepository;

    public List<Video> queryAllByMemberId(Long memberId) {
        return videoRepository.findByMemberIdOrderByCreatedDateDesc(memberId);
    }

    public Video queryById(Long videoId) {
        return videoRepository.findById(videoId)
                .orElseThrow(() -> new VideoHandler(VideoErrorStatus.VIDEO_NOT_FOUND));
    }
    
    /**
     * 회원별 특정 상태의 영상 목록 조회
     */
    public List<Video> queryAllByMemberIdAndStatus(Long memberId, VideoStatus status) {
        return videoRepository.findByMemberIdAndStatusOrderByCreatedDateDesc(memberId, status);
    }

}
