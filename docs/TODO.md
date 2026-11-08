# TODO — std-naming-hound (IntelliJ IDEA Plugin)

공공데이터 공통표준(용어/단어/도메인) 기반으로 **표준 변수명/컬럼명/테이블명 추천 + (TERM 없을 때) WORD 조합 생성**을 지원한다.  
UI는 **ToolWindow 중심**, 결과는 **단일 리스트**로 표시하되 **TERM/WORD 타입을 배지로 구분**한다.

---

## 0) 목표/원칙

- **우선순위**: TERM이 있으면 바로 사용(정답) → 없으면 WORD 조합(플랜B)
- **검색 입력 흐름**: 한글로 먼저 검색(용어/단어/설명/동의어) → 영문 확인은 “단어 영문명” 중심
- **결과 UI**: 리스트 분리하지 않고, 한 리스트에서 `[TERM] / [WORD]` 타입을 명확히 표시
- **재사용 설계**: 검색 엔진(SearchEngine)을 ToolWindow / Rename / Completion에 공통으로 사용
- **성능**: 200ms 디바운스 + 백그라운드 검색 + Top N 제한

---

## 1) 데이터 파이프라인 (Excel → Internal Dataset)

- [x] 엑셀 시트 구조 파악 및 컬럼 매핑 정의
  - [x] 공통표준용어(TERM): 용어명/영문약어/설명/도메인/동의어
  - [x] 공통표준단어(WORD): 단어명/영문명/영문약어/설명/동의어/형식단어 여부
  - [x] 공통표준도메인(DOMAIN): 도메인명/타입/길이/표현/허용값 등
- [x] 변환 스크립트 작성(빌드 타임)
  - [x] `excel_to_dataset` 도구(예: Kotlin/Gradle task or 별도 Python)로 JSON 생성
  - [x] 중복/빈값/이상치 처리 규칙 수립
- [x] 플러그인 리소스에 기본 데이터셋 포함
  - [x] `terms.json`, `words.json`, `domains.json`
- [x] 데이터셋 버전/메타 정보 추가
  - [x] `dataset_version`, `source`, `generated_at`

---

## 2) 내부 모델/스토리지

### 2.1 모델 정의
- [x] `Term` 모델
  - `koName`, `abbr`, `description`, `domainName`, `synonyms[]`
- [x] `Word` 모델
  - `koName`, `enName`, `abbr`, `description`, `synonyms[]`, `isFormWord`
- [x] `Domain` 모델
  - `name`, `dataType`, `length`, `scale`, `storageFormat`, `displayFormat`, `allowedValues`
- [x] `SearchItem`(UI 렌더용 통합 모델)
  - `type: TERM|WORD|DOMAIN`, `titleKo`, `primaryEn`, `abbr`, `subText`, `score`, `payloadRef`

### 2.2 영구 저장소
- [x] `PersistentStateComponent`로 설정 저장
  - [x] `useCustomOnly` (커스텀만 사용)
  - [x] `enableFuzzy` (퍼지 검색)
  - [x] `dbDialect` (Postgres/Oracle/MySQL 등)
- [x] 커스텀 데이터 저장(선택)
  - [x] 초기: state에 JSON 문자열 저장(간단)
  - [ ] 확장: config 디렉토리에 파일로 저장(대용량 대비)

---

## 3) 검색 엔진 (SearchEngine)

### 3.1 정규화/인덱스
- [x] normalize 함수 구현
  - [x] 한글: 공백/특수문자 제거, 원문 유지 + normalizeText 생성
  - [x] 영문: lower-case, `_`/`-`/공백 통일, camel/snake 토큰화 보조
- [x] 엔티티별 `searchTextKo`, `searchTextEn` 프리컴퓨트(로딩 시)
- [x] “한글 쿼리” vs “영문 쿼리” 감지(간단한 정규식)

### 3.2 스코어링(가중치)
- [x] 한글 쿼리 스코어
  - [x] 이름 exact/prefix/contains
  - [ ] 동의어 매치 가산
  - [x] 설명 contains 낮은 가중치
- [x] 영문 쿼리 스코어
  - [x] WORD: `enName` > `abbr` 우선
  - [x] TERM: `abbr` 매치(공식 약어)
- [x] 결과 제한: Top N(기본 50), 페이지/더보기 옵션은 나중에

### 3.3 퍼지 검색(옵션)
- [x] MVP: n-gram overlap으로 가벼운 퍼지
- [ ] 확장: 상위 후보 N개만 Levenshtein 재정렬

---

## 4) ToolWindow UI (Search 중심)

### 4.1 기본 UI
- [x] ToolWindow 등록 (`ToolWindowFactory`)
- [x] 검색창(`SearchTextField`)
- [x] 결과 리스트(`JBList`) + renderer
  - [x] 타입 배지: `[TERM]` / `[WORD]` / `[DOMAIN]`
  - [x] 표시 예
    - TERM: `등록가능여부  |  REG_PSBLTY_YN  |  Domain: 여부C1`
    - WORD: `로그  |  Log  |  LOG`
- [x] 상세 패널(선택 항목)
  - [x] 설명, 도메인(TERM), 동의어, (WORD면 en/abbr) 표시

