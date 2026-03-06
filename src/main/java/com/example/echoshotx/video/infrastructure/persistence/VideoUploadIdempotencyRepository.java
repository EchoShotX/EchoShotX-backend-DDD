package com.example.echoshotx.video.infrastructure.persistence;

import com.example.echoshotx.video.domain.entity.VideoUploadIdempotencyRecord;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoUploadIdempotencyRepository
        extends JpaRepository<VideoUploadIdempotencyRecord, Long> {

    Optional<VideoUploadIdempotencyRecord> findByMemberIdAndVideoIdAndIdempotencyKey(
            Long memberId, Long videoId, String idempotencyKey);
}
