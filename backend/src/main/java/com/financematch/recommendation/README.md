# 추천 기능 계약

추천 기능은 상품 유형별 정책을 독립적으로 구현하고, 공통 조립기가 정책 결과를 합치는 방식으로
개발한다. 이 문서는 추천 정책, 저장 및 조회 API가 지켜야 하는 현재 계약을 정의한다.

## 공통 계약

- 공동 추천 정책은 `JointRecommendationPolicy`를 구현한다.
- 개인 추천 정책은 `PersonalRecommendationPolicy`를 구현한다.
- 정책 입력은 `RecommendationContext`만 사용한다.
- 정책 출력은 `RecommendedProduct`를 사용한다.
- 상품 유형은 문자열 대신 `RecommendationSlotType` 또는 `PersonalRecommendationType`을 사용한다.
- 정책은 추천 결과를 계산하는 역할만 담당하며 DB에 직접 저장하지 않는다.
- 정책 클래스는 Spring Bean으로 등록해 `RecommendationPlanner`가 자동으로 수집하게 한다.
- 같은 추천 유형의 정책을 두 개 이상 등록하지 않는다.

### 공통 모델 위치

- `domain/RecommendationContext`: MyBatis가 조회한 추천 입력 VO
- `policy/RecommendedProduct`: 각 추천 정책이 반환하는 출력 계약
- `service/RecommendationPlan`: `RecommendationPlanner`가 조립한 서비스 결과
- `service/RecommendationPlanner`: 정책 실행과 결과 조립을 담당하는 Spring `@Service`

## 구현 규칙

- 기존 `product` 도메인의 유형별 Mapper와 DTO를 재사용한다.
- 상품 ID에 정책을 하드코딩하지 않고 상품 속성이나 별도 식별 코드를 기준으로 판단한다.
- 추천 순위는 1부터 시작하며 동일 조건일 때의 정렬 기준까지 명시한다.
- 추천할 상품이 없으면 `null` 대신 빈 목록을 반환한다.
- 대표 상품은 유형별로 최대 하나만 `selected=true`로 반환한다.
- 개인 추천 결과 Map의 key는 추천 대상 회원 ID로 사용한다.
- 컨트롤러에서 테스트용 회원 ID를 직접 사용하지 않는다.
- 추천 결과 저장은 모든 정책 실행이 끝난 뒤 하나의 트랜잭션에서 수행한다.

## 테스트 기준

각 정책 테스트는 최소한 다음 항목을 검증한다.

- 조건을 충족하지 않는 상품이 제외되는지
- 추천 순위와 동률 정렬이 결정적으로 유지되는지
- 후보가 없을 때 빈 목록을 반환하는지
- 개인 추천이 각 회원 ID에 맞게 분리되는지
- 대표 상품이 유형별로 하나만 선택되는지

## 추천 이유 계약

- 추천 이유는 각 공동 슬롯과 개인 추천 목록의 추천순 1등 상품에만 생성한다.
- 대안 상품의 추천 이유와 근거 코드는 `null`이다.
- 상품 순위를 결정한 정책이 `RecommendationReasonCode`를 지정한다.
- 사용자 문장은 별도 변환기가 근거 코드를 기준으로 생성하며 정책 안에 직접 작성하지 않는다.
- 추천 이유는 추천 생성 시점에 결과와 함께 저장하고 조회 시 현재 입력정보로 다시 계산하지 않는다.
- 사용자 문장은 마침표를 제외하고 28~34자로 작성한다.

| 근거 코드 | 적용 조건 | 사용자 문장 |
| --- | --- | --- |
| `DEPOSIT_TERM_AND_RATE` | 예금 1등 | 두 분의 목표 기간에 맞고 기본금리가 높은 예금 상품이에요. |
| `SAVINGS_KB_STAR_PRIORITY` | KB스타적금 가입 가능 및 목표기간 조건을 충족해 KB스타적금이 1등 | KB스타적금 가입이 가능하고 목표 기간에도 잘 맞는 상품이에요. |
| `SAVINGS_TERM_AND_RATE` | 일반 적금 1등 | 목표 기간과 월 저축액에 맞고 기본금리가 높은 적금 상품이에요. |
| `INVESTMENT_RETIREMENT_TDF_PRIORITY` | 1순위 목표가 노후이고 TDF가 투자 1등 | 노후 목표와 투자성향에 맞는 TDF 상품을 우선 골랐어요. |
| `INVESTMENT_RISK_AND_AUM` | 일반 투자 1등 | 투자성향에 맞는 위험등급 중 순자산이 큰 상품을 골랐어요. |
| `LOAN_TARGET_GROUP_RATE_AND_CHANNEL` | 대출 1등 | 대출 목적과 조건에 맞고 금리와 신청 편의성이 좋은 상품이에요. |
| `TAX_SAVING_ISA_KNOWLEDGE` | ISA 상품이 개인 절세 1등 | 계좌 현황과 금융 이해도에 잘 맞는 ISA 상품을 골랐어요. |
| `TAX_SAVING_PENSION_RETIREMENT` | 연금저축 상품이 개인 절세 1등 | 노후 목표와 계좌 현황에 잘 맞는 연금저축 상품을 골랐어요. |
| `TAX_SAVING_IRP_INVESTMENT_TYPE` | IRP 상품이 개인 절세 1등 | 노후 목표와 투자성향에 잘 맞는 IRP 상품을 골랐어요. |

