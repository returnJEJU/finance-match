# DB 마이그레이션 (Flyway)

찰떡궁합 DB 스키마·시드는 **Flyway**가 관리한다. 컨벤션 정본은 루트 [`AGENTS.md`](../../../../../AGENTS.md).

---

## ⚠️ 먼저 읽을 것 — 지키지 않으면 팀 전체가 막힌다

### 1. 이미 올라간 `V` 파일은 절대 수정하지 않는다

내가 만든 것이든 **다른 사람이 만든 것이든** 똑같다. `develop`에 머지된 `V...sql`은 건드리지 않는다.

Flyway는 적용할 때 파일 내용의 **체크섬**을 `flyway_schema_history`에 기록한다. 파일이 한 글자라도 바뀌면 기록과 어긋나서, **이미 적용한 사람 전원의 DB에서** 다음 마이그레이션이 거부된다.

```
Validate failed: Migration checksum mismatch for migration version 20260726.1640
```

내 PC에서는 멀쩡해 보인다. 아직 적용 안 한 사람은 새 내용으로 잘 돌기 때문이다. **문제는 이미 적용한 팀원에게서 터진다.**

### 2. 잘못됐으면 고치지 말고, 되돌리는 새 파일을 추가한다

컬럼 타입을 잘못 잡았다면 그 파일을 수정하는 게 아니라, **다음 번호의 새 파일**로 덮어쓴다.

```sql
-- ❌ V20260727_1030__add_nickname.sql 을 열어서 VARCHAR(30) → VARCHAR(50) 로 수정
-- ✅ V20260727_1415__widen_nickname.sql 을 새로 만든다
ALTER TABLE member MODIFY COLUMN nickname VARCHAR(50) NULL COMMENT '닉네임';
```

DB 이력은 "지나간 것을 고치는" 게 아니라 **"위에 계속 쌓는"** 방식이다. 틀린 것도 이력으로 남기고 그다음 파일로 바로잡는다.

### 3. 시드 파일(`R__`)은 예외 — 직접 수정한다

| 파일 | 수정 | 이유 |
|---|---|---|
| `V...sql` | ❌ 금지 | 한 번 적용하면 끝. 체크섬 고정 |
| `R__...sql` | ✅ 가능 | 내용이 바뀌면 자동으로 다시 적용 |

### 4. `docker compose down -v` 는 이제 거의 쓰지 않는다

예전에는 스키마를 고칠 때마다 필요했지만, 지금은 `./gradlew flywayMigrate` 하나면 **데이터를 유지한 채** 변경분만 적용된다.

---

## 1. 파일 구성

```
backend/src/main/resources/db/
├── migration/                              운영 포함
│   ├── V20260726_1640__init_schema.sql     24테이블 구조(DDL)
│   └── R__01_seed_product.sql              KB 상품 40 + 연령대별 금융자산 중앙값
└── dev-seed/                               로컬 전용
    ├── R__02_seed_demo.sql                 데모 커플 1쌍 + 설문 + 점수/리포트/추천
    └── R__03_seed_demo_couples.sql         데모 커플 3쌍용 회원 6명 (회원만)
```

`R__02` 와 `R__03` 은 방식이 다르다. `R__02` 는 결과(점수·리포트·추천)까지 손으로 적어둔 고정
픽스처고, `R__03` 은 **입력의 출발점만** 만들고 나머지는 앱이 실제로 계산하게 한다. 새로 만드는
데모는 `R__03` 방식을 따른다 — 손으로 적은 결과는 계산기·추천정책이 내놓는 답과 어긋나기 쉽다.

| 접두어 | 뜻 | 실행 시점 |
|---|---|---|
| `V<날짜>_<시각>__<설명>` | Versioned — 버전 마이그레이션 | 아직 적용 안 된 것만 **한 번** |
| `R__<번호>_<설명>` | Repeatable — 반복 실행 | 내용(체크섬)이 **바뀔 때마다** |

### 파일명 규칙

```
V20260727_1030__add_member_nickname.sql
 └──날짜──┘ └시각┘└┘└─────설명─────┘
                   밑줄 2개 (버전과 설명의 구분자)
```

- 버전은 **타임스탬프**로 짓는다. 팀원끼리 번호가 겹치지 않는다.
- 설명 앞은 반드시 **밑줄 2개**. 1개면 Flyway가 파일을 인식하지 못한다.
- 설명은 영문 소문자 + 밑줄.

### `R__` 앞의 `01_`·`02_` 는 지우지 말 것

Flyway는 `R__` 파일을 **설명의 알파벳순**으로 실행한다. 접두어가 없으면 `seed_demo` < `seed_product` 라서 데모가 먼저 돌고 **FK 오류로 깨진다.** 번호가 `상품 → 데모` 순서를 강제한다.

### 파일 맨 앞의 `DROP TABLE IF EXISTS` 도 지우지 말 것

