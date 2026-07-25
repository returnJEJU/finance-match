-- =====================================================================
-- 찰떡궁합 (Team return JEJU;) — schema.sql
-- 기준: Finance_match_ERD (1차 완료본) · MySQL 8.4 · utf8mb4 · Asia/Seoul
-- 포함 24 테이블 (ERD 21 + 마이데이터 2 + 참조 1: age_group_asset_median)
--          (1차 확정본 · 서브타입 부족 컬럼은 마이데이터에서 보완 가능)
-- 컨벤션: snake_case·단수 / id BIGINT PK / 금액 DECIMAL / created·updated_at
--         soft delete=member만 / FK fk_자식_부모 / UNIQUE uk_ · INDEX idx_
-- 결정: couple=하드딜리트(+참조 체인 ON DELETE CASCADE, CHECK 자기커플 방지)
--       couple.investment_type=리포트 매트릭스(min은 파생) / 점수·추천=couple당 1행(최신만)
--       deposit=예금+적금 통합(type 구분) / 개인추천=절세·투자 분리
--       [마이데이터] financial_summary(자산·부채)·pension_isa_account(연금·ISA)=member 1:1 요약 스냅샷
--       financial_asset=연금/IRP 잔액 포함 총액 / *_balance=목표달성 일반목표 차감 / 월납입=연납입÷12 파생
--       [참조] age_group_asset_median=금융자산(30) 축 M_A·M_B·M_T · KOSIS 가계금융복지조사 금융자산 중앙값(값은 data.sql)
-- =====================================================================

SET NAMES utf8mb4;
SET time_zone = '+09:00';

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS age_group_asset_median;
DROP TABLE IF EXISTS pension_isa_account;
DROP TABLE IF EXISTS financial_summary;
DROP TABLE IF EXISTS deposit_rate;
DROP TABLE IF EXISTS deposit;
DROP TABLE IF EXISTS loan;
DROP TABLE IF EXISTS tax_saving;
DROP TABLE IF EXISTS investment;
DROP TABLE IF EXISTS recommendation_product;
DROP TABLE IF EXISTS recommendation_slot;
DROP TABLE IF EXISTS recommendation;
DROP TABLE IF EXISTS report;
DROP TABLE IF EXISTS compatibility_result;
DROP TABLE IF EXISTS personal_recommendation_investment;
DROP TABLE IF EXISTS personal_recommendation_tax_saving;
DROP TABLE IF EXISTS favorite_product;
DROP TABLE IF EXISTS couple;
DROP TABLE IF EXISTS invitation_code;
DROP TABLE IF EXISTS common_survey;
DROP TABLE IF EXISTS personal_survey_investment_experience;
DROP TABLE IF EXISTS personal_survey;
DROP TABLE IF EXISTS member_agreement;
DROP TABLE IF EXISTS product;
DROP TABLE IF EXISTS member;
SET FOREIGN_KEY_CHECKS = 1;