## 조회 API 응답 계약

추천 생성 API `POST /v1/members/me/recommendation`은 추천 결과를 저장하고 `data: null`을
반환한다. 프론트는 생성 성공 후 추천 조회 API를 다시 호출한다.

추천 조회 API `GET /v1/members/me/recommendation`은
`dto/RecommendationResponse`를 반환한다.

- `packageSlots`에는 추천 상품이 하나 이상인 활성 공동 슬롯만 포함한다.
- 활성 슬롯의 `selectedProductId`는 해당 슬롯의 `products`에 반드시 포함한다.
- `products`는 추천 순위대로 정렬하며 현재 선택 상품과 추천 순위는 별개로 관리한다.
- 공동·개인 추천 상품의 `recommendationReason`은 추천 생성 시 저장된 문장을 반환한다. 추천순
  1등 상품에만 문자열을 반환하고 대안 상품은 `null`을 반환한다.
- 개인 절세 추천은 로그인 회원의 결과만 반환한다. 추천 상품이 없으면 객체 전체를 `null`로
  반환한다.
- 개인 투자 추천은 로그인 회원의 투자성향이 공동 투자 기준보다 더 적극적인 경우에만 로그인
  회원의 결과를 반환한다.
- 로그인 회원이 공동 투자 기준보다 적극적이지 않거나 두 회원의 투자성향이 같으면
  `personalInvestmentRecommendation`을 `null`로 반환한다. 상대 회원의 개인 투자 추천은
  반환하지 않는다.
- `hasHighInterestDebt`는 조회 시점에 두 회원의 현재 `financial_summary`를 조회해 계산한다.

### 추천 최신성

추천 입력 데이터가 저장된 추천 결과보다 최신이면 기존 결과와 현재 금융 상태를 섞어 반환하지
않는다.

최신성 비교 대상은 회원 추천 입력 수정 시각, 공동·개인 설문, 금융 요약, 연금·ISA 계좌와
추천에 사용하는 상품 정보다. 이 중 최신 수정 시각이 `recommendation.updated_at`보다 뒤라면
GET은 `RECOMMENDATION_NOT_FOUND`를 반환한다. 프론트는 POST로 전체 추천을 다시 생성한 뒤
GET을 재호출한다.

상품 서브타입 테이블에는 수정 시각이 없는 경우가 있으므로 상품 속성을 변경하는 작업은
`product.updated_at`도 함께 갱신하거나 해당 상품을 포함할 수 있는 기존 추천을 명시적으로
무효화해야 한다.

GET 요청 자체에서는 추천 결과를 생성하거나 DB를 변경하지 않는다.

### 로그인과 회원정보 최신성

회원의 일반 수정 시각인 `member.updated_at`은 추천 최신성 판단에 사용하지 않는다. 추천에
영향을 주는 회원정보의 최종 수정 시각은 `member.recommendation_input_updated_at`으로 별도
관리한다.

- 로그인, 이름, 프로필 메시지처럼 추천과 무관한 변경에서는 갱신하지 않는다.
- 개인 설문에서 투자성향을 저장하거나 기존 투자성향을 초기화할 때 현재 시각으로 갱신한다.
- 추천 조회 최신성 검사와 생성 전·후 입력 변경 검사에서는 두 회원의
  `recommendation_input_updated_at`을 비교한다.
- 로그인은 `last_login_at`과 `member.updated_at`을 변경할 수 있지만
  `recommendation_input_updated_at`은 변경하지 않으므로 기존 추천을 무효화하지 않는다.

향후 생년월일(`birth_date`) 또는 KB스타적금 가입 가능 여부
(`kb_star_savings_eligible`)를 수정하는 API를 구현할 때는 같은 회원 수정 트랜잭션에서
`recommendation_input_updated_at`도 반드시 현재 시각으로 갱신한다. 해당 값만 변경하고 추천
입력 수정 시각을 갱신하지 않는 구현은 허용하지 않는다.

### 추천 생성 중 입력 변경

추천 생성은 `READ_COMMITTED` 격리 수준에서 계산 직전과 저장 직후의 추천 입력 최종 수정 시각을
비교한다. 두 시각이 다르면 생성 도중 입력정보가 변경된 것으로 보고 예외를 발생시켜 추천 결과
저장 전체를 롤백한다.

현재 스키마의 `updated_at`은 초 단위이므로 같은 초 안에서 발생한 변경은 감지하지 못할 수 있다.
더 높은 정밀도가 필요해지면 수정 시각 정밀도 또는 별도 버전 컬럼을 개선한다.

### 상품 및 절세 데이터 전제

- 두 회원의 `financial_summary`는 추천 생성의 필수 입력이다. 한 명이라도 금융정보 입력을
  완료하지 않았다면 추천을 생성하지 않고 `RECOMMENDATION_NOT_READY`를 반환한다.
- 추천 생성 시 `tax_eligibility_status`와 `isa_eligibility_status`는 `ELIGIBLE` 또는
  `INELIGIBLE`로 계산 완료되어 있어야 한다. `UNKNOWN`은 준비되지 않은 입력정보로 처리한다.
- 현재는 상품 하나가 변경돼도 전체 추천을 오래된 결과로 판단한다. 상품 변경 빈도와 재생성
  비용이 커지면 관련 상품 또는 상품군 단위로 무효화 범위를 좁힌다.
