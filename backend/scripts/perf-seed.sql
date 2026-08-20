-- =====================================================================
-- 성능 테스트용 대량 시드 — perf-seed.sql
--
-- Flyway 대상이 아니다(resources 밖, backend/scripts/). 필요할 때 mysql
-- 클라이언트로 직접 실행한다 — 절대 WAR에 안 들어가고, 운영에 자동 반영될 일도 없다.
--
-- 【하는 일】
--   회원가입 API를 N번 호출하는 대신, 이미 회원가입·설문·매칭·리포트 생성까지
--   전부 끝난 "완성된 커플" N쌍을 SQL로 직접 INSERT한다. 비밀번호 해싱·검증 같은
--   비즈니스 로직을 거치지 않으니 몇만 건도 몇 초 안에 끝난다.
--   커플 1쌍 = member 2 · member_agreement 2 · personal_survey 2 ·
--             personal_survey_investment_experience 2 · financial_summary 2 ·
--             pension_isa_account 2 · common_survey 1 · invitation_code 1 ·
--             couple 1 · compatibility_result 1 · report 1  (총 15행)
--
-- 【범위 밖】
--   recommendation 계열(추천 상품)은 안 넣는다 — product_id를 실제 카탈로그와
--   맞춰야 해서 복잡도가 확 올라가는데, GET /v1/members/me/report 같은 리포트
--   조회 경로는 이 테이블들을 안 거친다. 추천 탭 성능까지 보고 싶으면 별도로 요청.
--
-- 【실행 방법】
--   docker exec -i fm-mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" finance_match' \
--     < backend/scripts/perf-seed.sql
--
-- 【치우는 방법】
--   backend/scripts/perf-seed-cleanup.sql 참고 (email 도메인 loadtest.local 기준으로 삭제)
--
-- 【로그인해서 확인하고 싶으면】
--   비밀번호는 전부 Test1234! (dev-seed 데모 계정과 동일한 해시를 재사용했다 — 이미
--   실제 앱에서 검증된 해시라 안전하다). 이메일은 perf<N>a@loadtest.local 형태.
-- =====================================================================

SET NAMES utf8mb4;

-- 몇 쌍을 만들지 여기만 바꾸면 된다.
SET @couple_count = 5000;

-- 재귀 CTE 기본 한도(1000)보다 크게 잡아야 @couple_count가 그 이상일 때 안 잘린다.
SET SESSION cte_max_recursion_depth = 100000;

-- 기존 데이터와 절대 안 겹치도록, 지금 각 테이블의 최댓값 이후부터 채운다.
SET @member_base          = (SELECT IFNULL(MAX(id), 0) FROM member);
SET @survey_base          = (SELECT IFNULL(MAX(id), 0) FROM personal_survey);
SET @psie_base            = (SELECT IFNULL(MAX(id), 0) FROM personal_survey_investment_experience);
SET @common_survey_base   = (SELECT IFNULL(MAX(id), 0) FROM common_survey);
SET @invitation_base      = (SELECT IFNULL(MAX(id), 0) FROM invitation_code);
SET @couple_base          = (SELECT IFNULL(MAX(id), 0) FROM couple);
SET @fs_base              = (SELECT IFNULL(MAX(id), 0) FROM financial_summary);
SET @pia_base             = (SELECT IFNULL(MAX(id), 0) FROM pension_isa_account);
SET @compat_base          = (SELECT IFNULL(MAX(id), 0) FROM compatibility_result);
SET @report_base          = (SELECT IFNULL(MAX(id), 0) FROM report);

-- 데모 계정(dev-seed)과 동일한 해시. 평문 비밀번호: Test1234!
SET @seed_password_hash = '$2b$10$S7kxCvVErKro6iMGGYtqieadaYQh/mlP/3AOB6q8G81Dl2MoyJeBG';

-- ─── member (커플당 2명: a=inviter, b=invitee) ────────────────────────
INSERT INTO member
    (id, email, password, name, gender, birth_date, status, last_login_at,
     investment_type, kb_star_savings_eligible, recommendation_input_updated_at,
     created_at, updated_at, deleted_at)
