# 저장소 가이드라인

## 프로젝트 구조 및 모듈 구성
- `vibeport_back/`: Spring Boot 백엔드.
- 백엔드 소스: `vibeport_back/src/main/java/com/vibeport/` (주요 패키지: `auth`, `user`, `concert`, `ai`, `mail`, `config`, `util`).
- 백엔드 리소스: `vibeport_back/src/main/resources/` 및 MyBatis XML 매퍼 `vibeport_back/src/main/resources/mapper/*.xml`.
- 백엔드 테스트: `vibeport_back/src/test/java/**`.
- `vibeport_front/`: React(CRA) 프론트엔드.
- 프론트 소스: `vibeport_front/src/` (주요 폴더: `api/`, `components/`, `pages/`, `store/`, `styles/`).
- 프론트 퍼블릭 자산: `vibeport_front/public/`.

## 빌드, 테스트, 개발 명령어
- 백엔드 빌드: `cd vibeport_back` 후 `.\gradlew build`(Windows) 또는 `./gradlew build`(Unix).
- 백엔드 실행: `.\gradlew bootRun` 또는 `./gradlew bootRun` (기본 포트 `8081`).
- 백엔드 테스트: `.\gradlew test` 또는 `./gradlew test`.
- 프론트 설치: `cd vibeport_front` 후 `npm install`.
- 프론트 개발 서버: `npm start` (CRA, 기본 Chrome 실행).
- 프론트 테스트: `npm test`.
- 프론트 프로덕션 빌드: `npm run build`.

## 코딩 스타일 및 네이밍 규칙
- JavaScript/JSX: 2칸 들여쓰기, 세미콜론, 작은따옴표(예: `vibeport_front/src/App.js`).
- ESLint: CRA 기본 설정 `react-app`, `react-app/jest` 사용(별도 설정 파일 없음).
- React 컴포넌트: 파일/컴포넌트명 `PascalCase` (예: `MyPage`, `ProtectedRoute`).
- Java: 패키지 `lower-case`(`com.vibeport...`), 클래스 `PascalCase`, 메서드/필드 `camelCase`. 기존 소스는 탭 들여쓰기를 사용하므로 일관성 유지.
- MyBatis: `*Mapper.java` 와 `*Mapper.xml` 페어링 유지.

## 테스트 가이드
- 백엔드: Spring Boot Test 기반 JUnit 5. `vibeport_back/src/test/java/**` 아래 `*Tests.java` 패턴.
- 프론트: Jest + React Testing Library. `*.test.js` (예: `vibeport_front/src/App.test.js`).
- 명시적 커버리지 기준은 없음. 신규 로직/회귀 위험 구간은 테스트 추가 권장.

## 커밋 및 PR 가이드
- 각 하위 프로젝트의 히스토리는 `feat:`, `fix:`, `test:` 프리픽스를 주로 사용(간결한 한글 요약 포함). 예: `feat: 장르 칼럼 추가`.
- 제목은 짧고 명령형으로 작성, 필요 시 변경 범위를 포함.
- PR에는 요약, 실행한 테스트(또는 미실행 사유), UI 변경 시 스크린샷 포함.

## 보안 및 설정 팁
- `vibeport_back/src/main/resources/application.properties` 에 환경별 설정(DB, AI, SES)이 포함됨. 실제 시크릿은 커밋하지 말고 로컬/배포 환경 변수로 관리.
- 프론트 API Base URL은 `vibeport_front/src/api/client.js` 에 있으며 기본값은 `http://localhost:8081/vibeport`.
