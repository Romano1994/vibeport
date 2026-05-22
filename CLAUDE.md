# Vibeport - AI 기반 콘서트 정보 서비스

## 프로젝트 개요

Vibeport는 AI를 활용한 콘서트 정보 제공 및 추천 서비스입니다.

**아키텍처:**
- 모노레포 구조: `vibeport_front` (React), `vibeport_back` (Spring Boot)
- 풀스택 웹 애플리케이션
- AI 기반 콘서트 정보 수집 및 추천

**주요 기술 스택:**
- Frontend: React 19, Redux Toolkit, React Router v7, Axios
- Backend: Spring Boot 3.5.6, Java 21, MyBatis, Spring AI, LangChain4j
- Database: MariaDB
- AI: OpenAI, Google Gemini, Perplexity
- 인증: JWT (Access/Refresh Token)

---

## 개발 환경 설정 및 실행

### 백엔드 (포트: 8081)

```bash
# 빌드
cd vibeport_back
gradlew build          # Windows
./gradlew build        # Unix/Mac

# 실행
gradlew bootRun        # Windows
./gradlew bootRun      # Unix/Mac

# 테스트
gradlew test           # Windows
./gradlew test         # Unix/Mac
```

### 프론트엔드 (포트: 3000)

```bash
# 의존성 설치
cd vibeport_front
npm install

# 개발 서버 (Chrome에서 자동 실행)
npm start

# 프로덕션 빌드
npm run build

# 테스트
npm test
```

---

## 고수준 아키텍처

### 백엔드 계층 구조

```
Controller → Service → Mapper (MyBatis) → Database
```

**도메인별 패키지 구조:**
- `auth`: 인증/로그인 관련
- `user`: 사용자 관리
- `concert`: 콘서트 정보 관리
- `ai`: AI 서비스 통합
- `mail`: 이메일 발송 (AWS SES)

각 도메인은 다음 구조를 따릅니다:
- `controller`: REST API 엔드포인트
- `service`: 비즈니스 로직
- `mapper`: MyBatis 인터페이스
- `vo`: Value Object (DTO)

**MyBatis 매퍼 XML:** `src/main/resources/mapper/*.xml`

### 인증 시스템

- **JWT 기반 인증** (Access Token + Refresh Token)
- `JwtAuthenticationFilter`: 모든 요청 전처리, JWT 검증
- `JwtUtil`: 토큰 생성/검증 유틸리티
- 토큰 저장: 프론트엔드 `sessionStorage`
- SecurityConfig: CORS 및 인증 설정

**주요 엔드포인트:**
- `/auth/login`: 로그인 (토큰 발급)
- `/auth/refresh`: Refresh Token으로 Access Token 재발급
- `/auth/logout`: 로그아웃

### AI 통합 아키텍처

**사용 라이브러리:**
- Spring AI (OpenAI, Google Gemini 통합)
- LangChain4j (Perplexity 통합)

**AI 서비스:**
- OpenAI: 콘서트 추천 및 자연어 처리
- Google Gemini: 콘서트 정보 수집 (배치)
- Perplexity: 웹 검색 기반 정보 보강

**패키지 구조:**
- `ai.service`: AI 서비스 비즈니스 로직
- `ai.client`: AI API 클라이언트 (LangChain4j)
- `ai.controller`: AI 기능 API 엔드포인트

**설정:** `application.properties`에 API 키 및 모델 설정 필요

### 배치 처리

- **Spring Batch + @Scheduled** 사용
- `GeminiConcertScheduler`: 콘서트 정보 자동 수집
- cron 설정: `batch.gemini.concert.cron` (application.properties)

### 프론트엔드 상태 관리

**Redux Toolkit + Redux Persist:**
- `store/userSlice.js`: 사용자 상태 (로그인 정보, 토큰)
- Redux Persist: `sessionStorage`에 상태 자동 저장/복원
- `store/store.js`: 스토어 설정

**상태 구조:**
```javascript
{
  user: {
    isLoggedIn: boolean,
    accessToken: string,
    refreshToken: string,
    userInfo: object
  }
}
```

### API 통신

**Axios 클라이언트:** `src/api/client.js`
- Base URL: `http://localhost:8081/vibeport`
- Request Interceptor: `sessionStorage`에서 JWT 토큰 자동 추가
- Response Interceptor: 401 에러 시 토큰 갱신 로직

### 라우팅

**React Router v7:**
- `ProtectedRoute`: 인증 필요 페이지 보호 컴포넌트
- 주요 라우트:
  - `/`: 홈 (콘서트 목록)
  - `/calendar`: 콘서트 캘린더
  - `/about`: 서비스 소개
  - `/mypage`: 마이페이지 (인증 필요)
  - `/review`: 리뷰 작성 (인증 필요)

---

## 중요한 설정 파일

### 백엔드

**`build.gradle`:**
- Java 21
- Spring Boot 3.5.6
- 주요 의존성: Spring Web, Security, MyBatis, Spring AI, LangChain4j

