package com.example.echoshotx.domain.video.repository;

import com.example.echoshotx.domain.video.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoRepository extends JpaRepository<Video, Long> {
}
