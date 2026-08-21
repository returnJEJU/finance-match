-- =====================================================================
-- 찰떡궁합 — seed-demo-couples.sql (데모 커플 4쌍 완성본 · 운영 X)
-- 실행: R__01_seed_product → R__02_seed_demo 다음.
-- 내용: 회원 6명(3~8번) + 회원 2명(401403·401404번) + 마이데이터 + 설문 + 커플연결 + 궁합점수 + 리포트 + 추천
-- =====================================================================
-- 【 이 값들은 손으로 적은 것이 아니다 】
--   앱에서 실제로 가입·연동·설문을 마친 뒤, 계산기와 추천정책이 만들어 낸 결과를 그대로 덤프한 것이다.
--   따라서 입력(마이데이터·설문)과 출력(점수·리포트·추천)이 정합하며, 회귀 테스트 기준값으로 쓸 수 있다.
--   계산식이나 추천정책을 바꿔서 이 값이 달라졌다면 둘 중 하나다 — 의도한 변경이거나, 버그다.
--
--   다시 만드는 방법은 같은 폴더의 README.md 참고. 앱에서 다시 입력한 뒤 덤프하면 된다.
-- =====================================================================
-- 【 인물 】 비밀번호는 모두 Test1234! · 나이는 2026-08-12 기준
--   3 김보람   demo.saver@     33세 여 · 자산 1.2억 · 전세대출 6,000만 · 든든지킴형
--   4 박준서   demo.investor@  34세 남 · 자산 1.23억 · 신용대출 2,000만 · 적극성장형
--   5 최유진   demo.newlywed@  31세 여 · 자산 350만 · 전세대출 1.2억 · 균형설계형
--   6 정민호   demo.renter@    32세 남 · 자산 240만 · 대출 2건 5,700만 · 균형설계형
--   7 한서연   demo.rich@      38세 여 · 자산 3억 · 무부채 · 든든지킴형
--   8 오태윤   demo.debt@      30세 남 · 자산 250만 · 카드론 17.9% 고금리 · 과감도전형
--   401403 박정훈   demo.video.retire1@  46세 남 · 자산 4,000만 · 대출 1,000만 · 적극성장형
--   401404 최은영   demo.video.retire2@  43세 여 · 자산 4,000만 · 대출 1,000만 · 균형설계형
--
-- 【 커플 】
--   커플2 = 3 + 4         둘 다 안정 · 성향 반대(DIFF_3)  → 추천 4종 전부 + 개인투자(박준서)
--   커플4 = 5 + 6         둘 다 불안정 · 성향 일치(DIFF_0) → 예금·투자·개인투자 꺼짐
--   커플3 = 7 + 8         극과 극(DIFF_4)                 → 대출 꺼짐(고금리) · 개인투자 TDF(오태윤)
--   커플5 = 401403+401404 노후 목표 · 성향 닮음(DIFF_1)    → 예금·적금·투자(TDF) · 대출 없음(loan_purpose NONE) · 개인투자(박정훈만)
--
--   커플 조합은 고정이 아니다. 마이페이지에서 커플을 끊고 다른 사람과 이어도 되며,
--   자산은 이메일에 묶여 있어 조합을 바꿔도 그 사람을 따라간다(asset/README.md 참고).
-- =====================================================================
-- ⚠️ 재실행하면 3~8번·401403~401404번 회원의 모든 데이터가 지워지고 이 파일 내용으로 되돌아간다.
--    앱에서 새로 만들어 둔 내용이 있으면 날아간다(리셋 용도로 일부러 쓰기도 한다).
--    기존 데모 커플(1 김하나 · 2 이두리)은 건드리지 않는다.
-- =====================================================================
SET NAMES utf8mb4;
-- ─── 초기화 (3~8번 회원에 딸린 것만. 자식 → 부모 순서) ───
DELETE FROM recommendation_product
 WHERE recommendation_slot_id IN (
   SELECT id FROM recommendation_slot WHERE recommendation_id IN (
     SELECT id FROM recommendation WHERE couple_id IN (
       SELECT id FROM couple
        WHERE inviter_id IN (3,4,5,6,7,8,401403,401404) OR invitee_id IN (3,4,5,6,7,8,401403,401404))));

DELETE FROM recommendation_slot
 WHERE recommendation_id IN (
   SELECT id FROM recommendation WHERE couple_id IN (
     SELECT id FROM couple
      WHERE inviter_id IN (3,4,5,6,7,8,401403,401404) OR invitee_id IN (3,4,5,6,7,8,401403,401404)));

DELETE FROM recommendation
 WHERE couple_id IN (
   SELECT id FROM couple
    WHERE inviter_id IN (3,4,5,6,7,8,401403,401404) OR invitee_id IN (3,4,5,6,7,8,401403,401404));

DELETE FROM report
 WHERE compatibility_result_id IN (
   SELECT id FROM compatibility_result WHERE couple_id IN (
     SELECT id FROM couple
      WHERE inviter_id IN (3,4,5,6,7,8,401403,401404) OR invitee_id IN (3,4,5,6,7,8,401403,401404)));

DELETE FROM compatibility_result
 WHERE couple_id IN (
   SELECT id FROM couple
    WHERE inviter_id IN (3,4,5,6,7,8,401403,401404) OR invitee_id IN (3,4,5,6,7,8,401403,401404));

DELETE FROM personal_recommendation_tax_saving  WHERE member_id IN (3,4,5,6,7,8,401403,401404);
DELETE FROM personal_recommendation_investment  WHERE member_id IN (3,4,5,6,7,8,401403,401404);
DELETE FROM favorite_product                    WHERE member_id IN (3,4,5,6,7,8,401403,401404);
DELETE FROM pension_isa_account                 WHERE member_id IN (3,4,5,6,7,8,401403,401404);
DELETE FROM financial_summary                   WHERE member_id IN (3,4,5,6,7,8,401403,401404);

DELETE FROM personal_survey_investment_experience
 WHERE personal_survey_id IN (SELECT id FROM personal_survey WHERE member_id IN (3,4,5,6,7,8,401403,401404));
DELETE FROM personal_survey WHERE member_id IN (3,4,5,6,7,8,401403,401404);

-- couple 이 invitation_code 를 참조하므로 couple 부터 지운다.
DELETE FROM couple WHERE inviter_id IN (3,4,5,6,7,8,401403,401404) OR invitee_id IN (3,4,5,6,7,8,401403,401404);
DELETE FROM invitation_code
 WHERE common_survey_id IN (SELECT id FROM common_survey WHERE member_id IN (3,4,5,6,7,8,401403,401404));
DELETE FROM common_survey   WHERE member_id IN (3,4,5,6,7,8,401403,401404);

DELETE FROM member_agreement WHERE member_id IN (3,4,5,6,7,8,401403,401404);
DELETE FROM member           WHERE id        IN (3,4,5,6,7,8,401403,401404);

-- 회원
INSERT INTO `member` (`id`, `email`, `password`, `name`, `gender`, `birth_date`, `status`, `last_login_at`, `investment_type`, `kb_star_savings_eligible`, `recommendation_input_updated_at`, `created_at`, `updated_at`, `deleted_at`) VALUES (3,'demo.saver@chaltteok.dev','$2b$10$S7kxCvVErKro6iMGGYtqieadaYQh/mlP/3AOB6q8G81Dl2MoyJeBG','김보람','F','1993-04-10','ACTIVE','2026-08-12 16:36:44','STABLE',1,'2026-08-12 16:48:32','2026-08-12 16:24:00','2026-08-12 16:48:32',NULL);
INSERT INTO `member` (`id`, `email`, `password`, `name`, `gender`, `birth_date`, `status`, `last_login_at`, `investment_type`, `kb_star_savings_eligible`, `recommendation_input_updated_at`, `created_at`, `updated_at`, `deleted_at`) VALUES (4,'demo.investor@chaltteok.dev','$2b$10$S7kxCvVErKro6iMGGYtqieadaYQh/mlP/3AOB6q8G81Dl2MoyJeBG','박준서','M','1992-06-22','ACTIVE','2026-08-12 16:49:11','AGGRESSIVE',0,'2026-08-12 16:50:27','2026-08-12 16:24:00','2026-08-12 16:50:27',NULL);
INSERT INTO `member` (`id`, `email`, `password`, `name`, `gender`, `birth_date`, `status`, `last_login_at`, `investment_type`, `kb_star_savings_eligible`, `recommendation_input_updated_at`, `created_at`, `updated_at`, `deleted_at`) VALUES (5,'demo.newlywed@chaltteok.dev','$2b$10$S7kxCvVErKro6iMGGYtqieadaYQh/mlP/3AOB6q8G81Dl2MoyJeBG','최유진','F','1995-02-14','ACTIVE','2026-08-12 17:06:22','NEUTRAL',0,'2026-08-12 17:07:42','2026-08-12 16:24:00','2026-08-12 17:07:42',NULL);
INSERT INTO `member` (`id`, `email`, `password`, `name`, `gender`, `birth_date`, `status`, `last_login_at`, `investment_type`, `kb_star_savings_eligible`, `recommendation_input_updated_at`, `created_at`, `updated_at`, `deleted_at`) VALUES (6,'demo.renter@chaltteok.dev','$2b$10$S7kxCvVErKro6iMGGYtqieadaYQh/mlP/3AOB6q8G81Dl2MoyJeBG','정민호','M','1994-03-03','ACTIVE','2026-08-12 17:08:07','NEUTRAL',0,'2026-08-12 17:08:52','2026-08-12 16:24:00','2026-08-12 17:08:52',NULL);
INSERT INTO `member` (`id`, `email`, `password`, `name`, `gender`, `birth_date`, `status`, `last_login_at`, `investment_type`, `kb_star_savings_eligible`, `recommendation_input_updated_at`, `created_at`, `updated_at`, `deleted_at`) VALUES (7,'demo.rich@chaltteok.dev','$2b$10$S7kxCvVErKro6iMGGYtqieadaYQh/mlP/3AOB6q8G81Dl2MoyJeBG','한서연','F','1988-01-25','ACTIVE','2026-08-12 17:15:44','STABLE',0,'2026-08-12 17:03:32','2026-08-12 16:24:00','2026-08-12 17:15:44',NULL);
INSERT INTO `member` (`id`, `email`, `password`, `name`, `gender`, `birth_date`, `status`, `last_login_at`, `investment_type`, `kb_star_savings_eligible`, `recommendation_input_updated_at`, `created_at`, `updated_at`, `deleted_at`) VALUES (8,'demo.debt@chaltteok.dev','$2b$10$S7kxCvVErKro6iMGGYtqieadaYQh/mlP/3AOB6q8G81Dl2MoyJeBG','오태윤','M','1996-05-30','ACTIVE','2026-08-12 17:13:45','VERY_AGGRESSIVE',1,'2026-08-12 17:04:58','2026-08-12 16:24:00','2026-08-12 17:13:45',NULL);

