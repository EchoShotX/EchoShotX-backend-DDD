# Complete Upload 리팩토링 상세 보고서

## 1. 문서 목적

본 문서는 `complete-upload` 경로 리팩토링의 기술 판단 근거, 트레이드오프, 장애 시나리오 대응 방식을 상세히 기록한다.

- 대상 경로: `POST /videos/{videoId}/complete-upload`
- 관련 도메인: Video, Credit, Job, Webhook, Security, Redis, DB 트랜잭션
- 목적:
  - 정합성(중복 과금/중복 작업/유실) 강화
  - 고부하 및 Redis 장애 시 Fail-safe 경로 확보
  - 보안 취약점(웹훅 위변조/기본 인증 정책) 개선

---

## 2. 리팩토링 전 문제 요약

### 2.1 정합성/동시성

1) **크레딧 차감 정합성 리스크**
- `@CurrentMember` 주입 객체가 트랜잭션 경계 밖에서 조회된 객체일 수 있음
- `open-in-view: false` 환경에서 detached 객체 변경이 flush되지 않을 가능성 존재
- 결과적으로 `credit_history`는 저장되지만 `member.currentCredits` 반영이 누락될 수 있음

2) **이벤트 발행 유실 윈도우**
- DB 커밋 후 이벤트 리스너에서 메시지 발행 시, 커밋 직후 프로세스 장애가 발생하면 메시지 유실 가능
- 상태는 `QUEUED`인데 큐 메시지가 없는 데이터 불일치 상태 발생 가능

3) **실패 웹훅 환불 중복 리스크**
- 실패 웹훅 재전송/중복 호출에 대한 환불 멱등 제어가 명확하지 않음

### 2.2 보안

1) **과도한 허용 정책**
- 보안 설정에서 `anyRequest().permitAll()` 상태였음

2) **웹훅 위변조/리플레이 방어 부재**
- 서명 검증(HMAC), 타임스탬프 검증, nonce replay 방지가 없었음

### 2.3 운영/성능

1) 프리티어 대비 커넥션풀/스레드 설정이 공격적(큰 풀)
2) Redis 활용은 락 중심이었고, Redis 장애 시 동작 기준이 코드 레벨에서 충분히 구조화되지 않음

---

## 3. 리팩토링 핵심 변경 사항

## 3.1 크레딧 정합성 강화 (DB 락 기반)

### 변경
- `MemberRepository.findByIdWithLock(...)` 추가
- `MemberAdaptor.queryByIdWithLock(...)` 추가
- `CreditService`의 차감/환불 경로에서 락 조회 사용

### 근거
- DB 레벨 `PESSIMISTIC_WRITE`는 동일 멤버 크레딧 갱신 경합 시 직렬화 효과를 제공
- 애플리케이션 레벨 synchronized로는 멀티 인스턴스 환경에서 불충분

### 트레이드오프
- 장점: 강한 정합성
- 단점: 락 대기 증가 가능
- 완화: 요청 입구 제어(rate limit), 짧은 커넥션 타임아웃, 트랜잭션 코드 최소화

---

## 3.2 환불 멱등 강화

### 변경
- `VIDEO_PROCESSING_REFUND:{videoId}` 키 개념 도입
- `CreditHistory`에 환불 키 저장 가능한 팩토리 추가
- 중복 환불 시 기존 레코드 재사용

### 근거
- 실패 웹훅은 네트워크 재전송(At-least-once)이 흔함
- 환불은 금전 도메인이므로 exactly-once가 불가능할 때 idempotent side-effect로 설계해야 함

### 트레이드오프
- 장점: 중복 환불 방지
- 단점: 키 정책 관리 필요

---

## 3.3 메시지 전달 신뢰성: Outbox 패턴 도입

### 변경
- `JobOutboxEvent`, `JobOutboxStatus`, `JobOutboxEventRepository` 추가
- `JobOutboxService`로 트랜잭션 내 outbox 적재
- `JobOutboxPublisherScheduler`로 비동기 발행 + 재시도 + 실패 마킹
- `CompleteVideoUploadUseCase`는 직접 이벤트 발행 대신 outbox 적재