WITH RECURSIVE seq AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < @couple_count
)
SELECT
    @member_base + (n * 2 - 1),
    CONCAT('perf', n, 'a@loadtest.local'),
    @seed_password_hash,
    CONCAT('부하테스트', n, 'A'),
    IF(n % 2 = 0, 'F', 'M'),
    DATE_ADD('1985-01-01', INTERVAL (n % 5000) DAY),
    'ACTIVE',
    NOW(),
    ELT(1 + (n % 5), 'STABLE', 'STABLE_SEEKING', 'NEUTRAL', 'AGGRESSIVE', 'VERY_AGGRESSIVE'),
    n % 2,
    NOW(), NOW(), NOW(), NULL
FROM seq
UNION ALL
SELECT
    @member_base + (n * 2),
    CONCAT('perf', n, 'b@loadtest.local'),
    @seed_password_hash,
    CONCAT('부하테스트', n, 'B'),
    IF(n % 2 = 0, 'M', 'F'),
    DATE_ADD('1985-01-01', INTERVAL ((n * 7) % 5000) DAY),
    'ACTIVE',
    NOW(),
    ELT(1 + ((n + 2) % 5), 'STABLE', 'STABLE_SEEKING', 'NEUTRAL', 'AGGRESSIVE', 'VERY_AGGRESSIVE'),
    (n + 1) % 2,
    NOW(), NOW(), NOW(), NULL
FROM seq;

-- ─── member_agreement (커플당 2행) ────────────────────────────────────
INSERT INTO member_agreement
    (member_id, agree_mydata_terms, agree_privacy, agree_asset_link, agree_couple_share,
     agree_marketing, agreed_at, created_at, updated_at)
WITH RECURSIVE seq AS (
    SELECT 1 AS n UNION ALL SELECT n + 1 FROM seq WHERE n < @couple_count
)
SELECT @member_base + (n * 2 - 1), 1, 1, 1, 1, n % 2, NOW(), NOW(), NOW() FROM seq
UNION ALL
SELECT @member_base + (n * 2),     1, 1, 1, 1, n % 2, NOW(), NOW(), NOW() FROM seq;

-- ─── personal_survey (커플당 2행) ─────────────────────────────────────
INSERT INTO personal_survey
    (id, member_id, annual_income, monthly_available_amount, financial_asset_ratio,
     financial_knowledge, capital_preservation_attitude, created_at, updated_at)
WITH RECURSIVE seq AS (
    SELECT 1 AS n UNION ALL SELECT n + 1 FROM seq WHERE n < @couple_count
)
SELECT
    @survey_base + (n * 2 - 1),
    @member_base + (n * 2 - 1),
    30000000 + (n % 100) * 1000000,
    200000 + (n % 50) * 100000,
    ELT(1 + (n % 5), 'UNDER_10', 'UNDER_30', 'UNDER_50', 'UNDER_80', 'OVER_80'),
    ELT(1 + (n % 5), 'VERY_LOW', 'LOW', 'MEDIUM', 'HIGH', 'VERY_HIGH'),
    ELT(1 + (n % 6), 'ZERO', 'UNDER_10', 'UNDER_20', 'UNDER_50', 'UNDER_70', 'FULL'),
    NOW(), NOW()
FROM seq
UNION ALL
SELECT
    @survey_base + (n * 2),
    @member_base + (n * 2),
    28000000 + (n % 80) * 1200000,
    150000 + (n % 40) * 120000,
    ELT(1 + ((n + 2) % 5), 'UNDER_10', 'UNDER_30', 'UNDER_50', 'UNDER_80', 'OVER_80'),
    ELT(1 + ((n + 2) % 5), 'VERY_LOW', 'LOW', 'MEDIUM', 'HIGH', 'VERY_HIGH'),
    ELT(1 + ((n + 3) % 6), 'ZERO', 'UNDER_10', 'UNDER_20', 'UNDER_50', 'UNDER_70', 'FULL'),
    NOW(), NOW()
FROM seq;

-- ─── personal_survey_investment_experience (설문당 1행) ──────────────
INSERT INTO personal_survey_investment_experience
    (id, personal_survey_id, investment_experience, created_at, updated_at)