-- 약관 동의
INSERT INTO `member_agreement` (`id`, `member_id`, `agree_mydata_terms`, `agree_privacy`, `agree_asset_link`, `agree_couple_share`, `agree_marketing`, `agreed_at`, `created_at`, `updated_at`) VALUES (36,3,1,1,1,1,1,'2026-08-12 09:00:00','2026-08-12 16:24:00','2026-08-12 16:24:00');
INSERT INTO `member_agreement` (`id`, `member_id`, `agree_mydata_terms`, `agree_privacy`, `agree_asset_link`, `agree_couple_share`, `agree_marketing`, `agreed_at`, `created_at`, `updated_at`) VALUES (37,4,1,1,1,1,0,'2026-08-12 09:05:00','2026-08-12 16:24:00','2026-08-12 16:24:00');
INSERT INTO `member_agreement` (`id`, `member_id`, `agree_mydata_terms`, `agree_privacy`, `agree_asset_link`, `agree_couple_share`, `agree_marketing`, `agreed_at`, `created_at`, `updated_at`) VALUES (38,5,1,1,1,1,1,'2026-08-12 09:10:00','2026-08-12 16:24:00','2026-08-12 16:24:00');
INSERT INTO `member_agreement` (`id`, `member_id`, `agree_mydata_terms`, `agree_privacy`, `agree_asset_link`, `agree_couple_share`, `agree_marketing`, `agreed_at`, `created_at`, `updated_at`) VALUES (39,6,1,1,1,1,0,'2026-08-12 09:15:00','2026-08-12 16:24:00','2026-08-12 16:24:00');
INSERT INTO `member_agreement` (`id`, `member_id`, `agree_mydata_terms`, `agree_privacy`, `agree_asset_link`, `agree_couple_share`, `agree_marketing`, `agreed_at`, `created_at`, `updated_at`) VALUES (40,7,1,1,1,1,0,'2026-08-12 09:20:00','2026-08-12 16:24:00','2026-08-12 16:24:00');
INSERT INTO `member_agreement` (`id`, `member_id`, `agree_mydata_terms`, `agree_privacy`, `agree_asset_link`, `agree_couple_share`, `agree_marketing`, `agreed_at`, `created_at`, `updated_at`) VALUES (41,8,1,1,1,1,1,'2026-08-12 09:25:00','2026-08-12 16:24:00','2026-08-12 16:24:00');

-- 개인설문
INSERT INTO `personal_survey` (`id`, `member_id`, `annual_income`, `monthly_available_amount`, `financial_asset_ratio`, `financial_knowledge`, `capital_preservation_attitude`, `created_at`, `updated_at`) VALUES (5,3,60000000,2000000,'OVER_80','LOW','ZERO','2026-08-12 16:48:32','2026-08-12 16:48:32');
INSERT INTO `personal_survey` (`id`, `member_id`, `annual_income`, `monthly_available_amount`, `financial_asset_ratio`, `financial_knowledge`, `capital_preservation_attitude`, `created_at`, `updated_at`) VALUES (6,4,80000000,2500000,'OVER_80','VERY_HIGH','UNDER_20','2026-08-12 16:50:27','2026-08-12 16:50:27');
INSERT INTO `personal_survey` (`id`, `member_id`, `annual_income`, `monthly_available_amount`, `financial_asset_ratio`, `financial_knowledge`, `capital_preservation_attitude`, `created_at`, `updated_at`) VALUES (7,7,120000000,5000000,'OVER_80','VERY_LOW','ZERO','2026-08-12 17:03:32','2026-08-12 17:03:32');
INSERT INTO `personal_survey` (`id`, `member_id`, `annual_income`, `monthly_available_amount`, `financial_asset_ratio`, `financial_knowledge`, `capital_preservation_attitude`, `created_at`, `updated_at`) VALUES (8,8,28000000,100000,'UNDER_10','HIGH','FULL','2026-08-12 17:04:58','2026-08-12 17:04:58');
INSERT INTO `personal_survey` (`id`, `member_id`, `annual_income`, `monthly_available_amount`, `financial_asset_ratio`, `financial_knowledge`, `capital_preservation_attitude`, `created_at`, `updated_at`) VALUES (9,5,32000000,200000,'UNDER_10','MEDIUM','UNDER_20','2026-08-12 17:07:42','2026-08-12 17:07:42');
INSERT INTO `personal_survey` (`id`, `member_id`, `annual_income`, `monthly_available_amount`, `financial_asset_ratio`, `financial_knowledge`, `capital_preservation_attitude`, `created_at`, `updated_at`) VALUES (10,6,30000000,150000,'UNDER_10','MEDIUM','UNDER_20','2026-08-12 17:08:52','2026-08-12 17:08:52');

-- 개인설문 투자경험(다중응답)
INSERT INTO `personal_survey_investment_experience` (`id`, `personal_survey_id`, `investment_experience`, `created_at`, `updated_at`) VALUES (33,5,'LOW_RISK','2026-08-12 16:48:32','2026-08-12 16:48:32');
INSERT INTO `personal_survey_investment_experience` (`id`, `personal_survey_id`, `investment_experience`, `created_at`, `updated_at`) VALUES (34,6,'MODERATE_LOW_RISK','2026-08-12 16:50:27','2026-08-12 16:50:27');
INSERT INTO `personal_survey_investment_experience` (`id`, `personal_survey_id`, `investment_experience`, `created_at`, `updated_at`) VALUES (35,6,'LOW_RISK','2026-08-12 16:50:27','2026-08-12 16:50:27');
INSERT INTO `personal_survey_investment_experience` (`id`, `personal_survey_id`, `investment_experience`, `created_at`, `updated_at`) VALUES (36,6,'MODERATE_RISK','2026-08-12 16:50:27','2026-08-12 16:50:27');
INSERT INTO `personal_survey_investment_experience` (`id`, `personal_survey_id`, `investment_experience`, `created_at`, `updated_at`) VALUES (37,6,'HIGH_RISK','2026-08-12 16:50:27','2026-08-12 16:50:27');
INSERT INTO `personal_survey_investment_experience` (`id`, `personal_survey_id`, `investment_experience`, `created_at`, `updated_at`) VALUES (38,6,'MODERATE_HIGH_RISK','2026-08-12 16:50:27','2026-08-12 16:50:27');
INSERT INTO `personal_survey_investment_experience` (`id`, `personal_survey_id`, `investment_experience`, `created_at`, `updated_at`) VALUES (39,7,'LOW_RISK','2026-08-12 17:03:32','2026-08-12 17:03:32');
INSERT INTO `personal_survey_investment_experience` (`id`, `personal_survey_id`, `investment_experience`, `created_at`, `updated_at`) VALUES (40,8,'MODERATE_LOW_RISK','2026-08-12 17:04:58','2026-08-12 17:04:58');
INSERT INTO `personal_survey_investment_experience` (`id`, `personal_survey_id`, `investment_experience`, `created_at`, `updated_at`) VALUES (41,8,'LOW_RISK','2026-08-12 17:04:58','2026-08-12 17:04:58');
INSERT INTO `personal_survey_investment_experience` (`id`, `personal_survey_id`, `investment_experience`, `created_at`, `updated_at`) VALUES (42,8,'MODERATE_RISK','2026-08-12 17:04:58','2026-08-12 17:04:58');
INSERT INTO `personal_survey_investment_experience` (`id`, `personal_survey_id`, `investment_experience`, `created_at`, `updated_at`) VALUES (43,8,'HIGH_RISK','2026-08-12 17:04:58','2026-08-12 17:04:58');
INSERT INTO `personal_survey_investment_experience` (`id`, `personal_survey_id`, `investment_experience`, `created_at`, `updated_at`) VALUES (44,8,'MODERATE_HIGH_RISK','2026-08-12 17:04:58','2026-08-12 17:04:58');
INSERT INTO `personal_survey_investment_experience` (`id`, `personal_survey_id`, `investment_experience`, `created_at`, `updated_at`) VALUES (45,9,'MODERATE_LOW_RISK','2026-08-12 17:07:42','2026-08-12 17:07:42');
INSERT INTO `personal_survey_investment_experience` (`id`, `personal_survey_id`, `investment_experience`, `created_at`, `updated_at`) VALUES (46,9,'LOW_RISK','2026-08-12 17:07:42','2026-08-12 17:07:42');
INSERT INTO `personal_survey_investment_experience` (`id`, `personal_survey_id`, `investment_experience`, `created_at`, `updated_at`) VALUES (47,10,'MODERATE_LOW_RISK','2026-08-12 17:08:52','2026-08-12 17:08:52');
INSERT INTO `personal_survey_investment_experience` (`id`, `personal_survey_id`, `investment_experience`, `created_at`, `updated_at`) VALUES (48,10,'LOW_RISK','2026-08-12 17:08:52','2026-08-12 17:08:52');
INSERT INTO `personal_survey_investment_experience` (`id`, `personal_survey_id`, `investment_experience`, `created_at`, `updated_at`) VALUES (49,10,'MODERATE_RISK','2026-08-12 17:08:52','2026-08-12 17:08:52');

