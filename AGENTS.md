# Repository Guidelines

이 문서는 Std Naming Hound 저장소에 기여하는 방법을 간단하고 명확하게 안내합니다. 아래 규칙은 현재 프로젝트 구조와 Gradle 설정을 기준으로 합니다.

## Shared Development Standards

Use the original guides under `.dev-standards/standards/` selectively.
Read only files that exist and match the current task:

- Read `.dev-standards/standards/base.md` before modifying code.
- Before starting change work or choosing a branch, read `.dev-standards/standards/workflows/branch.md`.
- For isolated or parallel work, worktree setup, or cleanup, read `.dev-standards/standards/workflows/worktree.md`.
- If new guides are synchronized under `.dev-standards/standards/` before this index is refreshed, inspect the relevant category directory selectively.

Match guides to the affected module; do not load unrelated languages or frameworks.
Do not read every guide or the merged `.dev-standards/styleguide.md` by default.
Repository-specific instructions in this file take precedence over shared guides.

## Project Structure & Module Organization
- `src/main/kotlin`: 플러그인 핵심 로직(검색, SQL 생성, ToolWindow UI 등)
- `src/main/resources`: 기본 사전 데이터와 리소스(`data/*.json`)
- `src/test/kotlin`: 테스트 코드(JUnit 기반)
- `src/test/testData`: 테스트용 샘플 데이터
- `build`, `gradle`, `gradlew`: Gradle 빌드/래퍼 관련 파일

## Build, Test, and Development Commands
- `./gradlew build`: 전체 빌드 및 기본 검증 수행
- `./gradlew test`: 단위 테스트 실행
- `./gradlew runIde`: 로컬 IntelliJ에서 플러그인 실행
- `./gradlew runIdeForUiTests`: UI 테스트 환경 실행(로봇 서버 포함)
- `./gradlew verifyPlugin`: 플러그인 검증(호환성 검사)
- For Gradle build or dependency configuration, read `.dev-standards/standards/build-tools/gradle.md`.

## Coding Style & Naming Conventions
- 들여쓰기: 4 spaces
- Kotlin 표준 코딩 컨벤션을 따른다.
- 클래스/인터페이스: `PascalCase`, 함수/변수: `camelCase`
- 상수: `UPPER_SNAKE_CASE`
- 패키지: 소문자/점 구분(`com.github.ydj515.stdnaminghound`)
- When modifying Kotlin code, read `.dev-standards/standards/languages/kotlin.md`.

## Testing Guidelines
- 테스트 프레임워크: JUnit + IntelliJ Platform Test Framework
- 테스트 파일은 `src/test/kotlin`에 위치
- 테스트 데이터는 `src/test/testData`에 배치
- 새 기능은 최소 1개 이상의 테스트 추가 권장
- For Kover test coverage configuration or verification, read `.dev-standards/standards/tools/languages/kotlin/kover.md`.

## Commit & Pull Request Guidelines
- 커밋 메시지는 대체로 Conventional Commits 형식 사용
  - 예: `feat: add export dictionary`, `fix: fix typo`, `refactor: ...`
- PR에는 변경 요약, 테스트 결과, 관련 이슈 링크 포함
- UI 변경이 있는 경우 간단한 스크린샷 첨부 권장
- When drafting or revising a commit message, read `.dev-standards/standards/workflows/commit.md`.
- When drafting or revising a PR title or description, read `.dev-standards/standards/workflows/pr.md`.