MySQL은 DDL이 트랜잭션이 아니라, 마이그레이션이 중간에 실패하면 스키마가 반쯤 만들어진 채 남는다. 이때 `flywayRepair` 후 재실행하는데, 맨 앞의 `DROP IF EXISTS` 덕분에 깨끗하게 다시 만들어진다.

---

## 2. 로컬 개발 (일상)

```bash
docker compose up -d              # 프로젝트 루트에서. MySQL·Redis 기동 (빈 DB 로 뜬다)
cd backend && ./gradlew flywayMigrate  # 스키마·시드 적용
```

- 컨테이너에는 **스키마가 들어있지 않다.** Flyway가 채운다.
- IDE에서 앱을 기동하면 `flyway.enabled=true` 설정에 따라 **자동으로도 적용**된다.
- 몇 번을 돌려도 안전하다. 적용할 게 없으면 아무 일도 일어나지 않는다.
- DB: `finance_match` · `mysql`(3306) · `redis`(6379)

### 팀원이 스키마를 바꿨을 때

```bash
git pull
cd backend && ./gradlew flywayMigrate
```

`down -v` 불필요. **내 데이터는 유지된다.**

### 데모 로그인 계정 (둘 다 비번 `Test1234!`)

| 이메일 | 이름 | 성향 | 역할 |
|---|---|---|---|
| `demo.a@chaltteok.dev` | 김하나 | 안정추구형 | 초대자 |
| `demo.b@chaltteok.dev` | 이두리 | 적극투자형 | 피초대자 |

> couple id = 1 · 초대코드 `DEMO2026` · 데모 궁합점수 **73.15**

### 데모 커플 3쌍용 계정 (`R__03` · 비번 동일 `Test1234!`)

회원만 들어 있다. 자산연동·설문·커플연결은 **앱에서 직접** 해야 궁합점수·리포트·추천이 생성된다.
자산은 `SimpleMyDataProvider` 가 이메일로 찾아 채운다(상세는 `asset/README.md`).

| 이메일 | 이름 | 특징 | 기본 조합 |
|---|---|---|---|
| `demo.saver@` | 김보람 | 자산 1.2억 · 전세대출 6,000만 · KB스타적금 가입가능 | 커플1 |
| `demo.investor@` | 박준서 | 자산 1.23억 · 신용대출 2,000만 | 커플1 |
| `demo.newlywed@` | 최유진 | 자산 350만 · 전세대출 1.2억 | 커플2 |
| `demo.renter@` | 정민호 | 자산 240만 · 대출 2건 5,700만 | 커플2 |
| `demo.rich@` | 한서연 | 자산 3억 · 무부채(8명 중 유일) | 커플3 |
| `demo.debt@` | 오태윤 | 자산 250만 · 카드론 17.9% 고금리 · KB스타적금 가입가능 | 커플3 |

기본 조합은 권장일 뿐이고, 초대코드로 아무나 짝지어도 된다 — 자산은 사람을 따라간다.

> ⚠️ `R__03` 을 고쳐서 다시 적용하면 3~8번 회원의 설문·커플연결·점수·리포트·추천이 **모두 지워진다.**
> 앱에서 만들어 둔 내용이 날아간다(리셋 용도로 일부러 쓰기도 한다). 1·2번 회원은 영향받지 않는다.

---

## 3. 스키마를 바꿀 때

1. `db/migration/`에 **새 파일**을 만든다 (기존 파일 수정 금지 — 위 ⚠️ 참고)

   ```
   V20260727_1030__add_member_nickname.sql
   ```

   ```sql
   ALTER TABLE member ADD COLUMN nickname VARCHAR(30) NULL COMMENT '닉네임';
   ```

2. 내 DB에 적용해 본다

   ```bash
   cd backend && ./gradlew flywayMigrate
   ```

3. 확인

   ```bash
   ./gradlew flywayInfo
   ```

4. 커밋 → PR

---

## 4. 시드 데이터를 바꿀 때

`R__01_seed_product.sql`·`R__02_seed_demo.sql`은 **파일을 직접 수정**한다. 새 파일을 만들 필요 없다. 다음 `flywayMigrate`에서 체크섬 변경이 감지돼 자동으로 다시 적용된다.

두 파일 모두 맨 앞에 `DELETE`가 있어 재실행해도 중복되지 않는다.

> ⚠️ **주의** — `R__`는 **자기 체크섬이 바뀔 때만** 다시 돈다. 상품 시드를 고쳐서 **상품 ID가 바뀌면**, 그 ID를 참조하는 데모 시드는 자동으로 돌지 않아 FK가 어긋난다. 상품 ID를 바꿨다면 데모 시드도 한 번 다시 적용할 것:
>
> ```bash
> docker exec -i finance-match-mysql mysql -uroot -proot finance_match \
>   < backend/src/main/resources/db/dev-seed/R__02_seed_demo.sql
> ```

---

## 5. 완전 초기화 (거의 쓸 일 없음)