-- 공동설문
INSERT INTO `common_survey` (`id`, `member_id`, `first_goal_type`, `second_goal_type`, `target_amount`, `target_period_months`, `loan_purpose`, `has_loan_within_one_month`, `created_at`, `updated_at`) VALUES (7,3,'HOUSING','RETIREMENT',500000000,36,'HOUSING',0,'2026-08-12 16:47:39','2026-08-12 16:47:39');
INSERT INTO `common_survey` (`id`, `member_id`, `first_goal_type`, `second_goal_type`, `target_amount`, `target_period_months`, `loan_purpose`, `has_loan_within_one_month`, `created_at`, `updated_at`) VALUES (8,7,'RETIREMENT','INVESTMENT',300000000,60,'HOUSING',0,'2026-08-12 17:03:03','2026-08-12 17:03:03');
INSERT INTO `common_survey` (`id`, `member_id`, `first_goal_type`, `second_goal_type`, `target_amount`, `target_period_months`, `loan_purpose`, `has_loan_within_one_month`, `created_at`, `updated_at`) VALUES (9,5,'MARRIAGE','HOUSING',80000000,24,'JEONSE',1,'2026-08-12 17:07:04','2026-08-12 17:07:04');

-- 초대코드
INSERT INTO `invitation_code` (`id`, `common_survey_id`, `code_value`, `status`, `created_at`, `updated_at`) VALUES (7,7,'YS4Z2HAF','USED','2026-08-12 16:47:39','2026-08-12 16:50:02');
INSERT INTO `invitation_code` (`id`, `common_survey_id`, `code_value`, `status`, `created_at`, `updated_at`) VALUES (8,8,'ZPH8AU56','USED','2026-08-12 17:03:03','2026-08-12 17:04:37');
INSERT INTO `invitation_code` (`id`, `common_survey_id`, `code_value`, `status`, `created_at`, `updated_at`) VALUES (9,9,'HH5CHN2Y','USED','2026-08-12 17:07:04','2026-08-12 17:08:31');

-- 커플
INSERT INTO `couple` (`id`, `inviter_id`, `invitee_id`, `invitation_code_id`, `investment_type`, `profile_message`, `created_at`, `updated_at`) VALUES (2,3,4,7,'DIFF_3',NULL,'2026-08-12 16:50:02','2026-08-12 16:50:27');
INSERT INTO `couple` (`id`, `inviter_id`, `invitee_id`, `invitation_code_id`, `investment_type`, `profile_message`, `created_at`, `updated_at`) VALUES (3,7,8,8,'DIFF_4',NULL,'2026-08-12 17:04:37','2026-08-12 17:04:58');
INSERT INTO `couple` (`id`, `inviter_id`, `invitee_id`, `invitation_code_id`, `investment_type`, `profile_message`, `created_at`, `updated_at`) VALUES (4,5,6,9,'DIFF_0',NULL,'2026-08-12 17:08:31','2026-08-12 17:08:52');

-- 마이데이터 자산·부채 요약
INSERT INTO `financial_summary` (`id`, `member_id`, `financial_asset`, `total_debt`, `available_balance`, `annual_debt_payment`, `average_interest_rate`, `has_high_rate_debt`, `created_at`, `updated_at`) VALUES (23,3,120000000,60000000,15000000,4800000,3.80,0,'2026-08-12 16:37:11','2026-08-12 16:37:11');
INSERT INTO `financial_summary` (`id`, `member_id`, `financial_asset`, `total_debt`, `available_balance`, `annual_debt_payment`, `average_interest_rate`, `has_high_rate_debt`, `created_at`, `updated_at`) VALUES (24,4,123000000,20000000,8000000,6000000,5.90,0,'2026-08-12 16:49:24','2026-08-12 16:49:24');
INSERT INTO `financial_summary` (`id`, `member_id`, `financial_asset`, `total_debt`, `available_balance`, `annual_debt_payment`, `average_interest_rate`, `has_high_rate_debt`, `created_at`, `updated_at`) VALUES (25,7,300000000,0,40000000,0,NULL,0,'2026-08-12 17:02:24','2026-08-12 17:02:24');
INSERT INTO `financial_summary` (`id`, `member_id`, `financial_asset`, `total_debt`, `available_balance`, `annual_debt_payment`, `average_interest_rate`, `has_high_rate_debt`, `created_at`, `updated_at`) VALUES (26,5,3500000,120000000,1200000,9600000,4.60,0,'2026-08-12 17:06:29','2026-08-12 17:06:29');
INSERT INTO `financial_summary` (`id`, `member_id`, `financial_asset`, `total_debt`, `available_balance`, `annual_debt_payment`, `average_interest_rate`, `has_high_rate_debt`, `created_at`, `updated_at`) VALUES (27,6,2400000,57000000,900000,15900000,7.87,0,'2026-08-12 17:08:14','2026-08-12 17:08:14');
INSERT INTO `financial_summary` (`id`, `member_id`, `financial_asset`, `total_debt`, `available_balance`, `annual_debt_payment`, `average_interest_rate`, `has_high_rate_debt`, `created_at`, `updated_at`) VALUES (28,8,2500000,15000000,800000,7000000,17.90,1,'2026-08-12 17:13:53','2026-08-12 17:13:53');

-- 마이데이터 연금·ISA 요약
INSERT INTO `pension_isa_account` (`id`, `member_id`, `has_pension_saving`, `has_irp`, `has_dc`, `has_isa`, `pension_saving_balance`, `irp_balance`, `pension_annual_payment`, `irp_annual_payment`, `dc_annual_payment`, `isa_annual_deposit`, `tax_eligibility_status`, `isa_eligibility_status`, `created_at`, `updated_at`) VALUES (23,3,1,0,0,0,15000000,0,3000000,0,0,0,'ELIGIBLE','ELIGIBLE','2026-08-12 16:37:11','2026-08-12 16:37:11');
INSERT INTO `pension_isa_account` (`id`, `member_id`, `has_pension_saving`, `has_irp`, `has_dc`, `has_isa`, `pension_saving_balance`, `irp_balance`, `pension_annual_payment`, `irp_annual_payment`, `dc_annual_payment`, `isa_annual_deposit`, `tax_eligibility_status`, `isa_eligibility_status`, `created_at`, `updated_at`) VALUES (24,4,0,1,0,1,0,10000000,0,3000000,0,5000000,'ELIGIBLE','ELIGIBLE','2026-08-12 16:49:24','2026-08-12 16:49:24');
INSERT INTO `pension_isa_account` (`id`, `member_id`, `has_pension_saving`, `has_irp`, `has_dc`, `has_isa`, `pension_saving_balance`, `irp_balance`, `pension_annual_payment`, `irp_annual_payment`, `dc_annual_payment`, `isa_annual_deposit`, `tax_eligibility_status`, `isa_eligibility_status`, `created_at`, `updated_at`) VALUES (25,7,1,1,0,0,30000000,20000000,6000000,3000000,0,0,'ELIGIBLE','ELIGIBLE','2026-08-12 17:02:24','2026-08-12 17:02:24');
INSERT INTO `pension_isa_account` (`id`, `member_id`, `has_pension_saving`, `has_irp`, `has_dc`, `has_isa`, `pension_saving_balance`, `irp_balance`, `pension_annual_payment`, `irp_annual_payment`, `dc_annual_payment`, `isa_annual_deposit`, `tax_eligibility_status`, `isa_eligibility_status`, `created_at`, `updated_at`) VALUES (26,5,0,0,0,0,0,0,0,0,0,0,'ELIGIBLE','ELIGIBLE','2026-08-12 17:06:29','2026-08-12 17:06:29');
INSERT INTO `pension_isa_account` (`id`, `member_id`, `has_pension_saving`, `has_irp`, `has_dc`, `has_isa`, `pension_saving_balance`, `irp_balance`, `pension_annual_payment`, `irp_annual_payment`, `dc_annual_payment`, `isa_annual_deposit`, `tax_eligibility_status`, `isa_eligibility_status`, `created_at`, `updated_at`) VALUES (27,6,0,0,0,0,0,0,0,0,0,0,'ELIGIBLE','ELIGIBLE','2026-08-12 17:08:14','2026-08-12 17:08:14');
INSERT INTO `pension_isa_account` (`id`, `member_id`, `has_pension_saving`, `has_irp`, `has_dc`, `has_isa`, `pension_saving_balance`, `irp_balance`, `pension_annual_payment`, `irp_annual_payment`, `dc_annual_payment`, `isa_annual_deposit`, `tax_eligibility_status`, `isa_eligibility_status`, `created_at`, `updated_at`) VALUES (28,8,0,0,0,0,0,0,0,0,0,0,'ELIGIBLE','ELIGIBLE','2026-08-12 17:13:53','2026-08-12 17:13:53');

