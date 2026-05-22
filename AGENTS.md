# Repository Guidelines

## Project Structure & Module Organization
- `vibeport_back/`: Spring Boot backend.
- Backend source: `vibeport_back/src/main/java/com/vibeport/` (package groups include `auth`, `user`, `concert`, `ai`, `mail`, `config`, `util`).
- Backend resources: `vibeport_back/src/main/resources/` and MyBatis XML mappers in `vibeport_back/src/main/resources/mapper/*.xml`.
- Backend tests: `vibeport_back/src/test/java/**`.
- `vibeport_front/`: React (CRA) frontend.
- Frontend source: `vibeport_front/src/` (notable folders: `api/`, `components/`, `pages/`, `store/`, `styles/`).
- Frontend public assets: `vibeport_front/public/`.

## Build, Test, and Development Commands
- Backend build: `cd vibeport_back` then `.\gradlew build` (Windows) or `./gradlew build` (Unix).
- Backend run: `.\gradlew bootRun` or `./gradlew bootRun` (serves on port `8081` by default).
- Backend tests: `.\gradlew test` or `./gradlew test`.
- Frontend install: `cd vibeport_front` then `npm install`.
- Frontend dev server: `npm start` (uses CRA; launches Chrome by default).
- Frontend tests: `npm test`.
- Frontend production build: `npm run build`.

## Coding Style & Naming Conventions
- JavaScript/JSX: 2‑space indentation, semicolons, single quotes (match existing files like `vibeport_front/src/App.js`).
- ESLint: CRA defaults via `react-app` and `react-app/jest` (no separate config file).
- React components: `PascalCase` file and component names (e.g., `MyPage`, `ProtectedRoute`).
- Java: packages are lower‑case (`com.vibeport...`), classes `PascalCase`, methods/fields `camelCase`. Keep formatting consistent with existing files (tabs are used in current sources).
- MyBatis: interface and XML pairs named `*Mapper.java` and `*Mapper.xml`.

## Testing Guidelines
- Backend: JUnit 5 via Spring Boot test starter. Tests live in `vibeport_back/src/test/java/**` and typically use `*Tests.java`.
- Frontend: Jest + React Testing Library. Tests live alongside code as `*.test.js` (e.g., `vibeport_front/src/App.test.js`).
- No explicit coverage threshold is defined; add tests for new business logic and regressions.

## Commit & Pull Request Guidelines
- Git history in both subprojects commonly uses `feat:`, `fix:`, and `test:` prefixes (often followed by a short Korean summary). Prefer that style, e.g., `feat: add genre column`.
- Keep subjects short and imperative. Include the impacted area when helpful.
- PRs should include: a brief summary, test commands run (or “not run” with reason), and screenshots for UI changes.

## Security & Configuration Tips
- `vibeport_back/src/main/resources/application.properties` contains environment-specific settings (DB, AI, SES). Avoid committing real secrets; use local overrides or environment variables in deployment.
- Frontend API base URL lives in `vibeport_front/src/api/client.js` and should match the backend (`http://localhost:8081/vibeport` by default).
