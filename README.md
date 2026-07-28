# 찰떡귱합 (finance-match)

부부·커플이 서로의 자산을 상세히 공개하지 않고도 두 사람의 금융 데이터를 결합해
공동 금융 상태를 진단하고, **금융궁합도(100점)** 로 제시한 뒤 맞춤 금융상품을 패키지로 추천하는 서비스.

> Team `return JEJU;` · KB IT's Your Life 7기 종합실무 프로젝트
> 주제 (3) 재테크를 위한 금융 상품 비교 추천 서비스

## 기술 스택

| 구분 | 기술 |
| --- | --- |
| 프론트엔드 | Vue 3, JavaScript, Vite, Pinia, axios, Vue Router, Tailwind CSS, Zod, TanStack Query |
| 백엔드 | Java 17, Spring Framework (Legacy), MyBatis, Spring Security, JWT, Gradle |
| 데이터 | MySQL 8.4, Redis |
| 인프라 | Docker Compose, Nginx, AWS EC2/RDS |

> 과정 규정: **Spring Boot / JPA / React 사용 불가**

## 폴더 구조

```
finance-match/
├── frontend/   프론트엔드 (Vue 앱)
└── backend/    백엔드 (Spring Legacy)
```

## 로컬 실행 방법

> 준비물: Node.js(22+), JDK 17, Docker(Docker Desktop 또는 colima), Git

```bash
# 1. 저장소 클론
git clone <repo-url>
cd finance-match

# 2. 인프라 실행 (로컬은 MySQL · Redis 만 — Nginx는 배포 단계)
#    MySQL 은 빈 DB 로 뜬다. 스키마·시드는 다음 단계의 Flyway 가 넣는다.
docker compose up -d

# 3. DB 스키마 적용 + 백엔드 빌드
cd backend && ./gradlew flywayMigrate && ./gradlew build

# 4. 프론트엔드 실행
cd frontend && npm install && npm run dev
```

> 상세 실행 방법은 각 단계 세팅이 끝나면 채워집니다.

## 브랜치 전략

Git Flow. `main`(안정) · `develop`(개발, 기본) · `feature`(기능별).
개발은 `develop`에서 기능 브랜치를 따서 진행하고 PR로 병합합니다.
