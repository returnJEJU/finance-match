-- =====================================================================
-- 찰떡궁합 — seed-product.sql (KB 상품 조사 반영 · 3차: schema_v2 enum 정렬)
-- 실행: 01-schema.sql 다음. 운영·개발 공통.
-- 반영: 투자 순자산=억 INT · 예적금 금리 · 상품 URL · 대출 11개 · 구간금리(임시)
--       enum=schema_v2 영문코드(product_type 적금도 DEPOSIT / deposit.type DEPOSIT·SAVINGS
--       / tax PENSION_SAVINGS·BROKERAGE·DISCRETIONARY / loan target_group·channel) · age중앙값 포함
-- ⚠️ deposit_rate = 임시 목데이터(실데이터는 finlife API 적재). loan = 현 테이블에 담기는 subset만.
-- =====================================================================
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS=0;
DELETE FROM deposit_rate;
DELETE FROM deposit;
DELETE FROM tax_saving;
DELETE FROM investment;
DELETE FROM loan;
DELETE FROM product;
DELETE FROM age_group_asset_median;
SET FOREIGN_KEY_CHECKS=1;

INSERT INTO product (id, product_type, product_name, description, url, company_name) VALUES
(1, 'DEPOSIT', 'KB Star 정기예금', '1~36개월 기본 정기예금', 'https://obank.kbstar.com/quics?page=C016613', 'KB국민은행'),
(2, 'DEPOSIT', 'KB e-plus 정기예금', '온라인 전용 정기예금', 'https://m.kiwimbank.com/mobweb/kiwisavings/eplusSavingsProd.do', 'KB저축은행'),
(3, 'DEPOSIT', 'KB 특★한 적금', '만기일 지정 단기 자유적금', 'https://obank.kbstar.com/quics?page=C016613', 'KB국민은행'),
(4, 'DEPOSIT', 'KB나만의 적금', '3~12개월 맞춤 자유적금', 'https://obank.kbstar.com/quics?page=C016613', 'KB국민은행'),
(5, 'DEPOSIT', 'KB스타적금Ⅲ', '신규·장기미거래 우대적금', 'https://obank.kbstar.com/quics?page=C016613', 'KB국민은행'),
(6, 'DEPOSIT', 'KB착한 e-plus정기적금', '중장기 정액 저축', 'https://m.kiwimbank.com/mobweb/kiwisavings/kindEplusSavingsProd.do', 'KB저축은행'),
(7, 'DEPOSIT', 'KB일반 e-plus정기적금', '월 100만원 초과 적립용', 'https://m.kiwimbank.com/mobweb/kiwisavings/installmentSavingsProd.do', 'KB저축은행'),
(8, 'TAX_SAVING', 'KB증권 중개형 ISA', '직접운용 ISA', 'https://www.kbsec.com/go.able?linkcd=m05080009', 'KB증권'),
(9, 'TAX_SAVING', 'KB국민은행 일임형 ISA', '일임형 ISA', 'https://obank.kbstar.com/quics?page=C041171', 'KB국민은행'),
(10, 'TAX_SAVING', 'KB증권 개인연금저축', '연금저축계좌', 'https://www.kbsec.com/go.able?linkcd=m01010030', 'KB증권'),
(11, 'TAX_SAVING', 'KB증권 IRP', 'ETF 중심 IRP', 'https://www.kbsec.com/go.able?linkcd=m01010029', 'KB증권'),
(12, 'TAX_SAVING', 'KB국민은행 IRP', '원리금보장·디폴트 IRP', 'https://okbfex.kbstar.com/quics?page=C019570', 'KB국민은행'),
(13, 'INVESTMENT', 'RISE 단기국공채액티브', 'RISE 단기국공채액티브', 'https://www.kbam.co.kr', 'KB자산운용'),
(14, 'INVESTMENT', 'RISE 단기통안채', 'RISE 단기통안채', 'https://www.kbam.co.kr', 'KB자산운용'),
(15, 'INVESTMENT', 'RISE 머니마켓액티브', 'RISE 머니마켓액티브', 'https://www.kbam.co.kr', 'KB자산운용'),
(16, 'INVESTMENT', 'RISE 종합채권(A-이상)액티브', 'RISE 종합채권(A-이상)액티브', 'https://www.kbam.co.kr', 'KB자산운용'),
(17, 'INVESTMENT', 'RISE 200채권혼합50', 'RISE 200채권혼합50', 'https://www.kbam.co.kr', 'KB자산운용'),
(18, 'INVESTMENT', 'RISE 글로벌자산배분액티브', 'RISE 글로벌자산배분액티브', 'https://www.kbam.co.kr', 'KB자산운용'),
(19, 'INVESTMENT', 'RISE TDF2030액티브 적격', 'RISE TDF2030액티브 적격', 'https://www.kbam.co.kr', 'KB자산운용'),
(20, 'INVESTMENT', 'RISE TDF2040액티브 적격', 'RISE TDF2040액티브 적격', 'https://www.kbam.co.kr', 'KB자산운용'),
(21, 'INVESTMENT', 'RISE TDF2050액티브 적격', 'RISE TDF2050액티브 적격', 'https://www.kbam.co.kr', 'KB자산운용'),
(22, 'INVESTMENT', 'RISE KIS국고채30년Enhanced', 'RISE KIS국고채30년Enhanced', 'https://www.kbam.co.kr', 'KB자산운용'),
(23, 'INVESTMENT', 'RISE 주식혼합', 'RISE 주식혼합', 'https://www.kbam.co.kr', 'KB자산운용'),
(24, 'INVESTMENT', 'RISE 200', 'RISE 200', 'https://www.kbam.co.kr', 'KB자산운용'),
(25, 'INVESTMENT', 'RISE 미국S&P500', 'RISE 미국S&P500', 'https://www.kbam.co.kr', 'KB자산운용'),
(26, 'INVESTMENT', 'RISE 미국AI밸류체인TOP3Plus', 'RISE 미국AI밸류체인TOP3Plus', 'https://www.kbam.co.kr', 'KB자산운용'),
(27, 'INVESTMENT', 'RISE 2차전지액티브', 'RISE 2차전지액티브', 'https://www.kbam.co.kr', 'KB자산운용'),
(28, 'LOAN', '청년전용 버팀목전세자금대출', 'JEONSE 대출(POLICY_YOUTH)', 'https://obank.kbstar.com/quics?page=C016613', 'KB국민은행'),
(29, 'LOAN', 'KB스타 전세자금대출(HF)', 'JEONSE 대출(HF)', 'https://obank.kbstar.com/quics?page=C016613', 'KB국민은행'),
(30, 'LOAN', 'KB스타 전세자금대출(HUG)', 'JEONSE 대출(HUG)', 'https://obank.kbstar.com/quics?page=C016613', 'KB국민은행'),
(31, 'LOAN', 'KB스타 전세자금대출(SGI)', 'JEONSE 대출(SGI)', 'https://obank.kbstar.com/quics?page=C016613', 'KB국민은행'),
(32, 'LOAN', 'KB 신혼부부 전세자금대출', 'JEONSE 대출(NEWLYWED)', 'https://obank.kbstar.com/quics?page=C016613', 'KB국민은행'),
(33, 'LOAN', 'KB 주택담보대출', 'HOUSING 대출(GENERAL_MORTGAGE)', 'https://obank.kbstar.com/quics?page=C016613', 'KB국민은행'),
(34, 'LOAN', 'KB스타 아파트담보대출(주택자금)', 'HOUSING 대출(APT_MORTGAGE)', 'https://obank.kbstar.com/quics?page=C016613', 'KB국민은행'),
(35, 'LOAN', 'KB 매직카대출(신차 구매용)', 'CAR 대출(NEW_CAR)', 'https://obank.kbstar.com/quics?page=C016613', 'KB국민은행'),
(36, 'LOAN', 'KB 친환경 매직카대출(신차 구매용)', 'CAR 대출(ECO_NEW_CAR)', 'https://obank.kbstar.com/quics?page=C016613', 'KB국민은행'),
(37, 'LOAN', 'KB 매직카대출(중고차 구매용)', 'CAR 대출(USED_CAR)', 'https://obank.kbstar.com/quics?page=C016613', 'KB국민은행'),
(38, 'LOAN', '소상공인시장진흥기금(정책자금)', 'BUSINESS 대출(POLICY_SMALL_BUSINESS)', 'https://obank.kbstar.com/quics?page=C016613', '소상공인시장진흥공단');

