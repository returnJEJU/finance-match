-- =====================================================================
-- 찰떡궁합 — seed-demo-couples.sql (데모 커플 3쌍용 회원 6명 · 운영 X)
-- 실행: R__01_seed_product → R__02_seed_demo 다음.
-- 내용: 회원 6명(3~8번) + 약관동의. 그 외에는 아무것도 넣지 않는다.
-- =====================================================================
-- 【 왜 회원만 넣나 】
--   자산·설문·궁합점수·리포트·추천은 손으로 적지 않고 앱이 실제로 만들게 한다.
--   손으로 적은 결과는 계산기·추천정책이 내놓는 답과 어긋나기 쉽고, 어긋나는 순간
--   "정답지"로서의 값어치가 사라진다. 여기서는 입력의 출발점만 준비한다.
--
--   · 자산: 로그인 후 자산연동을 누르면 SimpleMyDataProvider 가 이메일로 시나리오를 찾아 채운다.
--   · 투자성향(member.investment_type): 개인설문을 저장할 때 계산기가 채운다. 여기서 NULL 로 둔다.
--   · 궁합점수·리포트: 두 사람이 개인설문을 끝내면 자동 생성된다.
--   · 추천: 추천 화면에 들어가면 프론트가 생성 요청을 보낸다.
-- =====================================================================
-- 【 인물 】 비밀번호는 모두 Test1234! · 나이는 2026-08-12 기준
--   3 김보람   demo.saver@     33세 여 · 자산 1.2억 · 전세대출 6,000만 · KB스타적금 가입가능
--   4 박준서   demo.investor@  34세 남 · 자산 1.23억 · 신용대출 2,000만
--   5 최유진   demo.newlywed@  31세 여 · 자산 350만 · 전세대출 1.2억
--   6 정민호   demo.renter@    32세 남 · 자산 240만 · 신용+학자금 5,700만(2건)
--   7 한서연   demo.rich@      38세 여 · 자산 3억 · 무부채(8명 중 유일)
--   8 오태윤   demo.debt@      30세 남 · 자산 250만 · 카드론 1,500만 17.9% 고금리 · KB스타적금 가입가능
--
--   기본 조합(커플은 앱에서 초대코드로 연결한다. 다른 조합으로 바꿔도 자산은 사람을 따라간다)
--     커플1 = 3 + 4  둘 다 안정 · 성향 반대
--     커플2 = 5 + 6  둘 다 불안정 · 성향 일치
--     커플3 = 7 + 8  극과 극
-- =====================================================================
-- ⚠️ 재실행하면 3~8번 회원의 설문·커플연결·궁합점수·리포트·추천이 전부 지워진다.
--    앱에서 입력해 둔 내용이 날아가므로, 이 파일을 고칠 때는 그 점을 감안할 것.
--    (리셋이 필요할 때 일부러 쓰는 용도이기도 하다)
--    기존 데모 커플(1 김하나 · 2 이두리)은 건드리지 않는다.
-- =====================================================================
SET NAMES utf8mb4;

-- ─── 초기화 (3~8번 회원에 딸린 것만. 자식 → 부모 순서) ───
DELETE FROM recommendation_product
 WHERE recommendation_slot_id IN (
   SELECT id FROM recommendation_slot WHERE recommendation_id IN (
     SELECT id FROM recommendation WHERE couple_id IN (
       SELECT id FROM couple
        WHERE inviter_id IN (3,4,5,6,7,8) OR invitee_id IN (3,4,5,6,7,8))));

DELETE FROM recommendation_slot
 WHERE recommendation_id IN (
   SELECT id FROM recommendation WHERE couple_id IN (
     SELECT id FROM couple
      WHERE inviter_id IN (3,4,5,6,7,8) OR invitee_id IN (3,4,5,6,7,8)));

DELETE FROM recommendation
 WHERE couple_id IN (
   SELECT id FROM couple
    WHERE inviter_id IN (3,4,5,6,7,8) OR invitee_id IN (3,4,5,6,7,8));

DELETE FROM report
 WHERE compatibility_result_id IN (
   SELECT id FROM compatibility_result WHERE couple_id IN (
     SELECT id FROM couple
      WHERE inviter_id IN (3,4,5,6,7,8) OR invitee_id IN (3,4,5,6,7,8)));

