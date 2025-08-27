package com.example.echoshotx.domain.member.repository;

import com.example.echoshotx.domain.member.entity.CreditUsage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreditUsageRepository extends JpaRepository<CreditUsage, Long> {
}