### CS/시스템 관점 근거
- 단일 DB 트랜잭션은 DB 변경과 outbox 저장까지 원자성 보장
- 외부 브로커(SQS) 발행은 분산 트랜잭션(2PC) 없이 원자적 결합이 어려움
- Outbox는 "로컬 트랜잭션 + 비동기 재시도"로 분산 시스템의 현실적 정합성 패턴

### 트레이드오프
- 장점: 커밋/발행 간 유실 윈도우 대폭 축소
- 단점: 코드/테이블/운영 복잡성 증가, 최종 일관성(eventual consistency)

---

## 3.4 웹훅 보안 강화

### 변경
- `WebhookSignatureFilter` 추가
  - 헤더: `X-Signature`, `X-Timestamp`, `X-Nonce`
  - 서명: HMAC-SHA256
  - 시간창 검증: 허용 오차(`allowed-skew-seconds`)
- replay 방지
  - Redis `SET NX EX` 우선
  - Redis 장애 시 DB nonce 저장소 fallback
- `SecurityConfig`
  - 기본 정책: `anyRequest().authenticated()`
  - 웹훅 경로만 permitAll + 서명 필터 강제

### CS/네트워크/보안 관점 근거
- HTTPS는 전송 보호지만 요청 재생(replay) 자체는 막지 못함
- timestamp + nonce + HMAC 결합으로 메시지 무결성 + 재전송 공격 완화
- 상수시간 비교(constant-time compare)로 타이밍 기반 비교 공격 표면 축소

### 트레이드오프
- 장점: 위변조/리플레이 대폭 완화
- 단점: 파트너(웹훅 송신자)와 서명 프로토콜 동기화 필요

---

## 3.5 Redis 적극 활용 + 장애 대비(Fail-safe)

### 변경
- `RedisService`에 범용 연산(`setIfAbsent`, `getValue`, `setValue`) 추가
- 기존 complete-upload의 Redis 락 실패 시 DB 락으로 폴백 유지
- nonce 등록도 Redis 우선, 실패 시 DB 폴백

### 설계 원칙
- Redis는 **성능 계층**, DB는 **정합성 계층**
- Redis 실패는 예외가 아니라 정상 운영 시나리오로 간주

### 트레이드오프
- 장점: 정상시 지연/충돌 완화, 장애시 서비스 지속
- 단점: 이중 경로 코드 복잡성 증가

---

## 3.6 프리티어 보호 설정 (Pool/Thread/Timeout)

### 변경 (`application.yml`)
- Hikari: max 8 / min 2 / connection-timeout 1500ms
- Redis timeout: 100ms
- Tomcat: max threads 32 / accept-count 100 / connection-timeout 3s

### CS 관점 근거
- 작은 머신에서 과도한 커넥션풀/스레드풀은 문맥전환 비용 증가 + 큐 적체 + tail latency 악화
- fast-fail 타임아웃은 장애 전파를 줄여 시스템 전체 생존성을 높임

### 트레이드오프
- 장점: 붕괴 방지, 장애 격리
- 단점: 피크에서 429/503 증가 가능

---

## 4. 변경 파일 목록

### 수정
- `src/main/java/com/example/echoshotx/video/application/usecase/CompleteVideoUploadUseCase.java`
- `src/main/java/com/example/echoshotx/video/application/usecase/ProcessingFailedWebhookUseCase.java`
- `src/main/java/com/example/echoshotx/video/application/usecase/ProcessingCompletedWebhookUseCase.java`
- `src/main/java/com/example/echoshotx/credit/application/service/CreditService.java`
- `src/main/java/com/example/echoshotx/credit/domain/entity/CreditHistory.java`
- `src/main/java/com/example/echoshotx/credit/infrastructure/persistence/CreditHistoryRepository.java`
- `src/main/java/com/example/echoshotx/member/infrastructure/persistence/MemberRepository.java`
- `src/main/java/com/example/echoshotx/member/application/adaptor/MemberAdaptor.java`
- `src/main/java/com/example/echoshotx/job/application/service/JobService.java`
- `src/main/java/com/example/echoshotx/shared/security/config/SecurityConfig.java`
- `src/main/java/com/example/echoshotx/shared/security/resolver/CustomAuthenticationPrincipalArgumentResolver.java`
- `src/main/java/com/example/echoshotx/shared/redis/service/RedisService.java`
- `src/main/resources/application.yml`

