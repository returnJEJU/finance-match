# 자산 연동 목데이터

자산 연동은 커플 합산 연동이 아니라 회원별 개인 연동이다. 각 회원이 자신의 JWT로
`POST /v1/members/me/assets`를 호출하면 자신의 `member_id`에만 금융 요약이 저장된다.
궁합 계산과 공동 추천 단계에서 두 회원의 개인 요약을 조회해 합산한다.

## 회원과 자산은 이메일로 묶인다

실제 마이데이터는 사람을 식별해 그 사람의 계좌를 돌려준다. 목 구현도 같은 모양이어야 하므로
`SimpleMyDataProvider`가 **이메일**로 회원과 시나리오(`MyDataScenario`)를 연결한다.

이메일을 키로 쓰는 이유는 회원 번호와 달리 **가입할 때 직접 정할 수 있기** 때문이다. 데모 계정을
시드로 미리 넣든 시연 중에 그 자리에서 가입하든 같은 자산이 붙는다. 매핑에 없는 이메일은
`DEFAULT` 시나리오를 받으므로 등록되지 않은 계정도 연동에 실패하지 않는다.

자산이 사람에게 붙어 있으므로 **커플 조합을 바꿔도 각자의 자산은 그대로 따라간다.**

## 시나리오 목록

| 이메일 | 시나리오 | 금융자산 | 부채 | 입출금 | 절세계좌 |
| --- | --- | ---: | ---: | ---: | --- |
| `demo.a@` | `DEMO_INVITER` | 90,000,000 | 20,000,000 | 4,000,000 | 연금·ISA |
| `demo.b@` | `DEMO_INVITEE` | 180,000,000 | 40,000,000 | 6,000,000 | 연금·IRP·ISA |
| `demo.saver@` | `SAVER` | 120,000,000 | 60,000,000 | 15,000,000 | 연금 |
| `demo.investor@` | `INVESTOR` | 123,000,000 | 20,000,000 | 8,000,000 | IRP·ISA |
| `demo.newlywed@` | `NEWLYWED` | 3,500,000 | 120,000,000 | 1,200,000 | 없음 |
| `demo.renter@` | `RENTER` | 2,400,000 | 57,000,000 (2건) | 900,000 | 없음 |
| `demo.rich@` | `RICH` | 300,000,000 | 0 | 40,000,000 | 연금·IRP |
| `demo.debt@` | `HIGH_RATE_DEBT` | 2,500,000 | 15,000,000 **고금리** | 800,000 | 없음 |
| (그 외) | `DEFAULT` | 40,000,000 | 10,000,000 | 5,000,000 | 없음 |

도메인은 모두 `@chaltteok.dev`다.

### 시나리오가 추천을 어떻게 움직이나

- **입출금 합계 910만원 이상**이어야 예금 슬롯이 켜진다. `NEWLYWED`+`RENTER` 조합은 210만원이라 꺼진다.
- **고금리 부채가 하나라도 있으면** 대출 슬롯이 통째로 꺼진다. `HIGH_RATE_DEBT`가 그 역할을 한다.
- **연금·IRP·ISA 보유 여부**로 개인 절세 추천 대상이 갈린다. 이미 가진 계좌는 추천하지 않는다.
- `RENTER`는 대출이 2건이라 가중평균 금리(7.87%) 계산 경로를 지난다.
- 무부채는 `RICH` 한 명뿐이다. 따라서 "두 분 모두 부채가 없어요" 리포트 문구는 현재 조합으로는 볼 수 없다.

## 기존 데모 커플은 시드와 값이 일치해야 한다

`DEMO_INVITER`·`DEMO_INVITEE`의 합계는 `db/dev-seed/R__02_seed_demo.sql`의
`financial_summary`·`pension_isa_account` 값과 같다. 어긋나면 두 가지가 깨진다.

1. 자산 조회 화면의 **총액(DB)과 항목별 합계(provider)가 달라진다**.
2. 자산 갱신(`POST /assets/refresh`)이 DB를 덮어써 **데모 궁합 점수(73.15)의 전제가 무너진다**.

`SimpleMyDataProviderTest`의 `demoInviterMatchesSeededFinancialSummary`·
`demoInviteeMatchesSeededFinancialSummary`가 이 회귀를 막는다. 시드나 시나리오 중 하나를 고치면
반드시 다른 쪽도 함께 고칠 것.

## KB스타적금Ⅲ 가입 자격은 별도로 넣어야 한다

`member.kb_star_savings_eligible`은 마이데이터가 아니라 회원 컬럼이고, **회원가입에서 입력받지
않아 항상 기본값 0**으로 만들어진다(수정 API도 없다). 적금 추천에서 KB스타적금Ⅲ를 1등으로 띄우려면
직접 넣어야 한다.

```sql
UPDATE member SET kb_star_savings_eligible = 1
 WHERE email IN ('demo.saver@chaltteok.dev', 'demo.debt@chaltteok.dev');
```

## 알려진 한계

`GET /v1/members/me/assets`는 총액·부채를 DB에서 읽고 **카테고리별 금액과 연금·ISA 보유 여부는
provider를 다시 호출해** 채운다. `financial_summary`에 카테고리별 금액을 저장할 컬럼이 없어서다.
연금·ISA 보유 여부는 `pension_isa_account`에 이미 있으므로 DB에서 읽도록 바꿀 수 있다. 카테고리별
금액까지 DB로 옮기려면 마이그레이션이 필요하다.

## 목 서버로 교체할 때

`SimpleMyDataProvider`와 `MyDataScenario`를 함께 지우고 `MyDataProvider`의 HTTP 구현을 넣는다.
이메일 매핑표는 그때 통째로 사라진다. 컨트롤러·서비스·매퍼는 바뀌지 않는다.