DELETE FROM compatibility_result
 WHERE couple_id IN (
   SELECT id FROM couple
    WHERE inviter_id IN (3,4,5,6,7,8) OR invitee_id IN (3,4,5,6,7,8));

DELETE FROM personal_recommendation_tax_saving  WHERE member_id IN (3,4,5,6,7,8);
DELETE FROM personal_recommendation_investment  WHERE member_id IN (3,4,5,6,7,8);
DELETE FROM favorite_product                    WHERE member_id IN (3,4,5,6,7,8);
DELETE FROM pension_isa_account                 WHERE member_id IN (3,4,5,6,7,8);
DELETE FROM financial_summary                   WHERE member_id IN (3,4,5,6,7,8);

DELETE FROM personal_survey_investment_experience
 WHERE personal_survey_id IN (SELECT id FROM personal_survey WHERE member_id IN (3,4,5,6,7,8));
DELETE FROM personal_survey WHERE member_id IN (3,4,5,6,7,8);

-- couple 이 invitation_code 를 참조하므로 couple 부터 지운다.
DELETE FROM couple WHERE inviter_id IN (3,4,5,6,7,8) OR invitee_id IN (3,4,5,6,7,8);
DELETE FROM invitation_code
 WHERE common_survey_id IN (SELECT id FROM common_survey WHERE member_id IN (3,4,5,6,7,8));
DELETE FROM common_survey   WHERE member_id IN (3,4,5,6,7,8);

DELETE FROM member_agreement WHERE member_id IN (3,4,5,6,7,8);
DELETE FROM member           WHERE id        IN (3,4,5,6,7,8);

-- ─── 회원 (investment_type 은 개인설문이 채우므로 NULL) ───
--   kb_star_savings_eligible: 가입에서 입력받지 않는 값이라 여기서 직접 넣는다.
--   김보람·오태윤만 1 → 두 사람이 낀 커플에서만 KB스타적금Ⅲ 가 적금 1등으로 올라온다.
INSERT INTO member
 (id,email,password,name,gender,birth_date,status,investment_type,kb_star_savings_eligible) VALUES
(3,'demo.saver@chaltteok.dev',   '$2b$10$S7kxCvVErKro6iMGGYtqieadaYQh/mlP/3AOB6q8G81Dl2MoyJeBG','김보람','F','1993-04-10','ACTIVE',NULL,1),
(4,'demo.investor@chaltteok.dev','$2b$10$S7kxCvVErKro6iMGGYtqieadaYQh/mlP/3AOB6q8G81Dl2MoyJeBG','박준서','M','1992-06-22','ACTIVE',NULL,0),
(5,'demo.newlywed@chaltteok.dev','$2b$10$S7kxCvVErKro6iMGGYtqieadaYQh/mlP/3AOB6q8G81Dl2MoyJeBG','최유진','F','1995-02-14','ACTIVE',NULL,0),
(6,'demo.renter@chaltteok.dev',  '$2b$10$S7kxCvVErKro6iMGGYtqieadaYQh/mlP/3AOB6q8G81Dl2MoyJeBG','정민호','M','1994-03-03','ACTIVE',NULL,0),
(7,'demo.rich@chaltteok.dev',    '$2b$10$S7kxCvVErKro6iMGGYtqieadaYQh/mlP/3AOB6q8G81Dl2MoyJeBG','한서연','F','1988-01-25','ACTIVE',NULL,0),
(8,'demo.debt@chaltteok.dev',    '$2b$10$S7kxCvVErKro6iMGGYtqieadaYQh/mlP/3AOB6q8G81Dl2MoyJeBG','오태윤','M','1996-05-30','ACTIVE',NULL,1);

-- ─── 약관 동의 (가입 완료 상태를 만든다. 마케팅만 사람마다 다르게) ───
INSERT INTO member_agreement
 (member_id,agree_mydata_terms,agree_privacy,agree_asset_link,agree_couple_share,agree_marketing,agreed_at) VALUES
(3,1,1,1,1,1,'2026-08-12 09:00:00'),
(4,1,1,1,1,0,'2026-08-12 09:05:00'),
(5,1,1,1,1,1,'2026-08-12 09:10:00'),
(6,1,1,1,1,0,'2026-08-12 09:15:00'),
(7,1,1,1,1,0,'2026-08-12 09:20:00'),
(8,1,1,1,1,1,'2026-08-12 09:25:00');