-- 궁합 점수
INSERT INTO `compatibility_result` (`id`, `couple_id`, `asset_stability_score`, `debt_repayment_score`, `financial_value_score`, `goal_feasibility_score`, `tax_strategy_score`, `tax_strategy_calculated`, `total_score`, `result_summary`, `created_at`, `updated_at`) VALUES (2,2,14.86,15.31,13.20,15.54,2.50,1,62.00,NULL,'2026-08-12 16:50:27','2026-08-12 16:50:27');
INSERT INTO `compatibility_result` (`id`, `couple_id`, `asset_stability_score`, `debt_repayment_score`, `financial_value_score`, `goal_feasibility_score`, `tax_strategy_score`, `tax_strategy_calculated`, `total_score`, `result_summary`, `created_at`, `updated_at`) VALUES (4,3,13.58,14.79,1.00,20.00,3.00,1,53.00,NULL,'2026-08-12 17:14:11','2026-08-12 17:14:11');
INSERT INTO `compatibility_result` (`id`, `couple_id`, `asset_stability_score`, `debt_repayment_score`, `financial_value_score`, `goal_feasibility_score`, `tax_strategy_score`, `tax_strategy_calculated`, `total_score`, `result_summary`, `created_at`, `updated_at`) VALUES (3,4,0.54,1.05,24.00,3.73,0.00,1,30.00,NULL,'2026-08-12 17:08:52','2026-08-12 17:08:52');

-- 리포트
INSERT INTO `report` (`id`, `compatibility_result_id`, `asset_stability_reason`, `debt_repayment_reason`, `financial_value_reason`, `goal_feasibility_reason`, `expected_asset`, `tax_strategy_reason`, `expert_comment`, `created_at`, `updated_at`) VALUES (5,2,'두 분의 연령대를 각각 고려했을 때, 현재 합산 금융자산은 또래 기준보다 안정적인 편이에요.','두 분 모두 부채가 있어요. 하지만 부채 위험도는 보람님이 더 높아요.','두 분은 총자산 중 금융자산 비중은 비슷하지만, 투자 경험·금융 투자 상품 이해도·손실 감내력은 차이가 나요.','36개월 후 예상 자산은 약 3억 8860만원이에요. 목표 금액 5억 대비 약 1억 1139만원 모자라요.',388606793,'보람님은 ISA·IRP 계좌를 개설하지 않았어요. 준서님은 연금저축 계좌를 개설하지 않았어요. 추천탭에서 상품들을 만나보세요.','보람님과 준서님은 목표 달성 가능성이 탄탄해요. 현재 목표의 78% 도달이 예상되며, 목표 금액에 1억 1139만원 부족한 상황이에요. 부채 상환 수준도 잘 관리되고 있으며, 보람님이 6000만원, 준서님이 2000만원을 상환하고 있어요. 금융 자산 부분도 두 분의 합산이 2억 4300만원으로 또래 중앙값 대비 130%에 달해요.\n\n투자 가치관은 조금 보완이 필요해요. 두 분의 투자 가치관 일치도가 53%로 다소 낮은 편이에요. 절세 활용 부분도 신경 쓰면 좋겠어요. 보람님은 ISA와 IRP를 개설하지 않았고, 연금저축 한도에서는 300만원이 남아 있어요. 준서님은 ISA 한도까지 1500만원, IRP 한도까지 600만원이 남아있고 연금저축 또한 미개설이에요. 남은 한도를 채워보세요. 추천탭에서 확인해보세요.','2026-08-12 16:50:27','2026-08-12 16:50:27');
INSERT INTO `report` (`id`, `compatibility_result_id`, `asset_stability_reason`, `debt_repayment_reason`, `financial_value_reason`, `goal_feasibility_reason`, `expected_asset`, `tax_strategy_reason`, `expert_comment`, `created_at`, `updated_at`) VALUES (10,3,'금융 자산을 쌓기 위해 더 분발할 필요가 있어요. 금융 자산을 쌓는 걸 도와줄 상품들을 추천탭에서 만나보세요.','두 분 모두 부채가 있어요. 하지만 부채 위험도는 민호님이 더 높아요.','두 분은 가치관이 비슷해요.','24개월 후 예상 자산은 약 1490만원이에요. 목표 금액 8000만원 대비 약 6509만원 모자라요.',14901917,'민호님은 ISA·IRP·연금저축 모두 개설 안 하셨어요. 유진님은 ISA·IRP·연금저축 모두 개설 안 하셨어요. 추천탭에서 상품들을 만나보세요.','**투자 가치관이 잘 맞아요.** 두 분의 투자 가치관 일치도가 96%에 이르기 때문에, 공동의 목표를 향해 나아가는 데 큰 장점이 있어요.\n\n**목표 달성 가능성에 조금 더 신경 쓰면 좋아요.** 현재 2년 뒤 목표의 19% 도달이 예상되고, 6509만원이 부족한 상황이에요. **부채 상환에 보완이 필요해요.** 유진님의 부채가 1억 2000만원, 민호님은 5700만원에요. **금융 자산이 아쉬워요.** 두 분의 합산 금융 자산이 590만원으로 또래 중앙값 대비 3%에 불과해요. **절세 활용은 강화해야 해요.** 유진님과 민호님 모두 ISA, IRP, 연금저축을 미개설한 상태에요. 이렇게 보완하면 목표 달성과 재정 상황이 개선될 가능성이 높아요.','2026-08-12 17:08:52','2026-08-12 17:08:52');
INSERT INTO `report` (`id`, `compatibility_result_id`, `asset_stability_reason`, `debt_repayment_reason`, `financial_value_reason`, `goal_feasibility_reason`, `expected_asset`, `tax_strategy_reason`, `expert_comment`, `created_at`, `updated_at`) VALUES (15,4,'두 분의 연령대를 각각 고려했을 때, 현재 합산 금융자산은 또래 기준보다 안정적인 편이에요.','부채 점수 감점에 태윤님이 더 큰 영향을 끼쳤어요.','두 분은 총자산 중 금융자산 비중·투자 경험·금융 투자 상품 이해도·손실 감내력이 차이가 나요.','60개월 후 목표 달성 가능성이 커요. 목표 달성을 더 확실하게 도와줄 상품을 추천탭에서 만나보세요.',680043313,'태윤님은 ISA·IRP·연금저축 모두 개설 안 하셨어요. 서연님은 ISA 계좌를 개설하지 않았어요. 추천탭에서 상품들을 만나보세요.','현재 두 분의 목표 달성 가능성이 아주 높아요. 5년 뒤 목표인 3억 원을 초과 달성할 것으로 예상되고, 6억 8004만원에 이를 것으로 보이네요. 부채 상환도 잘하고 있어요. 태윤님은 1500만원의 부채를 가지고 있는데, 이 부분도 관리가 잘 되고 있어요. 금융 자산도 안정적이에요. 두 분의 합산 금융 자산이 3억 250만원으로 동년 배경에 비해 아주 좋은 수준이에요. **목표 달성이 가능성이 높고 부채와 자산 관리를 잘 하고 있어요.**\n\n절세 활용이 조금 아쉬워요. 서연님과 태윤님 모두 ISA와 IRP를 개설하지 않으셨고, 서연님은 한도에 600만원이 남아 있어요. 또, 두 분의 투자 가치관 일치도가 너무 낮아요. 현재 4%로 일치하지 않아서, 이 부분에 조금 더 신경 쓰면 좋아요. **절세와 투자 가치관 부분에서 보완이 필요해요.**','2026-08-12 17:14:11','2026-08-12 17:14:11');

-- 추천
INSERT INTO `recommendation` (`id`, `couple_id`, `created_at`, `updated_at`) VALUES (333,2,'2026-08-12 16:51:06','2026-08-12 16:51:06');
INSERT INTO `recommendation` (`id`, `couple_id`, `created_at`, `updated_at`) VALUES (335,3,'2026-08-12 17:15:44','2026-08-12 17:15:44');
INSERT INTO `recommendation` (`id`, `couple_id`, `created_at`, `updated_at`) VALUES (334,4,'2026-08-12 17:09:02','2026-08-12 17:09:02');

-- 추천 슬롯
INSERT INTO `recommendation_slot` (`id`, `recommendation_id`, `slot_type`, `created_at`, `updated_at`) VALUES (1174,333,'LOAN','2026-08-12 16:51:06','2026-08-12 16:51:06');
INSERT INTO `recommendation_slot` (`id`, `recommendation_id`, `slot_type`, `created_at`, `updated_at`) VALUES (1175,333,'SAVINGS','2026-08-12 16:51:06','2026-08-12 16:51:06');
INSERT INTO `recommendation_slot` (`id`, `recommendation_id`, `slot_type`, `created_at`, `updated_at`) VALUES (1176,333,'INVESTMENT','2026-08-12 16:51:06','2026-08-12 16:51:06');
INSERT INTO `recommendation_slot` (`id`, `recommendation_id`, `slot_type`, `created_at`, `updated_at`) VALUES (1177,333,'DEPOSIT','2026-08-12 16:51:06','2026-08-12 16:51:06');
INSERT INTO `recommendation_slot` (`id`, `recommendation_id`, `slot_type`, `created_at`, `updated_at`) VALUES (1178,334,'LOAN','2026-08-12 17:09:02','2026-08-12 17:09:02');
INSERT INTO `recommendation_slot` (`id`, `recommendation_id`, `slot_type`, `created_at`, `updated_at`) VALUES (1179,334,'SAVINGS','2026-08-12 17:09:02','2026-08-12 17:09:02');
INSERT INTO `recommendation_slot` (`id`, `recommendation_id`, `slot_type`, `created_at`, `updated_at`) VALUES (1180,335,'SAVINGS','2026-08-12 17:15:44','2026-08-12 17:15:44');
INSERT INTO `recommendation_slot` (`id`, `recommendation_id`, `slot_type`, `created_at`, `updated_at`) VALUES (1181,335,'INVESTMENT','2026-08-12 17:15:44','2026-08-12 17:15:44');
INSERT INTO `recommendation_slot` (`id`, `recommendation_id`, `slot_type`, `created_at`, `updated_at`) VALUES (1182,335,'DEPOSIT','2026-08-12 17:15:44','2026-08-12 17:15:44');

