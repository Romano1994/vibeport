# Vibeport

Vibeport는 AI 기반의 현대적인 풀스택 웹 애플리케이션입니다. React 기반의 프론트엔드와 Spring Boot 기반의 백엔드로 구성되어 있으며, OpenAI와 Google Gemini 등의 AI 서비스를 통합하고 있습니다.

---

## 목차

- [프로젝트 구조]
- [필수 요구사항]
- [설치 및 실행]
- [기술 스택]
- [주요 기능]

---

## 프로젝트 구조

vibeport/
├── vibeport_back/          # Spring Boot 백엔드
│   ├── src/
│   ├── build.gradle        # Gradle 빌드 설정
│   ├── Dockerfile          # 백엔드 Docker 이미지
│   └── README.md
├── vibeport_front/         # React 프론트엔드
│   ├── src/
│   ├── public/
│   ├── package.json        # NPM 의존성
│   ├── Dockerfile          # 프론트엔드 Docker 이미지
│   └── README.md
├── docker-compose.yml      # Docker Compose 설정
└── README.md              # 이 파일

---

## 필수 요구사항

### 백엔드
- Java: JDK 21 이상
- Gradle: 빌드 도구 (프로젝트에 포함)
- MariaDB: 데이터베이스 (Docker 권장)

### 프론트엔드
- Node.js: 18.0 이상
- npm 또는 yarn: 패키지 매니저

### 선택사항
- Docker: 컨테이너화 배포
- OpenAI API Key: AI 기능 사용
- Google Gemini API Key: AI 기능 사용
- AWS SES: 이메일 발송

---

## 설치 및 실행

### 1️⃣ 프로젝트 클론
git clone <repository-url>
cd vibeport

### 2️⃣ 백엔드 실행

#### 로컬 실행
cd vibeport_back

Windows
gradlew.bat bootRun

macOS/Linux
./gradlew bootRun

#### Docker로 실행
docker-compose up backend

기본 포트: http://localhost:8080

### 3️⃣ 프론트엔드 실행

#### 개발 서버
cd vibeport_front
npm install
npm start

기본 포트: http://localhost:3000 (자동으로 Chrome에서 열림)

#### 프로덕션 빌드
npm run build

#### 테스트
npm test

### 4️⃣ Docker Compose로 전체 실행
docker-compose up

---

## 기술 스택

### 프론트엔드
| 기술 | 버전 | 용도 |
|------|------|------|
| React | 19.2.0 | UI 라이브러리 |
| Redux Toolkit | 2.10.1 | 상태 관리 |
| React Router | 7.9.4 | 라우팅 |
| Axios | 1.12.2 | HTTP 클라이언트 |
| Redux Persist | 6.0.0 | 상태 영속성 |

### 백엔드
| 기술 | 버전 | 용도 |
|------|------|------|
| Spring Boot | 3.5.6 | 웹 프레임워크 |
| Spring Security | - | 인증/권한 관리 |
| JWT | 0.11.5 | 토큰 기반 인증 |
| MyBatis | 3.0.5 | ORM |
| MariaDB | - | 데이터베이스 |
| Spring AI | 1.1.0 | AI 통합 |
| OpenAI SDK | 4.9.0 | OpenAI API |
| LangChain4j | 0.35.0 | LLM 프레임워크 |
| Google Gemini | 1.30.0 | Google AI API |
| AWS SES | 2.29.46 | 이메일 서비스 |
| Spring Batch | - | 배치 처리 |

---

## 주요 기능

### 사용자 관리
- 회원가입/로그인
- JWT 기반 토큰 인증
- Spring Security 권한 관리

### AI 통합
- OpenAI API 연동
- Google Gemini API 연동
- LangChain4j를 통한 고급 LLM 기능

### 이메일
- AWS SES를 통한 이메일 발송
- JavaMail API 지원

### 데이터 관리
- MyBatis를 이용한 데이터베이스 CRUD
- MariaDB 연동

### 배치 처리
- Spring Batch를 통한 자동화 작업
