# 추천 기능 협업 가이드

추천 기능은 상품 유형별 정책을 독립적으로 구현하고, 공통 조립기가 정책 결과를 합치는 방식으로
개발한다. 이 문서는 추천 기능을 병렬로 작업할 때 지켜야 하는 공통 계약과 담당 범위를 정의한다.

## 담당 범위

| 담당 | 구현 범위 |
| --- | --- |
| 예적금 담당 | `DEPOSIT`, `SAVINGS` 공동 추천 정책 |
| 비예적금 담당 | `LOAN`, 공동·개인 `INVESTMENT`, 개인 `TAX_SAVING` 추천 정책 |
| 통합 담당 | 추천 결과 저장, 조회 API, 최종 응답 조립 |

각 담당자는 자신의 정책 클래스와 정책 테스트를 함께 작성한다. 다른 담당자의 정책 파일은 같은
PR에서 수정하지 않는다.

## 공통 계약

- 공동 추천 정책은 `JointRecommendationPolicy`를 구현한다.
- 개인 추천 정책은 `PersonalRecommendationPolicy`를 구현한다.
- 정책 입력은 `RecommendationContext`만 사용한다.
- 정책 출력은 `RecommendedProduct`를 사용한다.
- 상품 유형은 문자열 대신 `RecommendationSlotType` 또는 `PersonalRecommendationType`을 사용한다.
- 정책은 추천 결과를 계산하는 역할만 담당하며 DB에 직접 저장하지 않는다.
- 정책 클래스는 Spring Bean으로 등록해 `RecommendationPlanner`가 자동으로 수집하게 한다.
- 같은 추천 유형의 정책을 두 개 이상 등록하지 않는다.

공통 계약을 변경해야 한다면 정책 구현 PR과 섞지 않고 별도 PR로 먼저 합의하고 병합한다.

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

## 브랜치 및 병합 순서

1. 최신 `develop`에서 담당별 기능 브랜치를 만든다.
2. 예적금과 비예적금 정책을 각 브랜치에서 독립적으로 구현한다.
3. 먼저 병합된 PR이 있으면 나머지 담당자는 최신 `develop`을 반영한다.
4. 모든 정책이 병합된 뒤 통합 브랜치에서 저장·조회 API를 연결한다.
5. 통합 테스트와 전체 백엔드 빌드를 통과한 뒤 최종 PR을 올린다.

권장 브랜치 이름은 다음과 같다.

```text
feature/deposit-recommendation
feature/non-deposit-recommendation
feature/recommendation-integration
```

## 테스트 기준

각 정책 테스트는 최소한 다음 항목을 검증한다.

- 조건을 충족하지 않는 상품이 제외되는지
- 추천 순위와 동률 정렬이 결정적으로 유지되는지
- 후보가 없을 때 빈 목록을 반환하는지
- 개인 추천이 각 회원 ID에 맞게 분리되는지
- 대표 상품이 유형별로 하나만 선택되는지

## 조회 API 응답 계약

추천 생성 API `POST /v1/members/me/recommendation`은 추천 결과를 저장하고 `data: null`을
반환한다. 프론트는 생성 성공 후 추천 조회 API를 다시 호출한다.

추천 조회 API `GET /v1/members/me/recommendation`은
`dto/RecommendationResponse`를 반환한다.

- `packageSlots`에는 추천 상품이 하나 이상인 활성 공동 슬롯만 포함한다.
- 활성 슬롯의 `selectedProductId`는 해당 슬롯의 `products`에 반드시 포함한다.
- `products`는 추천 순위대로 정렬하며 현재 선택 상품과 추천 순위는 별개로 관리한다.
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

최신성 비교 대상은 회원 투자성향, 공동·개인 설문, 금융 요약, 연금·ISA 계좌와 추천에 사용하는
상품 정보다. 이 중 최신 수정 시각이 `recommendation.updated_at`보다 뒤라면 GET은
`RECOMMENDATION_NOT_FOUND`를 반환한다. 프론트는 POST로 전체 추천을 다시 생성한 뒤 GET을
재호출한다.

상품 서브타입 테이블에는 수정 시각이 없는 경우가 있으므로 상품 속성을 변경하는 작업은
`product.updated_at`도 함께 갱신하거나 해당 상품을 포함할 수 있는 기존 추천을 명시적으로
무효화해야 한다.

GET 요청 자체에서는 추천 결과를 생성하거나 DB를 변경하지 않는다.
