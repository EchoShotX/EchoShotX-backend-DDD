package com.example.demo.domain.member.repository;

import com.example.demo.domain.member.entity.CreditUsage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreditUsageRepository extends JpaRepository<CreditUsage, Long> {
}