### 신규
- `src/main/java/com/example/echoshotx/job/domain/entity/JobOutboxStatus.java`
- `src/main/java/com/example/echoshotx/job/domain/entity/JobOutboxEvent.java`
- `src/main/java/com/example/echoshotx/job/infrastructure/persistence/JobOutboxEventRepository.java`
- `src/main/java/com/example/echoshotx/job/application/service/JobOutboxService.java`
- `src/main/java/com/example/echoshotx/job/application/service/JobOutboxPublisherScheduler.java`
- `src/main/java/com/example/echoshotx/shared/security/config/WebhookSecurityProperties.java`
- `src/main/java/com/example/echoshotx/shared/security/filter/CachedBodyHttpServletRequest.java`
- `src/main/java/com/example/echoshotx/shared/security/filter/WebhookSignatureFilter.java`
- `src/main/java/com/example/echoshotx/shared/security/domain/WebhookNonceRecord.java`
- `src/main/java/com/example/echoshotx/shared/security/infrastructure/WebhookNonceRepository.java`
- `src/main/java/com/example/echoshotx/shared/security/service/WebhookReplayGuardService.java`

---

## 5. 장애/부하 시나리오별 동작 분석

## 5.1 Redis 전체 장애

### 문제
- Redis 락/nonce/캐시 경로 실패

### 현재 해법
- complete-upload: Redis 락 실패 -> DB 비관락 경로 진행
- webhook nonce: Redis 실패 -> DB nonce repository로 fallback

### 보장
- 정합성 유지(중복 과금/중복 환불 방지)
- 성능 저하 허용

### CS 관점
- 장애 도메인 분리: 캐시 장애가 트랜잭션 정합성 계층으로 전파되지 않도록 설계

---

## 5.2 동일 videoId 동시 중복 요청 폭주

### 문제
- N개의 동일 요청이 거의 동시에 도착

### 현재 해법
- Redis 락이 선행 억제
- 최종적으로 DB `PESSIMISTIC_WRITE` + 상태전이 검증이 단일 실행 보장
- idempotency 기록이 있으면 동일 응답 재사용

### 보장
- 중복 상태 전이 방지
- 중복 side effect 억제

### 트레이드오프
- 락 대기 증가 가능
- 대신 정합성 손실 방지

---

## 5.3 멤버 크레딧 동시 차감 경합

### 문제
- 같은 멤버가 여러 영상 완료 요청을 동시에 실행

### 현재 해법
- 멤버 행 락 조회 후 차감
- 차감 기록 키(unique) 기반 중복 차감 방지

### 보장
- 음수 크레딧/중복 차감 가능성 축소

### DB/시스템 관점
- Lost update를 애플리케이션 레벨이 아닌 DB 락으로 방지

---

## 5.4 커밋 직후 프로세스 크래시

### 문제
- 비즈니스 상태는 커밋됐지만 메시지 미전송 가능

### 현재 해법
- outbox row가 이미 커밋됨
- 재기동 후 스케줄러가 미전송 이벤트 재발행

### 보장
- 발행 지연은 가능하지만 유실 확률 대폭 감소

### 분산 시스템 관점
- Exactly-once 대신 at-least-once + 소비자 멱등으로 현실적 신뢰성 확보

---

## 5.5 웹훅 재전송/리플레이 공격

### 문제
- 정상 요청 복제 또는 타임스탬프 조작

### 현재 해법
- HMAC 무결성 검증
- timestamp window 검증
- nonce 1회성 등록(Redis/DB)

### 보장
- 위변조 및 단순 replay 방어 강화

### 보안 관점
- 인증(AuthN) 없이도 메시지 진위(AuthN-like)와 무결성(AuthZ 조건)를 분리해 보장

---

## 5.6 DB 풀 고갈/스레드 적체

### 문제
- 프리티어에서 높은 동시 접속 시 대기열 폭증

### 현재 해법
- 작은 커넥션풀, 작은 스레드풀, 짧은 타임아웃
- 빠른 실패로 시스템 붕괴(연쇄 대기) 방지

