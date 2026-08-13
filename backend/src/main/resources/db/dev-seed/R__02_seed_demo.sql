-- =====================================================================
-- 찰떡궁합 — seed-demo.sql (병렬 개발용 목데이터 · 운영 X)
-- 실행: 01-schema.sql → 02-seed-product.sql 다음.
-- 내용: 데모 커플 1쌍(A 안정추구형 · B 적극투자형) + 마이데이터 + 설문
--       + 궁합점수(73.15) + 리포트 + 추천(커플 슬롯 4종) + 개인추천(투자·절세)
-- 시나리오: 30대 초반 맞벌이, 전세(HOUSING) 목표. 성향 대비 커플.
--   · 총점 73.15 = 자산21.41 + 부채14.00 + 가치관16.95 + 목표12.79 + 절세8.00
--     (아래 마이데이터·설문 입력으로 계산식이 실제 산출하는 값 — 계산엔진 검증용)
--   · 커플 투자 슬롯 1등 = 안정추구형(min성향) 기준 → 저위험 상품(머니마켓)
--   · B 개인 투자추천 = 안정추구~적극투자 등급 상품이 함께 노출
-- ⚠️ investment_type 코드값은 schema 미정의 → 본 시드 가정(팀 확정 필요 · VARCHAR(20) 이내):
--    STABLE 안정형 / STABLE_SEEKING 안정추구형 / NEUTRAL 위험중립형
--    / AGGRESSIVE 적극투자형 / VERY_AGGRESSIVE 공격투자형
-- 비밀번호(둘 다) Test1234! = $2b$10$S7kxCvVErKro6iMGGYtqieadaYQh/mlP/3AOB6q8G81Dl2MoyJeBG
-- =====================================================================
-- 【 팀원별 사용법 】 이 파일 하나로 각자 파트를 독립 개발할 수 있다.
--   로그인: demo.a@chaltteok.dev(김하나·안정추구·초대자) / demo.b@chaltteok.dev(이두리·적극투자·피초대자)
--           둘 다 비번 Test1234! · couple id=1 · 초대코드 DEMO2026
--   · 임민지(AUTH)      : 데모 계정으로 로그인/JWT 인증 플로우 테스트 (member·member_agreement)
--   · 강현지(CPL·설문)  : 연동된 커플·설문 데이터로 조회/수정 화면. 신규연동 플로우는 이걸 정답 픽스처로
--   · 송찬우(추천)      : recommendation·slot·product로 패키지 화면 렌더 / 파이프라인 결과를 seed와 대조
--   · 진승환(리포트)    : compatibility_result·report로 리포트 화면(LLM 붙이기 전) 렌더
--   · 노유열·장지원     : 마이데이터+설문+age중앙값으로 5축 엔진 검증(골든값 총점=73.15) · 대시보드 카드
--   ※ 골든 픽스처: 입력(마이데이터·설문) → 출력(점수 73.15·추천상품)이 정합 → 회귀 테스트 기준값.
--   ※ 재실행 안전: 맨 앞 DELETE→INSERT라 몇 번 돌려도 데모 데이터가 초기화됨(꼬이면 이 파일만 재실행).
--   ※ 개발 전용 — 운영 배포에서는 제외할 것.
-- =====================================================================
SET NAMES utf8mb4;

SET FOREIGN_KEY_CHECKS=0;
DELETE FROM favorite_product;
DELETE FROM personal_recommendation_tax_saving;
DELETE FROM personal_recommendation_investment;
DELETE FROM recommendation_product;
DELETE FROM recommendation_slot;
DELETE FROM recommendation;
DELETE FROM report;
DELETE FROM compatibility_result;
DELETE FROM pension_isa_account;
DELETE FROM financial_summary;
DELETE FROM couple;
DELETE FROM invitation_code;
DELETE FROM common_survey;
DELETE FROM personal_survey_investment_experience;
DELETE FROM personal_survey;
DELETE FROM member_agreement;
DELETE FROM member;
SET FOREIGN_KEY_CHECKS=1;

-- ─── 회원 (A=초대자·안정추구형 32세·KB스타적금Ⅲ 가입 가능 / B=피초대자·적극투자형 33세·가입 불가) ───
INSERT INTO member
 (id,email,password,name,gender,birth_date,status,last_login_at,investment_type,kb_star_savings_eligible) VALUES