DB가 심하게 꼬였을 때만.

```bash
docker compose down -v && docker compose up -d
cd backend && ./gradlew flywayMigrate
```

`-v`는 볼륨 삭제(데이터 완전 소멸)다. 평소 스키마 변경에는 **필요 없다.**

---

## 6. 운영 배포

```bash
cd backend && ./gradlew flywayMigrate -Pprod
```

`-Pprod`를 붙이면 `dev-seed`(데모 커플)를 **제외**하고 `migration`만 적용한다. WAR 빌드 시에도 `dev-seed` 파일이 물리적으로 포함되지 않는다.

운영은 앱 기동 시 자동 적용을 **끈다**(`flyway.enabled=false`). 인스턴스가 여러 개 뜰 때 동시에 마이그레이션이 돌아 충돌하는 것을 막기 위해, 배포 파이프라인이 위 명령으로 **한 번만** 적용한다.

| | 로컬 | 운영 |
|---|---|---|
| 적용 폴더 | `migration` + `dev-seed` | `migration`만 |
| 앱 기동 시 자동 | ✅ | ❌ (파이프라인이 적용) |
| 데모 커플 | 있음 | 없음 |

---

## 7. 내 DB 상태 확인

`git pull` 후 "내 DB가 최신인가?" 가 궁금하면 이것부터 돌린다.

```bash
cd backend && ./gradlew flywayInfo
```

```
Schema version: 20260726.1640
+------------+---------------+-----------------+------+---------------------+---------+
| Category   | Version       | Description     | Type | Installed On        | State   |
+------------+---------------+-----------------+------+---------------------+---------+
| Versioned  | 20260726.1640 | init schema     | SQL  | 2026-07-26 18:05:25 | Success |
| Repeatable |               | 01 seed product | SQL  | 2026-07-26 18:05:25 | Success |
| Repeatable |               | 02 seed demo    | SQL  | 2026-07-26 18:05:25 | Success |
+------------+---------------+-----------------+------+---------------------+---------+
```

### 읽는 법

- 맨 위 `Schema version` = **내 DB의 현재 버전**
- 판단은 **`State` 열**로 한다

| State | 뜻 | 조치 |
|---|---|---|
| `Success` | 적용 완료 | 없음 |
| **`Pending`** | 파일은 받았는데 **내 DB엔 아직 안 들어감** | `./gradlew flywayMigrate` |
| `Failed` | 적용하다 실패 | `./gradlew flywayRepair` 후 재시도 |

> **`Pending`이 하나라도 있으면 내 DB가 뒤처진 것**, 하나도 없으면 최신이다.

SQL로 직접 볼 수도 있다.

```sql
SELECT installed_rank, version, description, success FROM flyway_schema_history ORDER BY installed_rank;
```

### 자주 쓰는 명령

```bash
cd backend
./gradlew flywayInfo       # 현재 상태 · 대기중 목록
./gradlew flywayMigrate    # 적용
./gradlew flywayValidate   # 변조 검사만
./gradlew flywayRepair     # 실패·체크섬 기록 복구
```

---

## 8. 트러블슈팅

**`Validate failed: Migration checksum mismatch`**
이미 적용된 `V` 파일이 수정됐다. **원인 제거가 우선** — 파일을 원래대로 되돌리고 변경은 새 마이그레이션으로 만든다. 정말 기록을 맞춰야 하면 `./gradlew flywayRepair && ./gradlew flywayMigrate`.

**마이그레이션이 중간에 실패하고 `success=0`으로 남음**
MySQL은 DDL 롤백이 안 돼 부분 적용 상태로 남는다. `./gradlew flywayRepair` → SQL 수정 → `./gradlew flywayMigrate`.

**`Unable to obtain connection ... Communications link failure`**
MySQL 컨테이너가 아직 안 떴다. `docker compose ps`로 healthy 확인 후 재시도.

**`flywayInfo`에 데모 시드가 안 보임**
`-Pprod`가 붙었다.

---

## 9. 팀 확정 필요 (합의 후 시드 수정)

- **`member.investment_type` 코드값** — 스키마 미정의. `R__02_seed_demo.sql`은 `STABLE / STABLE_SEEKING / NEUTRAL / AGGRESSIVE / VERY_AGGRESSIVE`로 **가정**(VARCHAR(20) 한도). 확정 시 member 2행·couple 1행 수정.
- **가치관 Q2(투자경험 다중응답) 점수화** — `R__02_seed_demo.sql`은 '최고위험 등급'으로 가정. 계산엔진 방식과 맞출 것.

## 10. 데이터 노트

- `deposit_rate`: 상품 1·2·7만 실데이터, 3·4·5·6은 임시(추후 금감원 finlife API 적재).
- `age_group_asset_median`: 2025 가계금융복지조사(KOSIS) 금융자산 중앙값.
- `investment.aum`: **억 단위 INT**(원 아님).
