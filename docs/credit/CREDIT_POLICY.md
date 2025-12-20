# 크레딧 정책 문서

## 개요

EchoShotX 서비스에서 비디오 AI 업스케일링 및 향상 처리를 위해 사용되는 크레딧 정책에 대한 문서입니다.

## 크레딧 정책

### 처리 타입별 크레딧 소모량

| 처리 타입 | 설명 | 초당 크레딧 |
|---------|------|-----------|
| `BASIC_ENHANCEMENT` | 기본 향상 | 1 credit/sec |
| `AI_UPSCALING` | AI 업스케일링 | 3 credits/sec |

### 계산 공식

```
필요한 크레딧 = ceil(초당 크레딧 × 영상 길이(초))
```

- `ceil()` 함수를 사용하여 소수점 이하는 올림 처리됩니다.
- 최소 크레딧은 1개입니다 (0.1초 이상의 영상).

## 예시

### 다양한 영상 길이에 대한 크레딧 계산

| 영상 길이 | BASIC_ENHANCEMENT | AI_UPSCALING |
|---------|------------------|--------------|
| 10초 | 10 credits | 30 credits |
| 30초 | 30 credits | 90 credits |
| 60초 (1분) | 60 credits | 180 credits |
| 300초 (5분) | 300 credits | 900 credits |
| 600초 (10분) | 600 credits | 1,800 credits |

### 소수점 처리 예시

| 영상 길이 | BASIC_ENHANCEMENT 계산 | 결과 |
|---------|---------------------|------|
| 10.0초 | ceil(1 × 10.0) = 10 | 10 credits |
| 10.1초 | ceil(1 × 10.1) = 11 | 11 credits |
| 10.3초 | ceil(1 × 10.3) = 11 | 11 credits |
| 10.9초 | ceil(1 × 10.9) = 11 | 11 credits |

| 영상 길이 | AI_UPSCALING 계산 | 결과 |
|---------|-----------------|------|
| 10.0초 | ceil(3 × 10.0) = 30 | 30 credits |
| 10.1초 | ceil(3 × 10.1) = 31 | 31 credits |
| 10.3초 | ceil(3 × 10.3) = 31 | 31 credits |
| 10.9초 | ceil(3 × 10.9) = 33 | 33 credits |

## API 사용법

### 엔드포인트

```
POST /credits/calculate
```

### 요청 예시

#### cURL

```bash
curl -X POST "http://localhost:8080/credits/calculate" \
  -H "Content-Type: application/json" \
  -d '{
    "processingType": "AI_UPSCALING",
    "durationSeconds": 60.5
  }'
```

#### JavaScript (Fetch API)

```javascript
const response = await fetch('http://localhost:8080/credits/calculate', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({
    processingType: 'AI_UPSCALING',
    durationSeconds: 60.5
  })
});

const data = await response.json();
console.log(data);
```

#### Java (Spring RestTemplate)

```java
RestTemplate restTemplate = new RestTemplate();

CalculateCreditCostRequest request = new CalculateCreditCostRequest();
request.setProcessingType(ProcessingType.AI_UPSCALING);
request.setDurationSeconds(60.5);

HttpEntity<CalculateCreditCostRequest> entity = new HttpEntity<>(request);
ResponseEntity<ApiResponseDto> response = restTemplate.postForEntity(
    "http://localhost:8080/credits/calculate",
    entity,
    ApiResponseDto.class
);
```

### 요청 스키마

```json
{
  "processingType": "BASIC_ENHANCEMENT" | "AI_UPSCALING",
  "durationSeconds": 60.5
}
```

**필드 설명:**
- `processingType` (필수): 처리 타입
  - `BASIC_ENHANCEMENT`: 기본 향상
  - `AI_UPSCALING`: AI 업스케일링
- `durationSeconds` (필수): 영상 길이 (초 단위)
  - 최소값: 0.1초
  - 소수점 지원

### 응답 예시

#### 성공 응답 (200 OK)

```json
{
  "isSuccess": true,
  "code": 2000,
  "message": "요청에 성공하였습니다.",
  "result": {
    "processingType": "AI_UPSCALING",
    "processingTypeDescription": "AI 업스케일링",
    "durationSeconds": 60.5,
    "creditCostPerSecond": 3,
    "requiredCredits": 182,
    "calculationFormula": "ceil(3 credits/sec × 60.50 sec) = 182 credits"
  }
}
```

#### 에러 응답 (400 Bad Request)

```json
{
  "isSuccess": false,
  "code": 4220,
  "message": "영상 길이는 0보다 커야 합니다.",
  "result": null
}
```

### 응답 스키마

```typescript
interface CreditCostResponse {
  processingType: "BASIC_ENHANCEMENT" | "AI_UPSCALING";
  processingTypeDescription: string;
  durationSeconds: number;
  creditCostPerSecond: number;
  requiredCredits: number;
  calculationFormula: string;
}
```

**필드 설명:**
- `processingType`: 처리 타입
- `processingTypeDescription`: 처리 타입 한글 설명
- `durationSeconds`: 입력받은 영상 길이 (초)
- `creditCostPerSecond`: 초당 크레딧 비용
- `requiredCredits`: 계산된 필요한 크레딧 (올림 처리됨)
- `calculationFormula`: 계산식 설명 문자열

## 주의사항

1. **소수점 처리**: 모든 계산은 `Math.ceil()` 함수를 사용하여 올림 처리됩니다.
   - 예: 10.1초 영상 → 11 credits (BASIC_ENHANCEMENT)

2. **최소 영상 길이**: 영상 길이는 최소 0.1초 이상이어야 합니다.

3. **처리 타입**: 유효하지 않은 처리 타입을 입력하면 에러가 발생합니다.

4. **인증**: 현재 이 API는 인증 없이 접근 가능합니다. (공개 API)

5. **실제 차감**: 이 API는 예상 비용만 계산하며, 실제 크레딧을 차감하지 않습니다.
   - 실제 크레딧 차감은 비디오 업로드 완료 시 (`POST /videos/{videoId}/complete-upload`) 발생합니다.

## 구현 세부사항

### 크레딧 계산 로직

크레딧 계산은 `CreditCalculator` 유틸리티 클래스에서 수행됩니다:

```java
public static int calculateRequiredCredits(
    ProcessingType processingType, 
    Double durationSeconds
) {
    if (processingType == null) {
        throw new CreditHandler(CreditErrorStatus.CREDIT_INVALID_PROCESSING_TYPE);
    }
    
    if (durationSeconds == null || durationSeconds <= 0) {
        throw new CreditHandler(CreditErrorStatus.CREDIT_INVALID_DURATION);
    }
    
    double costPerSecond = processingType.getCreditCostPerSecond();
    double totalCost = costPerSecond * durationSeconds;
    return (int) Math.ceil(totalCost);
}
```

### 에러 코드

| 에러 코드 | HTTP 상태 | 메시지 |
|---------|---------|--------|
| 4219 | 400 | 유효하지 않은 처리 타입입니다. |
| 4220 | 400 | 영상 길이는 0보다 커야 합니다. |

## 변경 이력

- 2024-XX-XX: 초기 문서 작성
  - BASIC_ENHANCEMENT: 1 credit/sec
  - AI_UPSCALING: 3 credits/sec