-- 추천 상품
INSERT INTO `recommendation_product` (`id`, `recommendation_slot_id`, `product_id`, `rank_no`, `is_selected`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (4380,1174,34,1,1,'대출 목적과 조건에 맞고 금리와 신청 편의성이 좋은 상품이에요.','2026-08-12 16:51:06','2026-08-12 16:51:06');
INSERT INTO `recommendation_product` (`id`, `recommendation_slot_id`, `product_id`, `rank_no`, `is_selected`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (4381,1174,33,2,0,NULL,'2026-08-12 16:51:06','2026-08-12 16:51:06');
INSERT INTO `recommendation_product` (`id`, `recommendation_slot_id`, `product_id`, `rank_no`, `is_selected`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (4382,1175,5,1,1,'KB스타적금 가입 대상이며 목표 기간과 월 저축액에도 맞아요.','2026-08-12 16:51:06','2026-08-12 16:51:06');
INSERT INTO `recommendation_product` (`id`, `recommendation_slot_id`, `product_id`, `rank_no`, `is_selected`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (4383,1175,6,2,0,NULL,'2026-08-12 16:51:06','2026-08-12 16:51:06');
INSERT INTO `recommendation_product` (`id`, `recommendation_slot_id`, `product_id`, `rank_no`, `is_selected`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (4384,1175,7,3,0,NULL,'2026-08-12 16:51:06','2026-08-12 16:51:06');
INSERT INTO `recommendation_product` (`id`, `recommendation_slot_id`, `product_id`, `rank_no`, `is_selected`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (4385,1175,4,4,0,NULL,'2026-08-12 16:51:06','2026-08-12 16:51:06');
INSERT INTO `recommendation_product` (`id`, `recommendation_slot_id`, `product_id`, `rank_no`, `is_selected`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (4386,1175,3,5,0,NULL,'2026-08-12 16:51:06','2026-08-12 16:51:06');
INSERT INTO `recommendation_product` (`id`, `recommendation_slot_id`, `product_id`, `rank_no`, `is_selected`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (4387,1176,14,1,1,'투자성향에 맞는 위험등급 중 순자산이 큰 상품을 골랐어요.','2026-08-12 16:51:06','2026-08-12 16:51:06');
INSERT INTO `recommendation_product` (`id`, `recommendation_slot_id`, `product_id`, `rank_no`, `is_selected`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (4388,1176,13,2,0,NULL,'2026-08-12 16:51:06','2026-08-12 16:51:06');
INSERT INTO `recommendation_product` (`id`, `recommendation_slot_id`, `product_id`, `rank_no`, `is_selected`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (4389,1177,2,1,1,'두 분의 목표 기간에 맞고 기본금리가 높은 예금 상품이에요.','2026-08-12 16:51:06','2026-08-12 16:51:06');
INSERT INTO `recommendation_product` (`id`, `recommendation_slot_id`, `product_id`, `rank_no`, `is_selected`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (4390,1177,1,2,0,NULL,'2026-08-12 16:51:06','2026-08-12 16:51:06');
INSERT INTO `recommendation_product` (`id`, `recommendation_slot_id`, `product_id`, `rank_no`, `is_selected`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (4391,1178,32,1,1,'대출 목적과 조건에 맞고 금리와 신청 편의성이 좋은 상품이에요.','2026-08-12 17:09:02','2026-08-12 17:09:02');
INSERT INTO `recommendation_product` (`id`, `recommendation_slot_id`, `product_id`, `rank_no`, `is_selected`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (4392,1178,29,2,0,NULL,'2026-08-12 17:09:02','2026-08-12 17:09:02');
INSERT INTO `recommendation_product` (`id`, `recommendation_slot_id`, `product_id`, `rank_no`, `is_selected`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (4393,1178,30,3,0,NULL,'2026-08-12 17:09:02','2026-08-12 17:09:02');
INSERT INTO `recommendation_product` (`id`, `recommendation_slot_id`, `product_id`, `rank_no`, `is_selected`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (4394,1178,31,4,0,NULL,'2026-08-12 17:09:02','2026-08-12 17:09:02');
INSERT INTO `recommendation_product` (`id`, `recommendation_slot_id`, `product_id`, `rank_no`, `is_selected`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (4395,1179,6,1,1,'목표 기간과 월 저축액에 맞고 기본금리가 높은 적금 상품이에요.','2026-08-12 17:09:02','2026-08-12 17:09:02');
INSERT INTO `recommendation_product` (`id`, `recommendation_slot_id`, `product_id`, `rank_no`, `is_selected`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (4396,1179,4,2,0,NULL,'2026-08-12 17:09:02','2026-08-12 17:09:02');
INSERT INTO `recommendation_product` (`id`, `recommendation_slot_id`, `product_id`, `rank_no`, `is_selected`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (4397,1179,3,3,0,NULL,'2026-08-12 17:09:02','2026-08-12 17:09:02');
INSERT INTO `recommendation_product` (`id`, `recommendation_slot_id`, `product_id`, `rank_no`, `is_selected`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (4398,1180,5,1,1,'KB스타적금 가입 대상이며 목표 기간과 월 저축액에도 맞아요.','2026-08-12 17:15:44','2026-08-12 17:15:44');
INSERT INTO `recommendation_product` (`id`, `recommendation_slot_id`, `product_id`, `rank_no`, `is_selected`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (4399,1180,6,2,0,NULL,'2026-08-12 17:15:44','2026-08-12 17:15:44');
INSERT INTO `recommendation_product` (`id`, `recommendation_slot_id`, `product_id`, `rank_no`, `is_selected`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (4400,1180,7,3,0,NULL,'2026-08-12 17:15:44','2026-08-12 17:15:44');
INSERT INTO `recommendation_product` (`id`, `recommendation_slot_id`, `product_id`, `rank_no`, `is_selected`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (4401,1180,4,4,0,NULL,'2026-08-12 17:15:44','2026-08-12 17:15:44');
INSERT INTO `recommendation_product` (`id`, `recommendation_slot_id`, `product_id`, `rank_no`, `is_selected`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (4402,1180,3,5,0,NULL,'2026-08-12 17:15:44','2026-08-12 17:15:44');
INSERT INTO `recommendation_product` (`id`, `recommendation_slot_id`, `product_id`, `rank_no`, `is_selected`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (4403,1181,14,1,1,'투자성향에 맞는 위험등급 중 순자산이 큰 상품을 골랐어요.','2026-08-12 17:15:44','2026-08-12 17:15:44');
INSERT INTO `recommendation_product` (`id`, `recommendation_slot_id`, `product_id`, `rank_no`, `is_selected`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (4404,1181,13,2,0,NULL,'2026-08-12 17:15:44','2026-08-12 17:15:44');
INSERT INTO `recommendation_product` (`id`, `recommendation_slot_id`, `product_id`, `rank_no`, `is_selected`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (4405,1182,2,1,1,'두 분의 목표 기간에 맞고 기본금리가 높은 예금 상품이에요.','2026-08-12 17:15:44','2026-08-12 17:15:44');
INSERT INTO `recommendation_product` (`id`, `recommendation_slot_id`, `product_id`, `rank_no`, `is_selected`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (4406,1182,1,2,0,NULL,'2026-08-12 17:15:44','2026-08-12 17:15:44');

-- 개인 투자 추천
INSERT INTO `personal_recommendation_investment` (`id`, `member_id`, `product_id`, `rank_no`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (3186,4,22,1,'투자성향에 맞는 위험등급 중 순자산이 큰 상품을 골랐어요.','2026-08-12 16:51:06','2026-08-12 16:51:06');
INSERT INTO `personal_recommendation_investment` (`id`, `member_id`, `product_id`, `rank_no`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (3187,4,23,2,NULL,'2026-08-12 16:51:06','2026-08-12 16:51:06');
INSERT INTO `personal_recommendation_investment` (`id`, `member_id`, `product_id`, `rank_no`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (3188,4,21,3,NULL,'2026-08-12 16:51:06','2026-08-12 16:51:06');
INSERT INTO `personal_recommendation_investment` (`id`, `member_id`, `product_id`, `rank_no`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (3189,4,18,4,NULL,'2026-08-12 16:51:06','2026-08-12 16:51:06');
INSERT INTO `personal_recommendation_investment` (`id`, `member_id`, `product_id`, `rank_no`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (3190,4,20,5,NULL,'2026-08-12 16:51:06','2026-08-12 16:51:06');
INSERT INTO `personal_recommendation_investment` (`id`, `member_id`, `product_id`, `rank_no`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (3191,4,17,6,NULL,'2026-08-12 16:51:06','2026-08-12 16:51:06');
INSERT INTO `personal_recommendation_investment` (`id`, `member_id`, `product_id`, `rank_no`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (3192,4,19,7,NULL,'2026-08-12 16:51:06','2026-08-12 16:51:06');
INSERT INTO `personal_recommendation_investment` (`id`, `member_id`, `product_id`, `rank_no`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (3193,4,15,8,NULL,'2026-08-12 16:51:06','2026-08-12 16:51:06');
INSERT INTO `personal_recommendation_investment` (`id`, `member_id`, `product_id`, `rank_no`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (3194,4,16,9,NULL,'2026-08-12 16:51:06','2026-08-12 16:51:06');
INSERT INTO `personal_recommendation_investment` (`id`, `member_id`, `product_id`, `rank_no`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (3195,4,14,10,NULL,'2026-08-12 16:51:06','2026-08-12 16:51:06');
INSERT INTO `personal_recommendation_investment` (`id`, `member_id`, `product_id`, `rank_no`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (3196,4,13,11,NULL,'2026-08-12 16:51:06','2026-08-12 16:51:06');
INSERT INTO `personal_recommendation_investment` (`id`, `member_id`, `product_id`, `rank_no`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (3197,8,21,1,'노후 목표와 투자성향에 맞는 TDF 상품을 우선 골랐어요.','2026-08-12 17:15:44','2026-08-12 17:15:44');
INSERT INTO `personal_recommendation_investment` (`id`, `member_id`, `product_id`, `rank_no`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (3198,8,20,2,NULL,'2026-08-12 17:15:44','2026-08-12 17:15:44');
INSERT INTO `personal_recommendation_investment` (`id`, `member_id`, `product_id`, `rank_no`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (3199,8,19,3,NULL,'2026-08-12 17:15:44','2026-08-12 17:15:44');
INSERT INTO `personal_recommendation_investment` (`id`, `member_id`, `product_id`, `rank_no`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (3200,8,27,4,NULL,'2026-08-12 17:15:44','2026-08-12 17:15:44');
INSERT INTO `personal_recommendation_investment` (`id`, `member_id`, `product_id`, `rank_no`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (3201,8,26,5,NULL,'2026-08-12 17:15:44','2026-08-12 17:15:44');
INSERT INTO `personal_recommendation_investment` (`id`, `member_id`, `product_id`, `rank_no`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (3202,8,25,6,NULL,'2026-08-12 17:15:44','2026-08-12 17:15:44');
INSERT INTO `personal_recommendation_investment` (`id`, `member_id`, `product_id`, `rank_no`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (3203,8,24,7,NULL,'2026-08-12 17:15:44','2026-08-12 17:15:44');
INSERT INTO `personal_recommendation_investment` (`id`, `member_id`, `product_id`, `rank_no`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (3204,8,22,8,NULL,'2026-08-12 17:15:44','2026-08-12 17:15:44');
INSERT INTO `personal_recommendation_investment` (`id`, `member_id`, `product_id`, `rank_no`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (3205,8,23,9,NULL,'2026-08-12 17:15:44','2026-08-12 17:15:44');
INSERT INTO `personal_recommendation_investment` (`id`, `member_id`, `product_id`, `rank_no`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (3206,8,18,10,NULL,'2026-08-12 17:15:44','2026-08-12 17:15:44');
INSERT INTO `personal_recommendation_investment` (`id`, `member_id`, `product_id`, `rank_no`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (3207,8,17,11,NULL,'2026-08-12 17:15:44','2026-08-12 17:15:44');
INSERT INTO `personal_recommendation_investment` (`id`, `member_id`, `product_id`, `rank_no`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (3208,8,15,12,NULL,'2026-08-12 17:15:44','2026-08-12 17:15:44');
INSERT INTO `personal_recommendation_investment` (`id`, `member_id`, `product_id`, `rank_no`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (3209,8,16,13,NULL,'2026-08-12 17:15:44','2026-08-12 17:15:44');
INSERT INTO `personal_recommendation_investment` (`id`, `member_id`, `product_id`, `rank_no`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (3210,8,14,14,NULL,'2026-08-12 17:15:44','2026-08-12 17:15:44');
INSERT INTO `personal_recommendation_investment` (`id`, `member_id`, `product_id`, `rank_no`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (3211,8,13,15,NULL,'2026-08-12 17:15:44','2026-08-12 17:15:44');

-- 개인 절세 추천
INSERT INTO `personal_recommendation_tax_saving` (`id`, `member_id`, `product_id`, `rank_no`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (9,3,9,1,'계좌 현황과 금융 이해도에 잘 맞는 ISA 상품을 골랐어요.','2026-08-12 16:51:06','2026-08-12 16:51:06');
INSERT INTO `personal_recommendation_tax_saving` (`id`, `member_id`, `product_id`, `rank_no`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (10,3,8,2,NULL,'2026-08-12 16:51:06','2026-08-12 16:51:06');
INSERT INTO `personal_recommendation_tax_saving` (`id`, `member_id`, `product_id`, `rank_no`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (11,3,12,3,NULL,'2026-08-12 16:51:06','2026-08-12 16:51:06');
INSERT INTO `personal_recommendation_tax_saving` (`id`, `member_id`, `product_id`, `rank_no`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (12,3,11,4,NULL,'2026-08-12 16:51:06','2026-08-12 16:51:06');
INSERT INTO `personal_recommendation_tax_saving` (`id`, `member_id`, `product_id`, `rank_no`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (13,4,10,1,'노후 목표와 계좌 현황에 잘 맞는 연금저축 상품을 골랐어요.','2026-08-12 16:51:06','2026-08-12 16:51:06');
INSERT INTO `personal_recommendation_tax_saving` (`id`, `member_id`, `product_id`, `rank_no`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (14,6,8,1,'계좌 현황과 금융 이해도에 잘 맞는 ISA 상품을 골랐어요.','2026-08-12 17:09:02','2026-08-12 17:09:02');
INSERT INTO `personal_recommendation_tax_saving` (`id`, `member_id`, `product_id`, `rank_no`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (15,6,9,2,NULL,'2026-08-12 17:09:02','2026-08-12 17:09:02');
INSERT INTO `personal_recommendation_tax_saving` (`id`, `member_id`, `product_id`, `rank_no`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (16,5,8,1,'계좌 현황과 금융 이해도에 잘 맞는 ISA 상품을 골랐어요.','2026-08-12 17:09:02','2026-08-12 17:09:02');
INSERT INTO `personal_recommendation_tax_saving` (`id`, `member_id`, `product_id`, `rank_no`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (17,5,9,2,NULL,'2026-08-12 17:09:02','2026-08-12 17:09:02');
INSERT INTO `personal_recommendation_tax_saving` (`id`, `member_id`, `product_id`, `rank_no`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (18,7,9,1,'계좌 현황과 금융 이해도에 잘 맞는 ISA 상품을 골랐어요.','2026-08-12 17:15:44','2026-08-12 17:15:44');
INSERT INTO `personal_recommendation_tax_saving` (`id`, `member_id`, `product_id`, `rank_no`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (19,7,8,2,NULL,'2026-08-12 17:15:44','2026-08-12 17:15:44');
INSERT INTO `personal_recommendation_tax_saving` (`id`, `member_id`, `product_id`, `rank_no`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (20,8,10,1,'노후 목표와 계좌 현황에 잘 맞는 연금저축 상품을 골랐어요.','2026-08-12 17:15:44','2026-08-12 17:15:44');
INSERT INTO `personal_recommendation_tax_saving` (`id`, `member_id`, `product_id`, `rank_no`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (21,8,11,2,NULL,'2026-08-12 17:15:44','2026-08-12 17:15:44');
INSERT INTO `personal_recommendation_tax_saving` (`id`, `member_id`, `product_id`, `rank_no`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (22,8,12,3,NULL,'2026-08-12 17:15:44','2026-08-12 17:15:44');
INSERT INTO `personal_recommendation_tax_saving` (`id`, `member_id`, `product_id`, `rank_no`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (23,8,8,4,NULL,'2026-08-12 17:15:44','2026-08-12 17:15:44');
INSERT INTO `personal_recommendation_tax_saving` (`id`, `member_id`, `product_id`, `rank_no`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (24,8,9,5,NULL,'2026-08-12 17:15:44','2026-08-12 17:15:44');

-- =====================================================================
-- 커플5 — 박정훈(401403) + 최은영(401404). 위 3~8번과 마찬가지로 앱에서 실제
-- 가입·연동·설문을 마친 뒤 덤프한 값이다. FK 의존 순서(회원 → 마이데이터/설문 →
-- 공동설문 → 초대코드 → 커플 → 궁합점수 → 리포트 → 추천)대로 삽입한다.
-- =====================================================================

-- 회원
INSERT INTO `member` (`id`, `email`, `password`, `name`, `gender`, `birth_date`, `status`, `last_login_at`, `investment_type`, `kb_star_savings_eligible`, `recommendation_input_updated_at`, `created_at`, `updated_at`, `deleted_at`) VALUES (401403,'demo.video.retire1@chaltteok.dev','$2a$10$JxnEElVdBJax1ZGPbd/eb.mz99XhrLmGm68uTcTfF.O7bmKWBFxAi','박정훈','M','1980-05-14','ACTIVE','2026-08-20 16:28:36','AGGRESSIVE',0,'2026-08-20 13:01:27','2026-08-20 13:01:09','2026-08-20 16:28:36',NULL);
INSERT INTO `member` (`id`, `email`, `password`, `name`, `gender`, `birth_date`, `status`, `last_login_at`, `investment_type`, `kb_star_savings_eligible`, `recommendation_input_updated_at`, `created_at`, `updated_at`, `deleted_at`) VALUES (401404,'demo.video.retire2@chaltteok.dev','$2a$10$bDwJ/O4HCVCclTFHJKfaG.bnUciWLt7e6a6AJAV4wG/sL9IP3NjCK','최은영','F','1982-11-02','ACTIVE',NULL,'NEUTRAL',0,'2026-08-20 13:01:27','2026-08-20 13:01:09','2026-08-20 13:01:27',NULL);

-- 약관 동의
INSERT INTO `member_agreement` (`id`, `member_id`, `agree_mydata_terms`, `agree_privacy`, `agree_asset_link`, `agree_couple_share`, `agree_marketing`, `agreed_at`, `created_at`, `updated_at`) VALUES (80,401403,1,1,1,1,0,'2026-08-20 13:01:09','2026-08-20 13:01:09','2026-08-20 13:01:09');
INSERT INTO `member_agreement` (`id`, `member_id`, `agree_mydata_terms`, `agree_privacy`, `agree_asset_link`, `agree_couple_share`, `agree_marketing`, `agreed_at`, `created_at`, `updated_at`) VALUES (81,401404,1,1,1,1,0,'2026-08-20 13:01:09','2026-08-20 13:01:09','2026-08-20 13:01:09');

-- 개인설문
INSERT INTO `personal_survey` (`id`, `member_id`, `annual_income`, `monthly_available_amount`, `financial_asset_ratio`, `financial_knowledge`, `capital_preservation_attitude`, `created_at`, `updated_at`) VALUES (973,401403,100000000,2500000,'UNDER_50','MEDIUM','UNDER_20','2026-08-20 13:01:27','2026-08-20 13:01:27');
INSERT INTO `personal_survey` (`id`, `member_id`, `annual_income`, `monthly_available_amount`, `financial_asset_ratio`, `financial_knowledge`, `capital_preservation_attitude`, `created_at`, `updated_at`) VALUES (974,401404,60000000,1500000,'UNDER_30','LOW','UNDER_10','2026-08-20 13:01:27','2026-08-20 13:01:27');

-- 개인설문 투자경험(다중응답)
INSERT INTO `personal_survey_investment_experience` (`id`, `personal_survey_id`, `investment_experience`, `created_at`, `updated_at`) VALUES (274,973,'LOW_RISK','2026-08-20 13:01:27','2026-08-20 13:01:27');
INSERT INTO `personal_survey_investment_experience` (`id`, `personal_survey_id`, `investment_experience`, `created_at`, `updated_at`) VALUES (275,973,'MODERATE_RISK','2026-08-20 13:01:27','2026-08-20 13:01:27');
INSERT INTO `personal_survey_investment_experience` (`id`, `personal_survey_id`, `investment_experience`, `created_at`, `updated_at`) VALUES (276,973,'MODERATE_LOW_RISK','2026-08-20 13:01:27','2026-08-20 13:01:27');
INSERT INTO `personal_survey_investment_experience` (`id`, `personal_survey_id`, `investment_experience`, `created_at`, `updated_at`) VALUES (277,974,'LOW_RISK','2026-08-20 13:01:27','2026-08-20 13:01:27');
INSERT INTO `personal_survey_investment_experience` (`id`, `personal_survey_id`, `investment_experience`, `created_at`, `updated_at`) VALUES (278,974,'MODERATE_LOW_RISK','2026-08-20 13:01:27','2026-08-20 13:01:27');

-- 공동설문 (초대자 박정훈이 작성)
INSERT INTO `common_survey` (`id`, `member_id`, `first_goal_type`, `second_goal_type`, `target_amount`, `target_period_months`, `loan_purpose`, `has_loan_within_one_month`, `created_at`, `updated_at`) VALUES (201097,401403,'RETIREMENT','INVESTMENT',1000000000,180,'NONE',0,'2026-08-20 13:01:16','2026-08-20 13:01:16');

-- 초대코드
INSERT INTO `invitation_code` (`id`, `common_survey_id`, `code_value`, `status`, `created_at`, `updated_at`) VALUES (201067,201097,'DFV6ZDBG','USED','2026-08-20 13:01:16','2026-08-20 13:01:16');

-- 커플
INSERT INTO `couple` (`id`, `inviter_id`, `invitee_id`, `invitation_code_id`, `investment_type`, `profile_message`, `created_at`, `updated_at`) VALUES (201017,401403,401404,201067,'DIFF_1',NULL,'2026-08-20 13:01:16','2026-08-20 13:01:27');

-- 마이데이터 자산·부채 요약
INSERT INTO `financial_summary` (`id`, `member_id`, `financial_asset`, `total_debt`, `available_balance`, `annual_debt_payment`, `average_interest_rate`, `has_high_rate_debt`, `created_at`, `updated_at`) VALUES (78,401403,40000000,10000000,5000000,3000000,5.50,0,'2026-08-20 13:01:16','2026-08-20 13:01:16');
INSERT INTO `financial_summary` (`id`, `member_id`, `financial_asset`, `total_debt`, `available_balance`, `annual_debt_payment`, `average_interest_rate`, `has_high_rate_debt`, `created_at`, `updated_at`) VALUES (79,401404,40000000,10000000,5000000,3000000,5.50,0,'2026-08-20 13:01:16','2026-08-20 13:01:16');

-- 마이데이터 연금·ISA 요약
INSERT INTO `pension_isa_account` (`id`, `member_id`, `has_pension_saving`, `has_irp`, `has_dc`, `has_isa`, `pension_saving_balance`, `irp_balance`, `pension_annual_payment`, `irp_annual_payment`, `dc_annual_payment`, `isa_annual_deposit`, `tax_eligibility_status`, `isa_eligibility_status`, `created_at`, `updated_at`) VALUES (78,401403,0,0,0,0,0,0,0,0,0,0,'ELIGIBLE','ELIGIBLE','2026-08-20 13:01:16','2026-08-20 13:01:16');
INSERT INTO `pension_isa_account` (`id`, `member_id`, `has_pension_saving`, `has_irp`, `has_dc`, `has_isa`, `pension_saving_balance`, `irp_balance`, `pension_annual_payment`, `irp_annual_payment`, `dc_annual_payment`, `isa_annual_deposit`, `tax_eligibility_status`, `isa_eligibility_status`, `created_at`, `updated_at`) VALUES (79,401404,0,0,0,0,0,0,0,0,0,0,'ELIGIBLE','ELIGIBLE','2026-08-20 13:01:16','2026-08-20 13:01:16');

-- 궁합 점수
INSERT INTO `compatibility_result` (`id`, `couple_id`, `asset_stability_score`, `debt_repayment_score`, `financial_value_score`, `goal_feasibility_score`, `tax_strategy_score`, `tax_strategy_calculated`, `total_score`, `result_summary`, `created_at`, `updated_at`) VALUES (154,201017,6.99,17.14,19.35,20.00,0.00,1,63.00,NULL,'2026-08-20 13:01:27','2026-08-20 13:01:27');

-- 리포트
INSERT INTO `report` (`id`, `compatibility_result_id`, `asset_stability_reason`, `debt_repayment_reason`, `financial_value_reason`, `goal_feasibility_reason`, `expected_asset`, `tax_strategy_reason`, `expert_comment`, `created_at`, `updated_at`) VALUES (373,154,'금융 자산을 쌓기 위해 더 분발할 필요가 있어요. 금융 자산을 쌓는 걸 도와줄 상품들을 추천탭에서 만나보세요.','두 분 모두 부채가 있어요. 하지만 부채 위험도는 은영님이 더 높아요.','두 분은 가치관이 비슷해요.','180개월 후 목표 달성 가능성이 커요. 목표 달성을 더 확실하게 도와줄 상품을 추천탭에서 만나보세요.',1029595031,'정훈님은 ISA·IRP·연금저축 모두 개설 안 하셨어요. 은영님은 ISA·IRP·연금저축 모두 개설 안 하셨어요. 추천탭에서 상품들을 만나보세요.','**두 분의 목표 달성 가능성이 매우 탄탄해요.** 15년 뒤에는 목표의 103%에 해당하는 10억 2,959만원에 도달할 것으로 예상해요. 이 점에서 보잘것없는 2,959만원이라는 초과 성과가 생길 것 같아요. 부채 상환 부분에서도 잘하고 있어요. 정훈님과 은영님 각각 1,000만원의 부채를 가지고 있어서 이 부분도 관리가 잘 되고 있어요. 또한 두 분의 투자 가치관도 77%로 일치하고 있어서 함께 투자하는 데에 어려움이 없을 것으로 보여요.\n\n**절세 활용은 보완이 필요해요.** 현재 정훈님과 은영님 모두 ISA, IRP, 연금저축을 개설하지 않은 상태여서 절세의 기회를 활용하지 못하고 있어요. 이 부분에 조금 더 신경 쓰면 좋겠어요. 금융 자산 부분에 있어서는 현재 두 분 합산으로 8,000만원이지만 또래 커플 대비 47% 정도에 해당하니 더 늘려보는 것도 좋겠어요. 이 부분을 보완하면 앞으로 더 안정적인 금융 상황을 마련할 수 있을 것 같아요.','2026-08-20 13:01:27','2026-08-20 13:01:32');

-- 추천
INSERT INTO `recommendation` (`id`, `couple_id`, `created_at`, `updated_at`) VALUES (629,201017,'2026-08-20 13:01:32','2026-08-20 13:01:32');

-- 추천 슬롯
INSERT INTO `recommendation_slot` (`id`, `recommendation_id`, `slot_type`, `created_at`, `updated_at`) VALUES (2173,629,'INVESTMENT','2026-08-20 13:01:32','2026-08-20 13:01:32');
INSERT INTO `recommendation_slot` (`id`, `recommendation_id`, `slot_type`, `created_at`, `updated_at`) VALUES (2174,629,'DEPOSIT','2026-08-20 13:01:32','2026-08-20 13:01:32');
INSERT INTO `recommendation_slot` (`id`, `recommendation_id`, `slot_type`, `created_at`, `updated_at`) VALUES (2175,629,'SAVINGS','2026-08-20 13:01:32','2026-08-20 13:01:32');

-- 추천 상품
INSERT INTO `recommendation_product` (`id`, `recommendation_slot_id`, `product_id`, `rank_no`, `is_selected`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (8288,2173,21,1,1,'노후 목표와 투자성향에 맞는 TDF 상품을 우선 골랐어요.','2026-08-20 13:01:32','2026-08-20 13:01:32');
INSERT INTO `recommendation_product` (`id`, `recommendation_slot_id`, `product_id`, `rank_no`, `is_selected`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (8289,2173,20,2,0,NULL,'2026-08-20 13:01:32','2026-08-20 13:01:32');
INSERT INTO `recommendation_product` (`id`, `recommendation_slot_id`, `product_id`, `rank_no`, `is_selected`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (8290,2173,19,3,0,NULL,'2026-08-20 13:01:32','2026-08-20 13:01:32');
INSERT INTO `recommendation_product` (`id`, `recommendation_slot_id`, `product_id`, `rank_no`, `is_selected`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (8291,2173,18,4,0,NULL,'2026-08-20 13:01:32','2026-08-20 13:01:32');
INSERT INTO `recommendation_product` (`id`, `recommendation_slot_id`, `product_id`, `rank_no`, `is_selected`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (8292,2173,17,5,0,NULL,'2026-08-20 13:01:32','2026-08-20 13:01:32');
INSERT INTO `recommendation_product` (`id`, `recommendation_slot_id`, `product_id`, `rank_no`, `is_selected`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (8293,2173,15,6,0,NULL,'2026-08-20 13:01:32','2026-08-20 13:01:32');
INSERT INTO `recommendation_product` (`id`, `recommendation_slot_id`, `product_id`, `rank_no`, `is_selected`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (8294,2173,16,7,0,NULL,'2026-08-20 13:01:32','2026-08-20 13:01:32');
INSERT INTO `recommendation_product` (`id`, `recommendation_slot_id`, `product_id`, `rank_no`, `is_selected`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (8295,2173,14,8,0,NULL,'2026-08-20 13:01:32','2026-08-20 13:01:32');
INSERT INTO `recommendation_product` (`id`, `recommendation_slot_id`, `product_id`, `rank_no`, `is_selected`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (8296,2173,13,9,0,NULL,'2026-08-20 13:01:32','2026-08-20 13:01:32');
INSERT INTO `recommendation_product` (`id`, `recommendation_slot_id`, `product_id`, `rank_no`, `is_selected`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (8297,2174,2,1,1,'두 분의 목표 기간에 맞고 기본금리가 높은 예금 상품이에요.','2026-08-20 13:01:32','2026-08-20 13:01:32');
INSERT INTO `recommendation_product` (`id`, `recommendation_slot_id`, `product_id`, `rank_no`, `is_selected`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (8298,2174,1,2,0,NULL,'2026-08-20 13:01:32','2026-08-20 13:01:32');
INSERT INTO `recommendation_product` (`id`, `recommendation_slot_id`, `product_id`, `rank_no`, `is_selected`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (8299,2175,6,1,1,'목표 기간과 월 저축액에 맞고 기본금리가 높은 적금 상품이에요.','2026-08-20 13:01:32','2026-08-20 13:01:32');
INSERT INTO `recommendation_product` (`id`, `recommendation_slot_id`, `product_id`, `rank_no`, `is_selected`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (8300,2175,7,2,0,NULL,'2026-08-20 13:01:32','2026-08-20 13:01:32');
INSERT INTO `recommendation_product` (`id`, `recommendation_slot_id`, `product_id`, `rank_no`, `is_selected`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (8301,2175,4,3,0,NULL,'2026-08-20 13:01:32','2026-08-20 13:01:32');
INSERT INTO `recommendation_product` (`id`, `recommendation_slot_id`, `product_id`, `rank_no`, `is_selected`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (8302,2175,3,4,0,NULL,'2026-08-20 13:01:32','2026-08-20 13:01:32');

-- 개인 투자 추천 (박정훈만 — 최은영은 개인 투자 추천 대상 아님)
INSERT INTO `personal_recommendation_investment` (`id`, `member_id`, `product_id`, `rank_no`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (5922,401403,21,1,'노후 목표와 투자성향에 맞는 TDF 상품을 우선 골랐어요.','2026-08-20 13:01:32','2026-08-20 13:01:32');
INSERT INTO `personal_recommendation_investment` (`id`, `member_id`, `product_id`, `rank_no`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (5923,401403,20,2,NULL,'2026-08-20 13:01:32','2026-08-20 13:01:32');
INSERT INTO `personal_recommendation_investment` (`id`, `member_id`, `product_id`, `rank_no`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (5924,401403,19,3,NULL,'2026-08-20 13:01:32','2026-08-20 13:01:32');
INSERT INTO `personal_recommendation_investment` (`id`, `member_id`, `product_id`, `rank_no`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (5925,401403,22,4,NULL,'2026-08-20 13:01:32','2026-08-20 13:01:32');
INSERT INTO `personal_recommendation_investment` (`id`, `member_id`, `product_id`, `rank_no`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (5926,401403,23,5,NULL,'2026-08-20 13:01:32','2026-08-20 13:01:32');
INSERT INTO `personal_recommendation_investment` (`id`, `member_id`, `product_id`, `rank_no`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (5927,401403,18,6,NULL,'2026-08-20 13:01:32','2026-08-20 13:01:32');
INSERT INTO `personal_recommendation_investment` (`id`, `member_id`, `product_id`, `rank_no`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (5928,401403,17,7,NULL,'2026-08-20 13:01:32','2026-08-20 13:01:32');
INSERT INTO `personal_recommendation_investment` (`id`, `member_id`, `product_id`, `rank_no`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (5929,401403,15,8,NULL,'2026-08-20 13:01:32','2026-08-20 13:01:32');
INSERT INTO `personal_recommendation_investment` (`id`, `member_id`, `product_id`, `rank_no`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (5930,401403,16,9,NULL,'2026-08-20 13:01:32','2026-08-20 13:01:32');
INSERT INTO `personal_recommendation_investment` (`id`, `member_id`, `product_id`, `rank_no`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (5931,401403,14,10,NULL,'2026-08-20 13:01:32','2026-08-20 13:01:32');
INSERT INTO `personal_recommendation_investment` (`id`, `member_id`, `product_id`, `rank_no`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (5932,401403,13,11,NULL,'2026-08-20 13:01:32','2026-08-20 13:01:32');

-- 개인 절세 추천
INSERT INTO `personal_recommendation_tax_saving` (`id`, `member_id`, `product_id`, `rank_no`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (100,401404,10,1,'노후 목표와 계좌 현황에 잘 맞는 연금저축 상품을 골랐어요.','2026-08-20 13:01:32','2026-08-20 13:01:32');
INSERT INTO `personal_recommendation_tax_saving` (`id`, `member_id`, `product_id`, `rank_no`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (101,401404,11,2,NULL,'2026-08-20 13:01:32','2026-08-20 13:01:32');
INSERT INTO `personal_recommendation_tax_saving` (`id`, `member_id`, `product_id`, `rank_no`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (102,401404,12,3,NULL,'2026-08-20 13:01:32','2026-08-20 13:01:32');
INSERT INTO `personal_recommendation_tax_saving` (`id`, `member_id`, `product_id`, `rank_no`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (103,401404,9,4,NULL,'2026-08-20 13:01:32','2026-08-20 13:01:32');
INSERT INTO `personal_recommendation_tax_saving` (`id`, `member_id`, `product_id`, `rank_no`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (104,401404,8,5,NULL,'2026-08-20 13:01:32','2026-08-20 13:01:32');
INSERT INTO `personal_recommendation_tax_saving` (`id`, `member_id`, `product_id`, `rank_no`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (105,401403,10,1,'노후 목표와 계좌 현황에 잘 맞는 연금저축 상품을 골랐어요.','2026-08-20 13:01:32','2026-08-20 13:01:32');
INSERT INTO `personal_recommendation_tax_saving` (`id`, `member_id`, `product_id`, `rank_no`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (106,401403,11,2,NULL,'2026-08-20 13:01:32','2026-08-20 13:01:32');
INSERT INTO `personal_recommendation_tax_saving` (`id`, `member_id`, `product_id`, `rank_no`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (107,401403,12,3,NULL,'2026-08-20 13:01:32','2026-08-20 13:01:32');
INSERT INTO `personal_recommendation_tax_saving` (`id`, `member_id`, `product_id`, `rank_no`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (108,401403,8,4,NULL,'2026-08-20 13:01:32','2026-08-20 13:01:32');
INSERT INTO `personal_recommendation_tax_saving` (`id`, `member_id`, `product_id`, `rank_no`, `recommendation_reason`, `created_at`, `updated_at`) VALUES (109,401403,9,5,NULL,'2026-08-20 13:01:32','2026-08-20 13:01:32');