-- ─── member : 회원 계정·기본정보 (soft delete) ───────────────────────
CREATE TABLE member (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    email           VARCHAR(255) NOT NULL,
    password        VARCHAR(255) NOT NULL                       COMMENT 'BCrypt 해시',
    name            VARCHAR(50)  NOT NULL,
    gender          VARCHAR(10)  NOT NULL                       COMMENT 'M/F',
    birth_date      DATE         NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'      COMMENT 'ACTIVE/DORMANT/WITHDRAWN',
    last_login_at   DATETIME     NULL                           COMMENT 'NULL이면 첫 로그인',
    investment_type VARCHAR(20)  NULL                           COMMENT '개인 투자성향',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at      DATETIME     NULL                           COMMENT 'soft delete(탈퇴)',
    PRIMARY KEY (id),
    UNIQUE KEY uk_member_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='회원';

-- ─── product : 상품(메뉴판) · 서브타입은 id 식별자 관계 ──────────────
CREATE TABLE product (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    product_type VARCHAR(20)  NOT NULL                          COMMENT 'DEPOSIT, TAX_SAVING, INVESTMENT, LOAN',
    product_name VARCHAR(100) NOT NULL,
    description  TEXT         NULL,
    url          TEXT         NOT NULL                          COMMENT 'KB 공식 안내 URL',
    company_name VARCHAR(100) NOT NULL                          COMMENT '금융사명',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_product_type (product_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='상품';

-- ─── member_agreement : 약관 동의 (member 1:1) ──────────────────────
CREATE TABLE member_agreement (
    id                 BIGINT     NOT NULL AUTO_INCREMENT,
    member_id          BIGINT     NOT NULL,
    agree_mydata_terms TINYINT(1) NOT NULL,
    agree_privacy      TINYINT(1) NOT NULL,
    agree_asset_link   TINYINT(1) NOT NULL,
    agree_couple_share TINYINT(1) NOT NULL,
    agree_marketing    TINYINT(1) NOT NULL DEFAULT 0,
    agreed_at          DATETIME   NOT NULL,
    created_at         DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_agreement_member (member_id),
    CONSTRAINT fk_agreement_member FOREIGN KEY (member_id) REFERENCES member(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='약관 동의';

-- ─── personal_survey : 개인 설문 (member 1:1) ───────────────────────
CREATE TABLE personal_survey (
    id                            BIGINT        NOT NULL AUTO_INCREMENT,
    member_id                     BIGINT        NOT NULL,
    annual_income                 DECIMAL(15,0) NOT NULL        COMMENT '연소득(원)',
    monthly_available_amount      DECIMAL(15,0) NOT NULL        COMMENT '월 저축/투자 가능액(원)',
    financial_asset_ratio         VARCHAR(20)   NOT NULL		COMMENT 'UNDER_10, UNDER_30, UNDER_50, UNDER_80, OVER_80',
    financial_knowledge           VARCHAR(20)   NOT NULL		COMMENT 'VERY_LOW, LOW, MEDIUM, HIGH, VERY_HIGH',
    capital_preservation_attitude VARCHAR(20)   NOT NULL		COMMENT 'ZERO, UNDER_10, UNDER_20, UNDER_50, UNDER_70, FULL',
    created_at                    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_personal_survey_member (member_id),
    CONSTRAINT fk_personal_survey_member FOREIGN KEY (member_id) REFERENCES member(id),
    CONSTRAINT chk_ps_income  CHECK (annual_income >= 0),
    CONSTRAINT chk_ps_monthly CHECK (monthly_available_amount >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='개인 설문';

-- ─── personal_survey_investment_experience : 투자경험 다중응답 (1:N) ─
CREATE TABLE personal_survey_investment_experience (
    id                    BIGINT      NOT NULL AUTO_INCREMENT,
    personal_survey_id    BIGINT      NOT NULL,
    investment_experience VARCHAR(20) NOT NULL			COMMENT 'LOW_RISK, MODERATE_LOW_RISK, MODERATE_RISK, MODERATE_HIGH_RISK, HIGH_RISK',
    created_at            DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_survey_experience (personal_survey_id, investment_experience),
    CONSTRAINT fk_experience_survey FOREIGN KEY (personal_survey_id) REFERENCES personal_survey(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='개인 설문 투자경험';

-- ─── common_survey : 공동 설문 (작성자 member 1:1) · 1·2순위 목표 ────
CREATE TABLE common_survey (
    id                        BIGINT        NOT NULL AUTO_INCREMENT,
    member_id                 BIGINT        NOT NULL            COMMENT '공동설문 작성자',
    first_goal_type           VARCHAR(20)   NOT NULL            COMMENT '1순위 목표 유형 (INVESTMENT, RETIREMENT, MARRIAGE, HOUSING, SHORT_TERM)',
    second_goal_type          VARCHAR(20)   NOT NULL            COMMENT '2순위 목표 유형 (INVESTMENT, RETIREMENT, MARRIAGE, HOUSING, SHORT_TERM)',
    target_amount             DECIMAL(15,0) NOT NULL            COMMENT '1순위 목표 금액(원)',
    target_period_months      INT           NOT NULL            COMMENT '1순위 목표 기간(개월)',
    loan_purpose              VARCHAR(20)   NOT NULL            COMMENT '예상 대출 목적 코드 (NONE, JEONSE, HOUSING, CAR, BUSINESS)',
    has_loan_within_one_month TINYINT(1)    NOT NULL,
    created_at                DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_common_survey_member (member_id),
    CONSTRAINT fk_common_survey_member FOREIGN KEY (member_id) REFERENCES member(id),
    CONSTRAINT chk_cs_amount CHECK (target_amount > 0),
    CONSTRAINT chk_cs_period CHECK (target_period_months > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='공동 설문';

-- ─── invitation_code : 초대 코드 (common_survey 1:1) ────────────────
CREATE TABLE invitation_code (
    id               BIGINT      NOT NULL AUTO_INCREMENT,
    common_survey_id BIGINT      NOT NULL,
    code_value       VARCHAR(20) NOT NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'      COMMENT 'ACTIVE/USED/CANCELLED',
    created_at       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_invitation_common_survey (common_survey_id),
    UNIQUE KEY uk_invitation_code_value (code_value),
    CONSTRAINT fk_invitation_common_survey FOREIGN KEY (common_survey_id) REFERENCES common_survey(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='초대 코드';

-- ─── couple : 커플 (하드딜리트 · 자기커플 방지 CHECK) ────────────────
CREATE TABLE couple (
    id                 BIGINT      NOT NULL AUTO_INCREMENT,
    inviter_id         BIGINT      NOT NULL                     COMMENT '초대한 회원',
    invitee_id         BIGINT      NOT NULL                     COMMENT '초대받은 회원',
    invitation_code_id BIGINT      NOT NULL,
    investment_type    VARCHAR(20) NULL                         COMMENT '부부 성향(리포트 매트릭스)',
    created_at         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_couple_inviter (inviter_id),
    UNIQUE KEY uk_couple_invitee (invitee_id),
    UNIQUE KEY uk_couple_invitation_code (invitation_code_id),
    CONSTRAINT fk_couple_inviter         FOREIGN KEY (inviter_id)         REFERENCES member(id),
    CONSTRAINT fk_couple_invitee         FOREIGN KEY (invitee_id)         REFERENCES member(id),
    CONSTRAINT fk_couple_invitation_code FOREIGN KEY (invitation_code_id) REFERENCES invitation_code(id),
    CONSTRAINT chk_couple_diff_members   CHECK (inviter_id <> invitee_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='커플';

-- ─── favorite_product : 즐겨찾기 (중복방지 UNIQUE) ──────────────────
CREATE TABLE favorite_product (
    id         BIGINT   NOT NULL AUTO_INCREMENT,
    member_id  BIGINT   NOT NULL,
    product_id BIGINT   NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_favorite_member_product (member_id, product_id),
    KEY idx_favorite_member (member_id),
    CONSTRAINT fk_favorite_member  FOREIGN KEY (member_id)  REFERENCES member(id),
    CONSTRAINT fk_favorite_product FOREIGN KEY (product_id) REFERENCES product(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='즐겨찾기 상품';

-- ─── personal_recommendation_tax_saving : 개인 추천 절세계좌 (1:N) ───
CREATE TABLE personal_recommendation_tax_saving (
    id         BIGINT   NOT NULL AUTO_INCREMENT,
    member_id  BIGINT   NOT NULL,
    product_id BIGINT   NOT NULL,
    rank_no    INT      NOT NULL                                COMMENT '추천 순위',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_prts_member_product (member_id, product_id),
    UNIQUE KEY uk_prts_member_rank (member_id, rank_no),
    CONSTRAINT fk_prts_member  FOREIGN KEY (member_id)  REFERENCES member(id),
    CONSTRAINT fk_prts_product FOREIGN KEY (product_id) REFERENCES product(id),
    CONSTRAINT chk_prts_rank CHECK (rank_no > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='개인 추천 절세계좌';

-- ─── personal_recommendation_investment : 개인 추천 투자상품 (1:N) ───
CREATE TABLE personal_recommendation_investment (
    id         BIGINT   NOT NULL AUTO_INCREMENT,
    member_id  BIGINT   NOT NULL,
    product_id BIGINT   NOT NULL,
    rank_no    INT      NOT NULL                                COMMENT '추천 순위',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_prin_member_product (member_id, product_id),
    UNIQUE KEY uk_prin_member_rank (member_id, rank_no),
    CONSTRAINT fk_prin_member  FOREIGN KEY (member_id)  REFERENCES member(id),
    CONSTRAINT fk_prin_product FOREIGN KEY (product_id) REFERENCES product(id),
    CONSTRAINT chk_prin_rank CHECK (rank_no > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='개인 추천 투자상품';

-- ─── compatibility_result : 궁합도 점수 (couple 1:1 · 최신만) ────────
--     couple 하드삭제 시 함께 삭제 (ON DELETE CASCADE)
CREATE TABLE compatibility_result (
    id                     BIGINT       NOT NULL AUTO_INCREMENT,
    couple_id              BIGINT       NOT NULL,
    asset_stability_score  DECIMAL(5,2) NOT NULL DEFAULT 0      COMMENT '자산 30',
    debt_repayment_score   DECIMAL(5,2) NOT NULL DEFAULT 0      COMMENT '부채 20',
    financial_value_score  DECIMAL(5,2) NOT NULL DEFAULT 0      COMMENT '가치관 25',
    goal_feasibility_score DECIMAL(5,2) NOT NULL DEFAULT 0      COMMENT '목표 15',
    tax_strategy_score     DECIMAL(5,2) NOT NULL DEFAULT 0      COMMENT '절세 10',
    total_score            DECIMAL(5,2) NOT NULL DEFAULT 0      COMMENT '총점 100',
    result_summary         VARCHAR(500) NULL,
    created_at             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_compat_couple (couple_id),
    CONSTRAINT fk_compat_couple FOREIGN KEY (couple_id) REFERENCES couple(id) ON DELETE CASCADE,
    CONSTRAINT chk_compat_asset CHECK (asset_stability_score  BETWEEN 0 AND 30),
    CONSTRAINT chk_compat_debt  CHECK (debt_repayment_score   BETWEEN 0 AND 20),
    CONSTRAINT chk_compat_value CHECK (financial_value_score  BETWEEN 0 AND 25),
    CONSTRAINT chk_compat_goal  CHECK (goal_feasibility_score BETWEEN 0 AND 15),
    CONSTRAINT chk_compat_tax   CHECK (tax_strategy_score     BETWEEN 0 AND 10),
    CONSTRAINT chk_compat_total CHECK (total_score            BETWEEN 0 AND 100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='금융 궁합도 점수';

-- ─── report : 리포트 (compatibility_result 1:1) ────────────────────
CREATE TABLE report (
    id                      BIGINT       NOT NULL AUTO_INCREMENT,
    compatibility_result_id BIGINT       NOT NULL,
    asset_stability_reason  VARCHAR(500) NULL,
    debt_repayment_reason   VARCHAR(500) NULL,
    financial_value_reason  VARCHAR(500) NULL,
    goal_feasibility_reason VARCHAR(500) NULL,
    tax_strategy_reason     VARCHAR(500) NULL,
    expert_comment          TEXT         NULL,
    created_at              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_report_compat (compatibility_result_id),
    CONSTRAINT fk_report_compat FOREIGN KEY (compatibility_result_id) REFERENCES compatibility_result(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='리포트';

-- ─── recommendation : 추천 결과 (couple 1:1 · 최신만) ───────────────
CREATE TABLE recommendation (
    id         BIGINT   NOT NULL AUTO_INCREMENT,
    couple_id  BIGINT   NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_recommendation_couple (couple_id),
    CONSTRAINT fk_recommendation_couple FOREIGN KEY (couple_id) REFERENCES couple(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='추천 결과';

-- ─── recommendation_slot : 추천 상품군 슬롯 (recommendation 1:N) ─────
CREATE TABLE recommendation_slot (
    id                BIGINT      NOT NULL AUTO_INCREMENT,
    recommendation_id BIGINT      NOT NULL,
    slot_type         VARCHAR(20) NOT NULL			COMMENT 'DEPOSIT, SAVINGS, INVESTMENT, LOAN',
    created_at        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_slot_recommendation_type (recommendation_id, slot_type),
    CONSTRAINT fk_slot_recommendation FOREIGN KEY (recommendation_id) REFERENCES recommendation(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='추천 상품군 슬롯';

-- ─── recommendation_product : 슬롯 내 추천 상품 (slot 1:N) ──────────
CREATE TABLE recommendation_product (
    id                     BIGINT     NOT NULL AUTO_INCREMENT,
    recommendation_slot_id BIGINT     NOT NULL,
    product_id             BIGINT     NOT NULL,
    rank_no                INT        NOT NULL                  COMMENT '슬롯 내 추천 순위',
    is_selected            TINYINT(1) NOT NULL DEFAULT 0        COMMENT '슬롯 대표 상품 여부',
    created_at             DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_rp_slot_product (recommendation_slot_id, product_id),
    UNIQUE KEY uk_rp_slot_rank (recommendation_slot_id, rank_no),
    CONSTRAINT fk_rp_slot    FOREIGN KEY (recommendation_slot_id) REFERENCES recommendation_slot(id) ON DELETE CASCADE,
    CONSTRAINT fk_rp_product FOREIGN KEY (product_id)             REFERENCES product(id),
    CONSTRAINT chk_rp_rank CHECK (rank_no > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='슬롯 내 추천 상품';

-- ─── deposit : 예적금(예금+적금 통합) · product 식별자 관계(id=product.id) ─
CREATE TABLE deposit (
    id         BIGINT       NOT NULL                            COMMENT '= product.id',
    type       VARCHAR(20)  NOT NULL                            COMMENT 'DEPOSIT, SAVINGS',
    min_term   INT          NOT NULL                            COMMENT '최소 가입기간(개월)',
    max_term   INT          NOT NULL                            COMMENT '최대 가입기간(개월)',
    saving_min INT          NULL                                COMMENT '1회 최소 납입금액',
    saving_max INT          NULL                                COMMENT '1회 최대 납입금액',
    min_rate   DECIMAL(5,2) NOT NULL                            COMMENT '최소 금리(%)',
    max_rate   DECIMAL(5,2) NOT NULL                            COMMENT '최대 금리(%)',
    PRIMARY KEY (id),
    CONSTRAINT fk_deposit_product FOREIGN KEY (id) REFERENCES product(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='예적금(상품 서브타입)';

-- ─── deposit_rate : 예적금 가입기간 구간별 기본금리 (deposit 1:N) ────
CREATE TABLE deposit_rate (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    deposit_id BIGINT       NOT NULL,
    min_term   INT          NOT NULL                            COMMENT '구간 최소 기간(개월)',
    max_term   INT          NULL                                COMMENT '구간 최대 기간(개월)',
    base_rate  DECIMAL(5,2) NOT NULL                            COMMENT '연 기본금리(%)',
    PRIMARY KEY (id),
    UNIQUE KEY uk_deposit_rate_term (deposit_id, min_term),
    CONSTRAINT fk_deposit_rate_deposit FOREIGN KEY (deposit_id) REFERENCES deposit(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='예적금 가입기간 구간별 금리';

-- ─── loan : 대출 · product 식별자 관계(id=product.id) ───────────────
CREATE TABLE loan (
    id                  BIGINT       NOT NULL                   COMMENT '= product.id',
    loan_purpose        VARCHAR(20)  NOT NULL                   COMMENT '대출 목적 코드 (JEONSE, HOUSING, CAR, BUSINESS)',
    target_group        VARCHAR(20)  NOT NULL                   COMMENT '대출 목적 대상 (NEWLYWED, GENERAL, OTHER)',
    min_age             INT          NULL                       COMMENT '가입 가능 최소 나이',
    max_age             INT          NULL                       COMMENT '가입 가능 최대 나이',
    income_basis        VARCHAR(20)  NULL                       COMMENT '소득 조건 판단 기준 (INDIVIDUAL, COUPLE)',
    max_income          BIGINT       NULL                       COMMENT '연소득 상한액',
    application_channel VARCHAR(20)  NOT NULL                   COMMENT '신청 방법 (MOBILE, ONLINE, BRANCH)',
    max_rate            DECIMAL(5,2) NOT NULL                   COMMENT '공시 최고금리(%)',
    PRIMARY KEY (id),
    CONSTRAINT fk_loan_product FOREIGN KEY (id) REFERENCES product(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='대출(상품 서브타입)';

-- ─── tax_saving : 절세계좌 · product 식별자 관계(id=product.id) ──────
--     (1차 확정 · 운용방식·최소보유·계좌수수료·세제한도 등은 추후/마이데이터 보완)
CREATE TABLE tax_saving (
    id           BIGINT      NOT NULL                           COMMENT '= product.id',
    account_type VARCHAR(20) NOT NULL                           COMMENT '계좌 유형 (ISA, PENSION_SAVINGS, IRP)',
    isa_type     VARCHAR(20) NULL                               COMMENT 'ISA 유형  (BROKERAGE, DISCRETIONARY)',
    PRIMARY KEY (id),
    CONSTRAINT fk_tax_saving_product FOREIGN KEY (id) REFERENCES product(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='절세계좌(상품 서브타입)';

-- ─── investment : 투자상품 · product 식별자 관계(id=product.id) ──────
--     (1차 확정 · 총보수·납입방식·주식비중 등은 추후/마이데이터 보완)
CREATE TABLE investment (
    id         BIGINT        NOT NULL                           COMMENT '= product.id',
    risk_level INT           NOT NULL                           COMMENT '위험등급 (1~6)',
    aum        INT 			 NOT NULL                           COMMENT '순자산(AUM, 억 단위)',
    is_tdf     TINYINT(1)    NOT NULL                           COMMENT 'TDF 여부',
    PRIMARY KEY (id),
    CONSTRAINT fk_investment_product FOREIGN KEY (id) REFERENCES product(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='투자상품(상품 서브타입)';

-- =====================================================================
-- 마이데이터 계산 입력 (점수 5축 원천 데이터) · 담당: 노유열
--   실연동 전 표준 규격 목데이터로 적재 · member 1:1 요약 스냅샷
--   FK→member: member는 soft delete(하드삭제 없음)라 CASCADE 미적용
-- =====================================================================

-- ─── financial_summary : 마이데이터 자산·부채 요약 (member 1:1) ───────
--   financial_asset·total_debt·annual_debt_payment → 금융자산(30)·부채(20) 축 입력
--   financial_asset = 연금저축·IRP 잔액 포함 총 금융자산(마이데이터 계좌 잔액 합산 파생값)
--   annual_debt_payment = '향후 12개월' 예상 원리금(과거 납부액 아님 · 계산식 정의)
--   average_interest_rate·has_high_rate_debt = 점수 미사용, 추천 대환 슬롯 판단용
CREATE TABLE financial_summary (
    id                    BIGINT        NOT NULL AUTO_INCREMENT,
    member_id             BIGINT        NOT NULL,
    financial_asset       DECIMAL(18,0) NOT NULL DEFAULT 0        COMMENT '총 금융자산(연금·IRP 잔액 포함)',
    total_debt            DECIMAL(18,0) NOT NULL DEFAULT 0        COMMENT '총 부채',
    available_balance     DECIMAL(18,0) NOT NULL DEFAULT 0       COMMENT '입출금·파킹통장 잔액 합계(예금 추천 슬롯 판단용)',
    annual_debt_payment   DECIMAL(18,0) NOT NULL DEFAULT 0        COMMENT '향후 12개월 예상 원리금 총 상환액(과거 아님)',
    average_interest_rate DECIMAL(5,2)  NULL                      COMMENT '전체 대출 가중평균 금리(추천 대환 슬롯용·점수 미사용)',
    has_high_rate_debt    TINYINT(1)    NOT NULL DEFAULT 0        COMMENT '고금리 부채 보유 여부(추천 대환 슬롯용·점수 미사용)',
    created_at            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_financial_summary_member (member_id),
    CONSTRAINT fk_financial_summary_member FOREIGN KEY (member_id) REFERENCES member(id),
    CONSTRAINT chk_fs_asset   CHECK (financial_asset     >= 0),
    CONSTRAINT chk_fs_debt    CHECK (total_debt          >= 0),
    CONSTRAINT chk_fs_balance CHECK (available_balance >= 0),
    CONSTRAINT chk_fs_payment CHECK (annual_debt_payment >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='마이데이터 자산·부채 요약';

-- ─── pension_isa_account : 마이데이터 연금·ISA 계좌 요약 (member 1:1) ─
--   *_annual_payment/deposit → 절세(10) 축 / *_balance → 목표달성(15) 일반목표 가용자산 차감
--   월납입액 컬럼 없음: '연납입액 ÷ 12'로 파생(마이데이터 표준에 월납입 필드 없음)
--   납입액·잔액 값 규칙: 미보유=0 · 조회실패=NULL(계산 전 오류 처리)
--   *_eligibility_status: UNKNOWN은 0으로 넣지 않고 오류 처리(계산식 기준) · 기본값 UNKNOWN
CREATE TABLE pension_isa_account (
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    member_id              BIGINT        NOT NULL,
    has_pension_saving     TINYINT(1)    NOT NULL DEFAULT 0        COMMENT '연금저축 보유 여부',
    has_irp                TINYINT(1)    NOT NULL DEFAULT 0        COMMENT 'IRP 보유 여부',
    has_dc                 TINYINT(1)    NOT NULL DEFAULT 0        COMMENT 'DC형 보유 여부',
    has_isa                TINYINT(1)    NOT NULL DEFAULT 0        COMMENT 'ISA 보유 여부',
    pension_saving_balance DECIMAL(18,0) NULL                     COMMENT '연금저축 잔액(평가금액)·목표달성 일반목표 차감용·미보유0/조회실패NULL',
    irp_balance            DECIMAL(18,0) NULL                     COMMENT 'IRP 잔액(평가금액)·목표달성 일반목표 차감용·미보유0/조회실패NULL',
    pension_annual_payment DECIMAL(18,0) NULL                     COMMENT '해당 연도 연금저축 납입액(절세)',
    irp_annual_payment     DECIMAL(18,0) NULL                     COMMENT '해당 연도 IRP 개인 납입액(절세)',
    dc_annual_payment      DECIMAL(18,0) NULL                     COMMENT '해당 연도 DC 개인 추가 납입액(절세·선택)',
    isa_annual_deposit     DECIMAL(18,0) NULL                     COMMENT '해당 연도 ISA 입금액(절세)',
    tax_eligibility_status VARCHAR(20)   NOT NULL DEFAULT 'UNKNOWN' COMMENT '연금 세액공제 평가대상 (ELIGIBLE/INELIGIBLE/UNKNOWN)',
    isa_eligibility_status VARCHAR(20)   NOT NULL DEFAULT 'UNKNOWN' COMMENT 'ISA 평가대상 (ELIGIBLE/INELIGIBLE/UNKNOWN)',
    created_at             DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_pension_isa_member (member_id),
    CONSTRAINT fk_pension_isa_member FOREIGN KEY (member_id) REFERENCES member(id),
    CONSTRAINT chk_pia_pension_bal CHECK (pension_saving_balance >= 0),
    CONSTRAINT chk_pia_irp_bal     CHECK (irp_balance            >= 0),
    CONSTRAINT chk_pia_pension_pay CHECK (pension_annual_payment >= 0),
    CONSTRAINT chk_pia_irp_pay     CHECK (irp_annual_payment     >= 0),
    CONSTRAINT chk_pia_dc_pay      CHECK (dc_annual_payment      >= 0),
    CONSTRAINT chk_pia_isa_dep     CHECK (isa_annual_deposit     >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='마이데이터 연금·ISA 계좌 요약';

-- ─── age_group_asset_median : 연령대별 금융자산 중앙값 (KOSIS 참조 · 값은 data.sql) ─
--   금융자산(30) 축 상대평가 분모 M_A·M_B·M_T 조회용
--   출처: 가계금융복지조사(KOSIS DT_1HDAAD01) 금융자산 '보유가구 중앙값'(보유율 ≈100%)
--   조회: age_min <= 만나이 AND (age_max IS NULL OR 만나이 <= age_max) · base_year 최신
--   주의: 가구원수×연령 교차표 부재 → 연령 기준(전 가구원수 혼합) 중앙값만 가능
CREATE TABLE age_group_asset_median (
    id                     BIGINT        NOT NULL AUTO_INCREMENT,
    age_min                INT           NOT NULL              COMMENT '연령대 하한(만 나이, 포함)',
    age_max                INT           NULL                  COMMENT '연령대 상한(만 나이, 포함) · NULL=이상',
    median_financial_asset DECIMAL(18,0) NOT NULL              COMMENT '금융자산 중앙값(원)',
    base_year              INT           NOT NULL              COMMENT '통계 기준연도(예: 2025)',
    created_at             DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_age_median_bracket (base_year, age_min),
    CONSTRAINT chk_agm_median CHECK (median_financial_asset >= 0),
    CONSTRAINT chk_agm_range  CHECK (age_max IS NULL OR age_max >= age_min)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='연령대별 금융자산 중앙값(KOSIS)';