### 4.2 인터랙션
- [x] 디바운스 검색(200ms) + 백그라운드 실행
- [ ] 단축키/동작
  - [x] Enter: Insert (현재 caret에 삽입)
  - [x] Ctrl/Cmd+Enter: Copy
  - [ ] Alt+Enter: (나중에) Rename Intention으로 연결
- [x] 컨텍스트 메뉴 제공
  - [x] Add to Builder
  - [x] Insert Name
  - [x] Copy Name
  - [x] (TERM일 때) Copy SQL Column

### 4.3 TERM 없을 때 WORD 조합 유도(단일 리스트 유지)
- [x] 검색 결과에서 TERM이 0개면:
  - [x] 상단 힌트(텍스트): “표준 용어(TERM)가 없습니다. 표준 단어(WORD)를 조합해 생성하세요.”
  - [x] WORD 항목 선택 시 “Builder로 추가” 액션 제공(컨텍스트 메뉴/더블클릭)
- [x] 리스트 안에서도 WORD는 “조합 가능”이 보이도록 아이콘/표시 추가

---

## 5) Builder(조합 생성기)

> TERM이 없을 때 사용자가 WORD를 조합해서 `COM_TBL` 같은 이름을 만든다.

- [x] Builder 상태 모델
  - [x] 선택된 토큰(Word[]) 유지
  - [x] 결과 프리뷰(`COM_TBL`, camelCase 등)
- [x] 토큰 추가/삭제 UI(칩 형태)
- [x] 이름 생성 규칙
  - [x] 기본: WORD `abbr`를 `_`로 join → SNAKE_UPPER (예: `COM_TBL`)
  - [x] 케이스 변환 제공: camelCase / PascalCase / snake_case / CONSTANT_CASE
- [ ] 자동 토큰 제안(옵션)
  - [ ] 사용자가 “공통 테이블” 입력했는데 TERM 0개면, longest-match로 WORD 후보를 제안

---

## 6) SQL Generator (컬럼 정의 생성)

- [x] 도메인→DB 타입 매핑 테이블
  - [x] Postgres / Oracle / MySQL 기본 제공
- [x] TERM 선택 시
  - [x] `COLUMN_NAME` + `TYPE(length[,scale])` 생성
  - [x] (옵션) COMMENT SQL 생성
- [x] Builder 조합 결과 + 사용자 도메인 선택 시에도 SQL 생성 가능(확장)

---

## 7) JSON Import (커스텀 데이터)

- [x] JSON 스키마 정의(version 포함)
- [x] Settings UI 추가
  - [x] JSON 붙여넣기 영역
  - [x] Import 버튼
  - [x] “커스텀 데이터만 사용” 체크박스
  - [x] Reset to Default
- [x] Import 처리
  - [x] 유효성 검사(필수 필드, 타입, 중복)
  - [x] 병합 정책(기본: 커스텀이 우선)
  - [x] 병합 후 인덱스 리빌드

---

## 8) (후순위) IDE 통합: Rename Intention / Completion

### 8.1 Rename Intention (Alt+Enter)
- [ ] UAST 기반으로 변수 식별자/선언부 감지(Java/Kotlin 통합)
- [ ] 추천 이름 생성(TERM 있으면 TERM, 없으면 WORD 기반 후보)
- [ ] 후보 1개면 바로 rename, 여러 개면 팝업 선택
- [ ] Refactoring API로 안전 rename 수행

### 8.2 Completion(자동완성)
- [ ] 변수 선언 컨텍스트에서만 동작하도록 제한
- [ ] 트리거: 2글자 이상 / camelCase 중간 대문자 입력
- [ ] SearchEngine 재사용 + 상위 20~50개 제한
- [ ] Dumb mode 대응(인덱싱 중 graceful degrade)

---

## 9) 품질/운영

- [ ] 성능 체크(7k~수만건)
  - [ ] 검색 P95 < 50ms 목표(인메모리 스캔 기준)
- [ ] Dumb mode / ReadAction/WriteAction 준수
- [x] 로깅/에러 처리(Import 실패, 데이터 깨짐 등)
- [x] About/라이선스/출처 명시(내장 데이터셋)
- [x] 호환 IDE 범위 결정(예: 2024.3~2025.3)
- [x] Marketplace 배포 vs 사내 배포 결정

---

## Milestones

### M1 — “검색 MVP”
- [x] Excel → JSON 변환
- [x] SearchEngine(한글 중심) + ToolWindow 단일 리스트 + Copy/Insert

### M2 — “조합(BUILDER)”
- [x] WORD 조합으로 `COM_TBL` 생성
- [x] 케이스 변환 + Builder 연동

### M3 — “SQL 생성 + 커스텀 데이터”
- [x] 도메인 매핑 + 컬럼 SQL 생성
- [x] JSON Import + persistence

### M4 — “IDE 편의 기능”
- [ ] Rename Intention
- [ ] Completion(자동완성)

---

## Notes / Decisions

- 결과 리스트는 **하나**로 유지하고, 항목에 `[TERM]/[WORD]/[DOMAIN]` 배지를 붙인다.
- TERM이 있으면 항상 가장 높은 점수로 상단에 노출되게 가중치를 준다.
- 영문 검색은 **WORD 영문명(enName)** 우선, 약어(abbr)는 보조로 사용한다.
- SearchEngine은 이후 Rename/Completion에서 그대로 재사용한다.
