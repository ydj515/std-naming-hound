# Std Naming Hound

![Build](https://github.com/ydj515/std-naming-hound/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/29937-std-naming-hound.svg)](https://plugins.jetbrains.com/plugin/29937-std-naming-hound)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/29937-std-naming-hound.svg)](https://plugins.jetbrains.com/plugin/29937-std-naming-hound)

<!-- Plugin description -->
Std Naming Hound is an IntelliJ plugin that helps you search a standardized glossary (terms/words/domains), build column names, and generate SQL quickly.

Highlights:
- Fast glossary search for terms, words, and domains
- Builder mode to compose column names
- CREATE TABLE SQL generation from staged columns
- DB dialect support (MySQL/Postgres/Oracle)
- Custom JSON import with merge policy

---

Std Naming Hound는 표준 용어/단어/도메인 사전을 빠르게 검색하고, 컬럼명을 조합해 SQL을 생성하는 IntelliJ 플러그인입니다.

주요 기능:
- 용어/단어/도메인 검색 및 정렬
- Builder 모드로 컬럼명 조합
- 컬럼 누적 목록 기반 CREATE TABLE SQL 생성
- DB Dialect 설정(MySQL/Postgres/Oracle) 반영
- 커스텀 JSON 사전 import 및 병합 정책 지원
<!-- Plugin description end -->

표준 용어/단어 사전을 빠르게 검색하고, 컬럼명을 조합해 SQL을 생성하는 IntelliJ 기반 플러그인입니다.


## 주요 기능
- 용어/단어/도메인 사전 검색 및 정렬
- Builder 모드로 단어/용어 조합하여 컬럼명 생성
- 컬럼 누적 목록 관리 및 `CREATE TABLE` SQL 출력
- DB Dialect 설정(MySQL/Postgres/Oracle) 반영
- 커스텀 JSON 사전 import 및 병합 정책 설정
- Case Style(예: SNAKE_UPPER) 기본값 설정

## 빠른 시작
1) ToolWindow 열기: `View > Tool Windows > StdNamingHound`
2) 검색창에 키워드 입력
3) Builder 모드에서 단어/용어를 추가해 컬럼명 조합
4) 컬럼 누적 목록에 추가 후 Output에서 SQL 확인/복사

## 사용 흐름
- 검색 리스트
  - 용어/단어 필터 체크박스로 결과 범위 선택
  - 리스트 항목 hover 시 설명 툴팁 확인
- Builder
  - 단어/용어 추가 → 토큰으로 누적
  - 도메인 선택(용어 추가 시 도메인 자동 선택)
  - “컬럼 추가”로 스테이징에 누적
- Output
  - 누적된 컬럼 기준 `CREATE TABLE` SQL 자동 생성
  - Output 우측 버튼으로 복사

## Settings
`Settings/Preferences > Tools > Std Naming Hound`
- DB Dialect
- Default Case Style
- Merge Policy (Custom 우선/기본 우선/중복 제거)
- Use only custom data
- 커스텀 JSON import

## 커스텀 JSON
- JSON 파일을 import하면 즉시 적용 및 인덱스 재생성
- 샘플 JSON 다운로드 제공
- `version` 필드 필수
- terms/words/domains는 비워도 됨

## 단축키/컨텍스트
- 리스트 우클릭: Copy/Insert/Add to Builder
- Output 복사 버튼 제공

## 설치
### IDE Marketplace
`Settings/Preferences > Plugins > Marketplace`에서 `std-naming-hound` 검색

### 수동 설치
JetBrains Marketplace에서 최신 릴리즈 다운로드 후
`Settings/Preferences > Plugins > ⚙️ > Install plugin from disk...`

---
Plugin based on the IntelliJ Platform Plugin Template.
