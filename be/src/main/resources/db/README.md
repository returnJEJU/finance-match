# DB 초기화 (schema · seed)

찰떡궁합 DB를 **Docker MySQL**로 세우는 방법. 컨벤션 정본은 루트 [`AGENTS.md`](../../../../../AGENTS.md).

---

## 1. 파일 구성 (적재 순서 = 파일명 순서)

| 파일 | 내용 | 운영 반영 |
|---|---|---|
| `01-schema.sql` | 24테이블 **구조**(DDL) | ✅ |
| `02-seed-product.sql` | KB 상품 38 + 서브타입 + `deposit_rate` + **연령대별 금융자산 중앙값** | ✅ 운영 참조데이터 |
| `03-seed-demo.sql` | **데모 커플 1쌍** + 마이데이터 + 설문 + 점수/리포트/추천 | ❌ **개발 전용(운영 제외)** |

- 반드시 **`01 → 02 → 03`** 순서로 적재된다(FK 의존). 숫자 접두어가 순서를 보장한다.
- 마운트 위치: 루트 `docker-compose.yml`(01·02) + `docker-compose.override.yml`(03) → 컨테이너 `/docker-entrypoint-initdb.d/`.

---

## 2. 로컬 개발 (각자 PC에서)

프로젝트 **루트**에서 실행한다(compose 파일이 루트에 있음). 공유 서버가 아니라 로컬 격리 컨테이너다.

```bash
docker compose up -d      # override 자동 병합 → mysql 에 01·02·03 적재, redis 기동
```

- 최초 기동(빈 볼륨)에만 sql이 적재된다 → 데모 커플이 채워진다.
- 이후 `up/down`(볼륨 유지)은 **재적재하지 않는다**(기존 데이터 유지).
- DB: `finance_match` · 서비스: `mysql`(3306) · `redis`(6379).

### 데모 로그인 계정 (둘 다 비번 `Test1234!`)
| 이메일 | 이름 | 성향 | 역할 |
|---|---|---|---|
| `demo.a@chaltteok.dev` | 김하나 | 안정추구형 | 초대자 |
| `demo.b@chaltteok.dev` | 이두리 | 적극투자형 | 피초대자 |

> couple id = 1 · 초대코드 `DEMO2026` · 데모 궁합점수 **73.15**

---

## 3. 초기화 / 재적재 (리셋)

`03`을 수정했거나 데이터가 꼬였을 때:

```bash
docker compose down -v    # ⚠️ -v = 볼륨 삭제(데이터 완전 초기화)
docker compose up -d      # 다음 기동에서 01·02·03 다시 적재
```

- **`-v`가 핵심.** 볼륨이 살아있으면 initdb sql이 다시 안 돈다.
- `03`만 다시 넣고 싶으면(재실행 안전 — 맨 앞 DELETE→INSERT):
  ```bash
  docker exec -i finance-match-mysql mysql -uroot -proot finance_match \
    < be/src/main/resources/db/03-seed-demo.sql
  ```

---

## 4. 운영 배포 (03 제외)

파일을 지우지 않는다. **override(=03)를 빼면** 운영엔 01·02만 적재된다.

- **로컬**: `docker compose up` → `docker-compose.override.yml` 자동 병합 → **01·02·03**
- **운영**: `docker compose -f docker-compose.yml up` (override 미포함) → **01·02만**

> 운영 compose(Nginx·앱 포함)는 배포 단계에서 별도 구성 예정(AGENTS.md 참고). 그때 base(`docker-compose.yml`)의 01·02 마운트를 재사용하고 override는 로컬에만 둔다.

---

## 5. 전체 사이클

```
git clone / pull  (01,02,03,compose 모두 레포에 있음)
        │
   ┌────┴─────────────────────┐
   ▼ 로컬 개발                 ▼ 운영 배포(추후)
 docker compose up            docker compose -f docker-compose.yml up
 (override 자동 → 01+02+03)    (override 제외 → 01+02)
   → 데모 커플 O               → 실데이터만, 데모 X
 리셋: down -v → up
```

---

## 6. 팀 확정 필요 (적재 전 합의)

- **`member.investment_type` 코드값** — 스키마 미정의. `03`은 `STABLE / STABLE_SEEKING / NEUTRAL / AGGRESSIVE / VERY_AGGRESSIVE`로 **가정**(VARCHAR(20) 한도). 확정 시 `03`의 member 2행·couple 1행 수정.
- **가치관 Q2(투자경험 다중응답) 점수화** — `03`은 '최고위험 등급'으로 가정. 계산엔진 방식과 맞출 것.

## 7. 데이터 노트

- `deposit_rate`: 상품 1·2·7만 실데이터, 3·4·5·6은 임시(추후 금감원 finlife API 적재).
- `age_group_asset_median`: 2025 가계금융복지조사(KOSIS) 금융자산 중앙값.
- `investment.aum`: **억 단위 INT**(원 아님).