(1,'demo.a@chaltteok.dev','$2b$10$S7kxCvVErKro6iMGGYtqieadaYQh/mlP/3AOB6q8G81Dl2MoyJeBG','김하나','F','1994-03-15','ACTIVE','2026-07-22 09:30:00','STABLE_SEEKING',1),
(2,'demo.b@chaltteok.dev','$2b$10$S7kxCvVErKro6iMGGYtqieadaYQh/mlP/3AOB6q8G81Dl2MoyJeBG','이두리','M','1992-11-20','ACTIVE','2026-07-22 21:10:00','AGGRESSIVE',0);

-- ─── 약관 동의 ───
INSERT INTO member_agreement (member_id,agree_mydata_terms,agree_privacy,agree_asset_link,agree_couple_share,agree_marketing,agreed_at) VALUES
(1,1,1,1,1,1,'2026-07-15 10:00:00'),
(2,1,1,1,1,0,'2026-07-15 20:30:00');

-- ─── 개인 설문 (Q1 자산비중·Q3 이해도·Q4 원금보존 + 소득·월저축) ───
--   A(안정추구): Q1=UNDER_50(3)·Q3=MEDIUM(3)·Q4=UNDER_50(4)
--   B(적극투자): Q1=UNDER_80(4)·Q3=HIGH(4)·Q4=UNDER_10(2)
INSERT INTO personal_survey (id,member_id,annual_income,monthly_available_amount,financial_asset_ratio,financial_knowledge,capital_preservation_attitude) VALUES
(1,1,45000000,1000000,'UNDER_50','MEDIUM','UNDER_50'),
(2,2,75000000,1500000,'UNDER_80','HIGH','UNDER_10');

-- ─── 개인 설문 투자경험(Q2 다중응답 · 점수=최고위험 등급) ───
--   A 최고=MODERATE_RISK(3) / B 최고=MODERATE_HIGH_RISK(4)
INSERT INTO personal_survey_investment_experience (personal_survey_id,investment_experience) VALUES
(1,'LOW_RISK'),(1,'MODERATE_LOW_RISK'),(1,'MODERATE_RISK'),
(2,'LOW_RISK'),(2,'MODERATE_LOW_RISK'),(2,'MODERATE_RISK'),(2,'MODERATE_HIGH_RISK');

-- ─── 공동 설문 (초대자 A만 작성 · 전세 목표 3.5억/36개월) ───
--   has_loan_within_one_month=0 → 투자 슬롯 ON 유지(전세 대출은 1개월 밖 계획)
INSERT INTO common_survey (id,member_id,first_goal_type,second_goal_type,target_amount,target_period_months,loan_purpose,has_loan_within_one_month) VALUES
(1,1,'HOUSING','SHORT_TERM',350000000,36,'JEONSE',0);

-- ─── 초대 코드 → 커플 연동 ───
INSERT INTO invitation_code (id,common_survey_id,code_value,status) VALUES
(1,1,'DEMO2026','USED');

-- couple.investment_type = CoupleInvestmentTypeCalculator 기준(안정추구=1, 적극투자=3 → |1-3|=2 → DIFF_2)
INSERT INTO couple (id,inviter_id,invitee_id,invitation_code_id,investment_type) VALUES
(1,1,2,1,'DIFF_2');

-- ─── 마이데이터: 자산·부채 요약 ───
INSERT INTO financial_summary (member_id,financial_asset,total_debt,available_balance,annual_debt_payment,average_interest_rate,has_high_rate_debt) VALUES
(1, 90000000, 20000000, 4000000, 6000000, 4.50, 0),
(2,180000000, 40000000, 6000000, 10000000, 5.20, 0);

-- ─── 마이데이터: 연금·ISA 계좌 요약 ───
--   A: 연금저축·ISA 보유(IRP·DC 미보유=0) / B: 연금저축·IRP·ISA 보유
INSERT INTO pension_isa_account
 (member_id,has_pension_saving,has_irp,has_dc,has_isa,
  pension_saving_balance,irp_balance,pension_annual_payment,irp_annual_payment,dc_annual_payment,isa_annual_deposit,
  tax_eligibility_status,isa_eligibility_status) VALUES
(1, 1,0,0,1,  5000000,       0, 6000000,       0, 0, 10000000, 'ELIGIBLE','ELIGIBLE'),
(2, 1,1,0,1, 20000000,15000000, 6000000, 3000000, 0, 20000000, 'ELIGIBLE','ELIGIBLE');

-- ─── 궁합도 점수 (couple 1:1 · 위 입력으로 계산식 산출값) ───
-- 점수는 실제 MatchCalculator 산출값으로 맞춰져 있다(2026-08-13, overall_comment 실검증 시 확인).
INSERT INTO compatibility_result
 (id,couple_id,asset_stability_score,debt_repayment_score,financial_value_score,goal_feasibility_score,tax_strategy_score,total_score,result_summary) VALUES