### OS/큐잉 이론 관점
- 처리율(capacity) 이상 유입 시 queue를 무한히 늘리면 latency만 증가하고 실패가 지연될 뿐
- bounded 자원 + fail-fast가 tail latency/복구성 측면에서 유리

---

## 6. CS 지식 관점 심층 설명

## 6.1 네트워크
- 웹훅은 TCP 재전송/애플리케이션 재시도로 중복 전송이 자연스럽게 발생
- 따라서 멱등 설계는 네트워크 불완전성 전제를 기반으로 해야 함
- 타임아웃을 짧게 설정한 이유는 HOL blocking 및 연결 점유를 줄이기 위함

## 6.2 운영체제
- 스레드 수 증가는 문맥 전환 오버헤드와 스택 메모리 사용량을 증가시킴
- 프리티어에서 과다 스레드는 CPU 캐시 locality 저하와 run-queue 증가로 p99 악화

## 6.3 데이터베이스
- 비관락은 충돌 구간의 직렬화를 제공해 금융성(credit) 로직에 적합
- unique key는 멱등성의 최후 보루(Last line of defense)
- Outbox는 DB 로컬 트랜잭션의 원자성을 활용한 분산 일관성 패턴

## 6.4 컴퓨터시스템/분산시스템
- exactly-once는 현실적으로 비용이 매우 크므로, at-least-once + idempotency가 표준
- 장애는 독립적으로 오지 않음(복합 장애): Redis 지연 + DB 대기 + 재시도 폭주
- 따라서 핵심은 성능 극대화보다 장애 시 graceful degradation

---

## 7. 트레이드오프 총정리

1) 락 기반 정합성 vs 처리량
- 선택: 정합성 우선

2) Outbox 도입 복잡성 vs 메시지 신뢰성
- 선택: 복잡성 수용

3) 보안 검증 비용 vs 위변조 리스크
- 선택: 웹훅 경로에 비용 지불

4) 작은 풀/fail-fast vs 순간 성공률
- 선택: 시스템 생존성 우선

5) Redis 의존 성능 vs Redis 장애 취약성
- 선택: Redis는 보조 계층, DB fallback 강제

---

## 8. 남은 리스크 및 후속 과제

1) Outbox 퍼블리셔 경쟁 제어
- 현재는 기본 배치형이며, 다중 인스턴스에서 `SKIP LOCKED` 기반 고도화 필요

2) idempotency/nonce/outbox 테이블 운영성
- 데이터 보존 정책, 인덱스, 청소 주기 튜닝 필요

3) 빌드 검증 환경 이슈
- 현 환경에서 Gradle worker classpath 오류로 컴파일/테스트 미검증
- 배포 전 CI 환경에서 반드시 재검증 필요

4) webhook 서명 클라이언트 연동
- 송신자 측 구현과 헤더/서명 스펙 동기화 필요

---

## 9. 성능/안정성 검증 계획

## 9.1 기능/정합성
- 동일 idempotency key 동시 100건: 실처리 1건 확인
- 실패 웹훅 100회 재전송: 환불 1회 확인
- Redis down 상태에서 complete-upload 정상 처리 확인

## 9.2 부하
- 목표: 프리티어 한계 내에서 p95/p99 안정성 관찰
- 관측 지표: DB pool wait, lock wait, 429/5xx, outbox pending age

## 9.3 장애 주입
- Redis 차단, DB 지연, SQS 장애, 프로세스 재기동 시나리오
- 기대: 데이터 정합성 유지 + 지연/실패는 통제된 범위

---

## 10. 결론

이번 리팩토링은 "고성능 최적화"보다 "정합성/보안/장애 복원성"을 우선한 구조 개편이다.

- 금전성 도메인(크레딧)에는 DB 락 + unique key로 강한 제어 적용
- 분산 메시징은 outbox로 유실 윈도우를 줄이고 재시도 가능 구조로 전환
- 웹훅은 HMAC + nonce + timestamp로 위변조와 replay를 실질적으로 억제
- Redis는 적극 활용하되, 실패를 정상 시나리오로 간주해 DB fallback 경로를 확보

즉, 본 변경은 프리티어 제약 하에서 시스템 붕괴를 막고, 향후 Scale-Up/Scale-Out 단계로 확장 가능한 기반을 만드는 데 초점을 맞췄다.