-- deposit (예적금)
INSERT INTO deposit (id,type,min_term,max_term,saving_min,saving_max,min_rate,max_rate) VALUES
(1, 'DEPOSIT', 1, 36, NULL, NULL, 1.8, 2.2),
(2, 'DEPOSIT', 3, 36, NULL, NULL, 2.4, 3.6),
(3, 'SAVINGS', 1, 6, 1000, 300000, 2.0, 6.0),
(4, 'SAVINGS', 3, 12, 10000, 1000000, 1.0, 4.0),
(5, 'SAVINGS', 12, 12, 10000, 300000, 2.0, 5.0),
(6, 'SAVINGS', 12, 36, NULL, 1000000, 3.7, 3.8),
(7, 'SAVINGS', 12, 36, 10000, NULL, 2.3, 3.4);

-- deposit_rate (구간별 금리) · 상품1·2·7=조사 이미지 실데이터, 3·4·5·6=임시(finlife 예정)
INSERT INTO deposit_rate (deposit_id,min_term,max_term,base_rate) VALUES
(1, 1, 2, 1.8),
(1, 3, 5, 2.0),
(1, 6, 8, 2.1),
(1, 9, 11, 2.1),
(1, 12, 23, 2.15),
(1, 24, 35, 2.2),
(1, 36, NULL, 2.2),
(2, 3, 5, 2.3),
(2, 6, 11, 2.4),
(2, 12, 23, 3.6),
(2, 24, 35, 2.6),
(2, 36, NULL, 2.5),
(7, 12, 23, 3.1),
(7, 24, 35, 3.2),
(7, 36, NULL, 3.3),
(3, 1, 3, 2.0),
(3, 4, 6, 4.0),
(4, 3, 6, 1.0),
(4, 7, 12, 4.0),
(5, 12, NULL, 3.5),
(6, 12, 23, 3.7),
(6, 24, NULL, 3.8);