WITH RECURSIVE seq AS (
    SELECT 1 AS n UNION ALL SELECT n + 1 FROM seq WHERE n < @couple_count
)
SELECT
    @psie_base + (n * 2 - 1),
    @survey_base + (n * 2 - 1),
    ELT(1 + (n % 5), 'LOW_RISK', 'MODERATE_LOW_RISK', 'MODERATE_RISK', 'MODERATE_HIGH_RISK', 'HIGH_RISK'),
    NOW(), NOW()
FROM seq
UNION ALL
SELECT
    @psie_base + (n * 2),
    @survey_base + (n * 2),
    ELT(1 + ((n + 1) % 5), 'LOW_RISK', 'MODERATE_LOW_RISK', 'MODERATE_RISK', 'MODERATE_HIGH_RISK', 'HIGH_RISK'),
    NOW(), NOW()
FROM seq;

-- ─── common_survey (커플당 1행, 작성자=a) ─────────────────────────────
INSERT INTO common_survey
    (id, member_id, first_goal_type, second_goal_type, target_amount, target_period_months,
     loan_purpose, has_loan_within_one_month, created_at, updated_at)
WITH RECURSIVE seq AS (
    SELECT 1 AS n UNION ALL SELECT n + 1 FROM seq WHERE n < @couple_count
)
SELECT
    @common_survey_base + n,
    @member_base + (n * 2 - 1),
    ELT(1 + (n % 5), 'INVESTMENT', 'RETIREMENT', 'MARRIAGE', 'HOUSING', 'SHORT_TERM'),
    ELT(1 + ((n + 1) % 5), 'INVESTMENT', 'RETIREMENT', 'MARRIAGE', 'HOUSING', 'SHORT_TERM'),
    100000000 + (n % 400) * 1000000,
    12 + (n % 48),
    ELT(1 + (n % 5), 'NONE', 'JEONSE', 'HOUSING', 'CAR', 'BUSINESS'),
    n % 2,
    NOW(), NOW()
FROM seq;

-- ─── invitation_code (커플당 1행, 이미 사용됨) ────────────────────────
INSERT INTO invitation_code
    (id, common_survey_id, code_value, status, created_at, updated_at)
WITH RECURSIVE seq AS (
    SELECT 1 AS n UNION ALL SELECT n + 1 FROM seq WHERE n < @couple_count
)
SELECT
    @invitation_base + n,
    @common_survey_base + n,
    CONCAT('PERF', LPAD(n, 10, '0')),
    'USED',
    NOW(), NOW()
FROM seq;

-- ─── couple ───────────────────────────────────────────────────────────
INSERT INTO couple
    (id, inviter_id, invitee_id, invitation_code_id, investment_type, profile_message,
     created_at, updated_at)
WITH RECURSIVE seq AS (
    SELECT 1 AS n UNION ALL SELECT n + 1 FROM seq WHERE n < @couple_count
)
SELECT
    @couple_base + n,
    @member_base + (n * 2 - 1),
    @member_base + (n * 2),
    @invitation_base + n,
    ELT(1 + (n % 5), 'DIFF_0', 'DIFF_1', 'DIFF_2', 'DIFF_3', 'DIFF_4'),
    NULL,
    NOW(), NOW()
FROM seq;

-- ─── financial_summary (커플당 2행) ───────────────────────────────────
INSERT INTO financial_summary
    (member_id, financial_asset, total_debt, available_balance, annual_debt_payment,
     average_interest_rate, has_high_rate_debt, created_at, updated_at)
WITH RECURSIVE seq AS (
    SELECT 1 AS n UNION ALL SELECT n + 1 FROM seq WHERE n < @couple_count
)
SELECT
    @member_base + (n * 2 - 1),
    (n % 300) * 1000000,
    (n % 150) * 1000000,
    (n % 50) * 1000000,
    (n % 30) * 500000,
    IF(n % 4 = 0, NULL, 3.0 + (n % 15) * 0.3),
    n % 5 = 0,
    NOW(), NOW()
