package com.example.echoshotx.member.infrastructure.persistence;

import com.example.echoshotx.member.domain.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByUsername(String username);

}