(1,1, 15.52, 14.00, 16.95, 17.05, 8.00, 72.00,
 '두 분의 금융궁합은 72.00점입니다. 자산·목표는 견고하나 위험 선호 차이와 절세 여력에 보완 여지가 있습니다.');

-- ─── 리포트 (축별 자연어 근거 · LLM 산출 자리) ───
-- expert_comment는 위 점수 기준으로 실제 OverallCommentService(gpt-4o-mini)가 생성한 문장을 그대로 옮겼다.
INSERT INTO report
 (compatibility_result_id,asset_stability_reason,debt_repayment_reason,financial_value_reason,goal_feasibility_reason,tax_strategy_reason,expert_comment) VALUES
(1,
 '부부 합산 금융자산이 30대 중앙값을 웃돌아 자산 축이 안정적입니다.',
 'DSR·부채부담률이 모두 관리 범위 안에 있어 상환 여력이 양호합니다.',
 '안정추구형과 적극투자형으로 위험 선호 차이가 있어 공동자금 운용 규칙 합의가 필요합니다.',
 '3년 내 전세 목표 달성률이 약 85%로, 저축·투자 속도를 조금 높이면 도달 가능합니다.',
 '연금 세액공제 활용은 우수하나 ISA 기본 납입한도에 여력이 남아 있습니다.',
 '두 분은 **목표 달성 가능성이 높아요**. 3년 뒤 목표의 85%에 도달할 것으로 예상되며, 목표 금액이 3억 5000만원이므로 5167만원 부족해요. 절세 활용이 잘 되고 있어서, 하나님과 두리님 각각의 연금저축과 ISA 한도를 잘 채우고 있어요. 이 점에서 1000만원과 2000만원 한도가 남아 있다는 점도 플러스에요. 부채 상환도 탄탄해서, 하나님은 2000만원, 두리님은 4000만원의 부채를 갖고 있어요. 금융 자산도 안정적인데, 두 분의 합산 금융자산이 2억 7000만원으로 또래 중앙값 대비 145% 이상이에요.\n\n두 분의 **투자 가치관에는 보완이 필요해요**. 현재 두 분의 투자 가치관 일치도가 68%로 이 부분에서 조금 더 신경 쓰면 좋겠어요. 투자 가치관을 조율하고 맞춰가면, 앞으로 금융 관리에 더 큰 도움이 될 수 있을 것 같아요.');

-- ─── 추천 결과 (커플 슬롯 4종: DEPOSIT·SAVINGS·INVESTMENT·LOAN / 절세는 개인추천) ───
INSERT INTO recommendation (id,couple_id) VALUES (1,1);

INSERT INTO recommendation_slot (id,recommendation_id,slot_type) VALUES
(1,1,'DEPOSIT'),
(2,1,'SAVINGS'),
(3,1,'INVESTMENT'),
(4,1,'LOAN');

-- 슬롯별 상품(rank_no=순위 · is_selected=1은 슬롯 대표=1등)
INSERT INTO recommendation_product (recommendation_slot_id,product_id,rank_no,is_selected) VALUES
-- 예금: 금리 높은 e-plus 정기예금 1등
(1, 2, 1, 1),(1, 1, 2, 0),
-- 적금: 목표기간(36개월) 근접 정기적금 1등
(2, 6, 1, 1),(2, 7, 2, 0),
-- 투자: min성향(안정추구형) 기준 → 저위험·순자산최고 머니마켓(15) 1등
(3,15, 1, 1),(3,16, 2, 0),
-- 대출: 전세·신혼부부 특화(32) 1등, 일반 전세(29) 대안
(4,32, 1, 1),(4,29, 2, 0);

-- ─── 개인 추천: 투자 (성향별 · B는 안정추구~적극투자 스펙트럼) ───
--   A(안정추구): 저위험 위주(등급5·6)
INSERT INTO personal_recommendation_investment (member_id,product_id,rank_no) VALUES
(1,15,1),(1,16,2),(1,13,3),
--   B(적극투자): 등급5(안정추구)~등급2(적극투자)까지 폭넓게
(2,15,1),(2,16,2),(2,25,3),(2,22,4);

-- ─── 개인 추천: 절세 (노후 목적 없음 → ISA만 · 이해도로 중개형) ───
INSERT INTO personal_recommendation_tax_saving (member_id,product_id,rank_no) VALUES
(1,8,1),
(2,8,1);

-- ─── 즐겨찾기 (선택) ───
INSERT INTO favorite_product (member_id,product_id) VALUES
(1,15),
(2,25);
