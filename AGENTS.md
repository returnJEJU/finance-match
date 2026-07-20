# AGENTS.md — 찰떡귱합 (finance-match)

> AI 코딩 도구(Claude Code·Cursor·Copilot 등)와 팀원이 함께 지키는 프로젝트 규칙.
> Team `return JEJU;` · KB IT's Your Life 7기 · 부부·커플 금융궁합도 진단 + 금융상품 추천 서비스.
> **이 파일이 컨벤션의 정본이다.** (`CLAUDE.md`는 이 파일을 가리키는 포인터)

---

## ⚠️ 절대 규칙 (과정 규정 — 위반 시 평가 감점)

- **Spring Boot 금지** — Spring Framework(**Legacy**)만 사용. `@SpringBootApplication`, starter 의존성, `application.yml` 자동설정 쓰지 말 것.
- **JPA/Hibernate 금지** — DB 연동은 **MyBatis**만.
- **React 금지** — 프론트는 **Vue 3**만.
- **TypeScript 아님** — 프론트는 **JavaScript**. `.ts`/타입 문법 추가하지 말 것.

## 기술 스택

| 구분 | 기술 |
| --- | --- |
| FE | Vue 3, JavaScript, Vite, Pinia, Vue Router, axios, **Tailwind CSS**, **Zod**, **TanStack Query** |
| BE | Java 17, Spring Framework(Legacy), MyBatis, Spring Security, JWT, Gradle, Lombok |
| 데이터 | MySQL 8.4, Redis |
| 인프라 | Docker Compose (로컬), Nginx·AWS(배포) |

## 저장소 구조 (모노레포)

```
finance-match/
├── fe/                 프론트엔드 (Vue 앱, npm)
│   └── src/
│       ├── api/client.js   공통 API 클라이언트 (아래 FE 규칙 참고)
│       ├── pages/          라우트 화면 (XxxPage.vue)
│       ├── router/         Vue Router
│       └── assets/         전역 CSS 등
├── be/                 백엔드 (Spring Legacy, Gradle)
│   └── src/main/java/com/financematch/
│       ├── common/         ApiResponse, ErrorCode (공통 응답)
│       ├── config/         Java Config (Root·Web·Redis·Initializer)
│       ├── exception/      ApiException, GlobalExceptionHandler
│       └── <도메인>/        회원·자산·상품 등 (각 담당자가 추가)
├── docker-compose.yml  로컬 인프라 (MySQL·Redis)
└── lefthook.yml        Git 훅 (커밋 시 FE 린트·포맷 자동)
```

## FE 작업 규칙

- **API 호출은 반드시 `src/api/client.js`의 `api`를 통해서** 한다. raw axios 직접 호출 금지.
  - 공통 래퍼(ApiResponse)는 interceptor가 자동으로 벗긴다 → `const data = await api.get(url)` (학습 때처럼 `.data` 구조분해 하지 말 것).
  - 실패는 `ApiError`로 throw → `try/catch`로 잡고 `e.message`, `e.code`(백엔드 ErrorCode 이름) 사용.
  - JWT는 interceptor가 자동 첨부. 헤더 수동 조작 금지. 토큰 저장은 `setAccessToken`/`clearAccessToken` 헬퍼로만.
- **응답 검증은 Zod**로 (endpoint 함수에서 파싱). 래퍼는 이미 벗겨졌으니 `data`만 파싱.
- **파일명**: 컴포넌트/페이지는 `PascalCase.vue`, 라우트 화면은 `XxxPage.vue`. import는 `@` 별칭(=`src`) 사용.
- **스타일은 Tailwind** 유틸 클래스 우선. 서버 데이터는 **TanStack Query**로 관리.
- **코드 스타일**: Prettier(작은따옴표·세미콜론 없음·printWidth 100) + ESLint. **커밋 시 lefthook이 자동 적용**하므로 신경 쓸 필요 없음. 미사용 변수는 에러(무시하려면 `_` 접두사).

## BE 작업 규칙