**`application.properties`:**
- 데이터베이스 설정 (MariaDB)
- JWT 설정 (secret, expiration)
- AI API 키 (OpenAI, Google, Perplexity)
- AWS SES 설정 (이메일 발송)
- 배치 스케줄러 cron 설정

**`SecurityConfig.java`:**
- CORS 설정: localhost:3000, localhost:8081, 13.211.47.98, vibeport.live 허용
- JWT 필터 등록
- 인증 예외 경로 설정

### 프론트엔드

**`package.json`:**
- React 19
- Redux Toolkit, Redux Persist
- Axios, React Router v7

**`src/api/client.js`:**
- Axios 인스턴스 설정
- Base URL 및 인터셉터 구성

**`src/store/store.js`:**
- Redux 스토어 설정
- Redux Persist 설정 (sessionStorage)

---

## 데이터베이스

**DBMS:** MariaDB (포트: 3306)
**ORM:** MyBatis

**매퍼 위치:** `vibeport_back/src/main/resources/mapper/`

**주요 매퍼 파일:**
- `LoginMapper.xml`: 인증 관련 쿼리
- `UserMapper.xml`: 사용자 관리 쿼리
- `ConcertMapper.xml`: 콘서트 정보 쿼리
- `AiMapper.xml`: AI 관련 데이터 쿼리

**설정:**
- snake_case → camelCase 자동 매핑 활성화
- 트랜잭션 관리: Spring @Transactional

---

## 개발 시 주의사항

### 백엔드

1. **새 API 엔드포인트 추가 시:**
   - `SecurityConfig.java`에서 CORS 설정 확인
   - 인증 필요 여부에 따라 `.permitAll()` 또는 `.authenticated()` 설정

2. **MyBatis 매퍼 작성 시:**
   - XML 매퍼와 인터페이스 네임스페이스 일치 확인
   - snake_case (DB) → camelCase (Java) 자동 변환됨
   - resultType/resultMap 명확히 지정

3. **AI 서비스 사용 시:**
   - `application.properties`에 API 키 설정 확인
   - API 호출 실패 시 fallback 로직 고려
   - 비용 관리 위해 불필요한 호출 최소화

4. **배치 작업 추가 시:**
   - `@Scheduled` 사용 또는 Spring Batch Job 정의
   - cron 표현식은 `application.properties`에 외부화

### 프론트엔드

1. **API 호출:**
   - 반드시 `src/api/client.js`의 axios 인스턴스 사용
   - 토큰은 자동으로 헤더에 추가됨

2. **인증 필요 페이지:**
   - `<ProtectedRoute>` 컴포넌트로 감싸기
   - 미인증 시 자동으로 로그인 페이지로 리다이렉트

3. **토큰 관리:**
   - 토큰 키는 `enum/Tokens.ts`에 정의된 상수 사용
   - Redux 상태에서 토큰 접근

4. **상태 관리:**
   - 전역 상태는 Redux Toolkit 사용
   - 컴포넌트 로컬 상태는 `useState` 사용

### 공통

1. **환경변수/API 키:**
   - 민감한 정보는 `application.properties` 또는 환경변수로 관리
   - Git에 커밋하지 않도록 주의 (.gitignore 확인)

2. **개발 환경:**
   - 프론트엔드 개발 시 백엔드가 8081 포트에서 실행 중이어야 함
   - CORS 설정으로 로컬 개발 환경 허용됨

3. **코드 스타일:**
   - 백엔드: Java 네이밍 규칙 준수 (camelCase)
   - 프론트엔드: ESLint 규칙 따름

---

## 디렉토리 구조

```
vibeport/
├── vibeport_back/          # Spring Boot 백엔드
│   ├── src/main/java/com/vibeport/
│   │   ├── auth/          # 인증
│   │   ├── user/          # 사용자
│   │   ├── concert/       # 콘서트
│   │   ├── ai/            # AI 서비스
│   │   ├── mail/          # 이메일
│   │   ├── config/        # 설정 (Security, MyBatis 등)
│   │   └── util/          # 유틸리티 (JWT 등)
│   ├── src/main/resources/
│   │   ├── mapper/        # MyBatis XML 매퍼
│   │   └── application.properties
│   └── build.gradle
│
└── vibeport_front/         # React 프론트엔드
    ├── src/
    │   ├── api/           # API 클라이언트
    │   ├── components/    # 재사용 컴포넌트
    │   ├── pages/         # 페이지 컴포넌트
    │   ├── store/         # Redux 스토어
    │   ├── enum/          # 상수 정의
    │   └── App.js         # 라우팅 설정
    └── package.json
```

---

## 유용한 팁

- **로컬 개발:** 백엔드 먼저 실행 후 프론트엔드 실행
- **디버깅:** 백엔드 로그는 콘솔, 프론트엔드는 Chrome DevTools 활용
- **데이터베이스:** MyBatis 쿼리 디버깅 시 `application.properties`에서 SQL 로깅 활성화
- **AI 테스트:** Postman 등으로 AI API 직접 호출 테스트 가능