-- tax_saving (절세)
INSERT INTO tax_saving (id,account_type,isa_type) VALUES
(8, 'ISA', 'BROKERAGE'),
(9, 'ISA', 'DISCRETIONARY'),
(10, 'PENSION_SAVINGS', NULL),
(11, 'IRP', NULL),
(12, 'IRP', NULL);

-- investment (aum: 억 단위 INT · schema_v2 기준. 원=aum×1억)
INSERT INTO investment (id,risk_level,aum,is_tdf) VALUES
(13, 6, 1564, 0),
(14, 6, 2781, 0),
(15, 5, 28049, 0),
(16, 5, 20975, 0),
(17, 4, 401, 0),
(18, 4, 2893, 0),
(19, 4, 216, 1),
(20, 4, 478, 1),
(21, 4, 3144, 1),
(22, 3, 8085, 0),
(23, 3, 185, 0),
(24, 2, 51, 0),
(25, 2, 15545, 0),
(26, 1, 946, 0),
(27, 1, 1825, 0);

-- loan (현 테이블에 맞는 필요 정보만. loan_purpose=HOUSING/CAR 통일. 조사의 subtype·대표·상태 등은 미포함)
INSERT INTO loan (id,loan_purpose,target_group,min_age,max_age,income_basis,max_income,application_channel,max_rate) VALUES
(28, 'JEONSE', 'OTHER', 19, 34, 'COUPLE', 50000000, 'BRANCH', 3.3),
(29, 'JEONSE', 'GENERAL', 19, NULL, NULL, NULL, 'MOBILE', 5.05),
(30, 'JEONSE', 'GENERAL', 19, NULL, NULL, NULL, 'MOBILE', 5.11),
(31, 'JEONSE', 'GENERAL', 19, NULL, NULL, NULL, 'MOBILE', 5.41),
(32, 'JEONSE', 'NEWLYWED', 19, NULL, NULL, NULL, 'BRANCH', 5.48),
(33, 'HOUSING', 'GENERAL', 19, NULL, NULL, NULL, 'BRANCH', 6.52),
(34, 'HOUSING', 'GENERAL', 19, NULL, NULL, NULL, 'MOBILE', 5.51),
(35, 'CAR', 'GENERAL', 19, NULL, NULL, NULL, 'MOBILE', 6.88),
(36, 'CAR', 'GENERAL', 19, NULL, NULL, NULL, 'MOBILE', 6.88),
(37, 'CAR', 'GENERAL', 19, NULL, NULL, NULL, 'MOBILE', 7.89),
(38, 'BUSINESS', 'GENERAL', 19, NULL, NULL, NULL, 'MOBILE', 4.04);

-- age_group_asset_median (연령대별 금융자산 중앙값 · KOSIS 가계금융복지조사 2025 · 만원×10000=원)
INSERT INTO age_group_asset_median (age_min,age_max,median_financial_asset,base_year) VALUES
(0,  29,  52450000, 2025),
(30, 39,  93300000, 2025),
(40, 49,  84600000, 2025),
(50, 59,  81000000, 2025),
(60, NULL,41500000, 2025);