FROM seq
UNION ALL
SELECT
    @member_base + (n * 2),
    ((n + 10) % 300) * 1000000,
    ((n + 5) % 150) * 1000000,
    ((n + 3) % 50) * 1000000,
    ((n + 2) % 30) * 500000,
    IF(n % 3 = 0, NULL, 3.0 + ((n + 4) % 15) * 0.3),
    n % 7 = 0,
    NOW(), NOW()
FROM seq;

-- ─── pension_isa_account (커플당 2행, 전부 미보유 — 단순화) ──────────
INSERT INTO pension_isa_account
    (member_id, has_pension_saving, has_irp, has_dc, has_isa,
     pension_saving_balance, irp_balance, pension_annual_payment, irp_annual_payment,
     dc_annual_payment, isa_annual_deposit, tax_eligibility_status, isa_eligibility_status,
     created_at, updated_at)
WITH RECURSIVE seq AS (
    SELECT 1 AS n UNION ALL SELECT n + 1 FROM seq WHERE n < @couple_count
)
SELECT @member_base + (n * 2 - 1), 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 'ELIGIBLE', 'ELIGIBLE', NOW(), NOW() FROM seq
UNION ALL
SELECT @member_base + (n * 2),     0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 'ELIGIBLE', 'ELIGIBLE', NOW(), NOW() FROM seq;

-- ─── compatibility_result (0~만점 범위 CHECK 통과하도록 % 로 클램프) ──
INSERT INTO compatibility_result
    (id, couple_id, asset_stability_score, debt_repayment_score, financial_value_score,
     goal_feasibility_score, tax_strategy_score, tax_strategy_calculated, total_score,
     result_summary, created_at, updated_at)
WITH RECURSIVE seq AS (
    SELECT 1 AS n UNION ALL SELECT n + 1 FROM seq WHERE n < @couple_count
)
SELECT
    @compat_base + n,
    @couple_base + n,
    ROUND((n % 251) / 10, 2)  AS asset_score,   -- 0~25.0
    ROUND((n % 201) / 10, 2)  AS debt_score,    -- 0~20.0
    ROUND((n % 251) / 10, 2)  AS value_score,   -- 0~25.0
    ROUND((n % 201) / 10, 2)  AS goal_score,    -- 0~20.0
    ROUND((n % 101) / 10, 2)  AS tax_score,     -- 0~10.0
    1,
    ROUND((n % 251) / 10, 2) + ROUND((n % 201) / 10, 2) + ROUND((n % 251) / 10, 2)
        + ROUND((n % 201) / 10, 2) + ROUND((n % 101) / 10, 2),
    NULL,
    NOW(), NOW()
FROM seq;

-- ─── report ───────────────────────────────────────────────────────────
INSERT INTO report
    (id, compatibility_result_id, asset_stability_reason, debt_repayment_reason,
     financial_value_reason, goal_feasibility_reason, expected_asset, tax_strategy_reason,
     expert_comment, created_at, updated_at)
WITH RECURSIVE seq AS (
    SELECT 1 AS n UNION ALL SELECT n + 1 FROM seq WHERE n < @couple_count
)
SELECT
    @report_base + n,
    @compat_base + n,
    '[부하테스트] 금융 자산 관련 사유 텍스트예요.',
    '[부하테스트] 부채 상환 관련 사유 텍스트예요.',
    '[부하테스트] 투자 가치관 관련 사유 텍스트예요.',
    '[부하테스트] 목표 달성 가능성 관련 사유 텍스트예요.',
    100000000 + (n % 500) * 1000000,
    '[부하테스트] 절세 활용 관련 사유 텍스트예요.',
    '[부하테스트] 종합 코멘트예요. 실제 GPT 호출 없이 성능 테스트를 위해 채워둔 더미 문단입니다.\n\n두 번째 문단도 같은 이유로 채워둔 더미 텍스트예요.',
    NOW(), NOW()
FROM seq;

-- ─── 결과 확인 ────────────────────────────────────────────────────────
SELECT
    (SELECT COUNT(*) FROM member)              AS member_total,
    (SELECT COUNT(*) FROM couple)              AS couple_total,
    (SELECT COUNT(*) FROM report)              AS report_total,
    @couple_count                              AS couple_count_requested;