- 베이스 패키지 `com.financematch`. 도메인별 하위 패키지로 추가.
- **MyBatis는 XML 설정 파일 없이 Java Config**(`SqlSessionFactoryBean`). `snake_case → camelCase` 자동 매핑 켜져 있음.
- **컨트롤러 성공 응답은 `ApiResponse.ok(data)`로 감싼다.** 실패는 `ApiException`을 던지면 `GlobalExceptionHandler`가 `ApiResponse.fail(...)`로 변환 — 컨트롤러에서 직접 실패 응답 만들지 말 것.
- 새 에러는 `ErrorCode`에 추가 후 사용. Lombok 사용 가능.
- `application-local.properties`는 로컬 더미값이라 **커밋한다**. 실제 비밀값(`application-secret.properties`)은 커밋 금지(.gitignore됨).

## 명령어

```bash
# 인프라 (프로젝트 루트, colima 또는 Docker Desktop 먼저 실행)
docker compose up -d          # MySQL·Redis 띄우기
docker compose down           # 정지 (데이터 유지)

# 백엔드
cd be && ./gradlew build      # 빌드
                              # 실행은 Tomcat(8080)에 배포 / IDE에서 실행

# 프론트엔드
cd fe && npm install          # 의존성 설치 (+ lefthook 훅 자동 설치)
npm run dev                   # 개발 서버 (5173, /api → 8080 프록시)
npm run lint                  # 전체 ESLint --fix
npm run format                # 전체 Prettier
```

## Git

> 컨벤션 확정: 2026-07-20. **도구로 강제(commitlint)하지 않는다** — 이 문서가 기준이니 지켜서 커밋할 것. (lefthook은 코드 포맷만 자동 적용)

### 커밋 메시지

형식:
```
<type>(<scope>): <제목>

<본문>    ← 선택
<꼬리말>  ← 선택 (Closes #12, BREAKING CHANGE 등)
```

- **type (8종, 영어 고정)**: `feat`(기능) · `fix`(버그) · `docs`(문서) · `style`(포맷·로직 X) · `refactor`(구조개선·동작 X) · `perf`(성능) · `test`(테스트) · `chore`(빌드·설정)
- **scope (영어 고정, 고정 목록)**: 지금은 계층 `fe`·`be`·`infra`·`config`·`api`. 도메인 확정 후 `auth`·`member`·`asset`·`product`·`match` 추가. 전역 변경은 **생략 가능**.
- **제목·본문은 한글**. 명령조·현재시제, 첫 글자 소문자, **제목 끝 마침표 없음**. 제목은 짧게(한글 ~30자, 최대 72자).

```
feat(auth): JWT 로그인 기능 추가          ← 도메인 확정 후
feat(fe): 공통 API 클라이언트 추가         ← 지금은 계층 scope
chore: .gitignore 정리                    ← scope 생략
```

### 브랜치

- 기본 브랜치 **`develop`** — 여기서 따서 PR(타겟 `develop`, **리뷰 1명 필수**) → 병합 후 브랜치 삭제. `main`은 안정 완성본만(MVP 시점 통합).
- 이름: **타입 우선**, 소문자+하이픈만(공백·`_`·대문자·한글 금지). 설명은 영어 2~4단어.
  - 이슈 있을 때 `<type>/<이슈번호>-<설명>`, 없을 때 `<type>/<설명>`
  - type: `feature` · `bugfix` · `hotfix` · `refactor` · `docs` · `chore`

```
feature/12-jwt-login    bugfix/34-asset-null-check    chore/add-pr-template
```

### 이슈

- **기능·버그는 이슈로, 오타·포맷 등 잡일은 이슈 없이.** 커밋·PR 꼬리말 `Closes #12`로 이슈를 연결·자동 종료.
- `develop`·`main`은 branch protection: 직접 push 금지, PR+리뷰 1명 필수.

---

_최종 갱신: 2026-07-20 · 담당: 임민지(팀장) · 세팅 상세는 `~/Desktop/종합실무프로젝트/setting/찰떡귱합_*세팅.md` 참고_
