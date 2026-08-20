-- =====================================================================
-- perf-seed.sql 로 넣은 데이터를 전부 지운다.
--   perf-seed.sql이 만든 회원은 전부 email이 @loadtest.local 로 끝난다 —
--   그 조건 하나로 대상을 찾아서, 자식 → 부모 순서로 삭제한다.
--
-- 실행:
--   docker exec -i fm-mysql sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" finance_match' \
--     < backend/scripts/perf-seed-cleanup.sql
-- =====================================================================

DELETE FROM report
 WHERE compatibility_result_id IN (
   SELECT id FROM compatibility_result WHERE couple_id IN (
     SELECT id FROM couple WHERE inviter_id IN (
       SELECT id FROM member WHERE email LIKE '%@loadtest.local')));

DELETE FROM compatibility_result
 WHERE couple_id IN (
   SELECT id FROM couple WHERE inviter_id IN (
     SELECT id FROM member WHERE email LIKE '%@loadtest.local'));

DELETE FROM financial_summary
 WHERE member_id IN (SELECT id FROM member WHERE email LIKE '%@loadtest.local');

DELETE FROM pension_isa_account
 WHERE member_id IN (SELECT id FROM member WHERE email LIKE '%@loadtest.local');

DELETE FROM couple
 WHERE inviter_id IN (SELECT id FROM member WHERE email LIKE '%@loadtest.local');

DELETE FROM invitation_code
 WHERE common_survey_id IN (
   SELECT id FROM common_survey WHERE member_id IN (
     SELECT id FROM member WHERE email LIKE '%@loadtest.local'));

DELETE FROM common_survey
 WHERE member_id IN (SELECT id FROM member WHERE email LIKE '%@loadtest.local');

DELETE FROM personal_survey_investment_experience
 WHERE personal_survey_id IN (
   SELECT id FROM personal_survey WHERE member_id IN (
     SELECT id FROM member WHERE email LIKE '%@loadtest.local'));

DELETE FROM personal_survey
 WHERE member_id IN (SELECT id FROM member WHERE email LIKE '%@loadtest.local');

DELETE FROM member_agreement
 WHERE member_id IN (SELECT id FROM member WHERE email LIKE '%@loadtest.local');

DELETE FROM member WHERE email LIKE '%@loadtest.local';

SELECT ROW_COUNT() AS deleted_last_statement;
