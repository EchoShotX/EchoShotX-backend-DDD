# Presigned URL 업로드 가이드

## 📋 3단계 업로드 플로우

```
1. 백엔드에 업로드 요청 → Presigned URL 받기
2. S3에 직접 업로드 (Presigned URL 사용)
3. 백엔드에 업로드 완료 알림
```

---

## Step 1: Presigned URL 받기

```http
POST /api/videos/initiate-upload
Authorization: Bearer {token}
Content-Type: application/json

{
  "fileName": "my-video.mp4",
  "filesSizeBytes": 10485760,
  "contentType": "video/mp4",
  "processingType": "BASIC_ENHANCEMENT"
}
```

**응답:**
```json
{
  "videoId": 123,
  "uploadId": "550e8400-e29b-41d4-a716-446655440000",
  "uploadUrl": "https://bucket.s3.amazonaws.com/...",
  "contentType": "video/mp4",
  "expiresAt": "2025-01-23T14:45:22"
}
```

---

## Step 2: S3에 직접 업로드

받은 `uploadUrl`로 **HTTP PUT** 요청:

```http
PUT {uploadUrl}
Content-Type: video/mp4
Content-Length: 10485760

[파일 바이너리 데이터]
```

⚠️ **주의**: 
- 백엔드가 아닌 **S3로 직접** 요청
- `Content-Type` 헤더 필수

---

## Step 3: 업로드 완료 알림

```http
POST /api/videos/{videoId}/complete-upload
Authorization: Bearer {token}
Content-Type: application/json

{
  "uploadId": "550e8400-e29b-41d4-a716-446655440000"
}
```

---

## 💻 JavaScript 예시

```javascript
// Step 1: Presigned URL 받기
const response = await fetch('/api/videos/initiate-upload', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    fileName: file.name,
    filesSizeBytes: file.size,
    contentType: file.type,
    processingType: 'BASIC_ENHANCEMENT'
  })
});

const { videoId, uploadId, uploadUrl, contentType } = await response.json();

// Step 2: S3에 업로드
await fetch(uploadUrl, {
  method: 'PUT',
  headers: {
    'Content-Type': contentType
  },
  body: file
});

// Step 3: 완료 알림
await fetch(`/api/videos/${videoId}/complete-upload`, {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({ uploadId })
});
```

---

## 📝 지원 스펙

| 항목 | 값 |
|------|-----|
| **파일 형식** | MP4, MOV, AVI, MKV |
| **최대 크기** | 500MB |
| **URL 유효시간** | 15분 |
| **처리 타입** | `BASIC_ENHANCEMENT`, `AI_UPSCALING` |

---

## ❌ 주요 에러

| 에러 | 원인 | 해결 |
|------|------|------|
| `403 Forbidden` (S3) | URL 만료 | Step 1부터 재시작 |
| `400 Bad Request` (S3) | Content-Type 불일치 | 응답의 contentType 사용 |
| `400 Bad Request` (백엔드) | 파일 크기/형식 오류 | 500MB 이하, 지원 형식 확인 |

---

## 🔑 핵심 포인트

1. **Step 2는 백엔드가 아닌 S3로 직접 업로드**
2. **15분 내에 업로드 완료** (URL 만료)
3. **Content-Type 헤더 필수**
4. **실패 시 Step 1부터 재시작**

