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
(1, 'DEPOSIT', 'KB Star 정기예금', '1~36개월 가입 가능한 기본형 정기예금', 'https://obank.kbstar.com/quics?page=C016613&cc=b061496:b061645&isNew=N&prcode=DP01000938', 'KB국민은행'),
(2, 'DEPOSIT', 'KB e-plus 정기예금', '비대면으로 가입하는 온라인 전용 정기예금', 'https://m.kiwimbank.com/mobweb/kiwisavings/eplusSavingsProd.do', 'KB저축은행'),
(3, 'DEPOSIT', 'KB 특★한 적금', '만기일을 자유롭게 정하는 단기 자유적립식 적금', 'https://obank.kbstar.com/quics?page=C016613&cc=b061496:b061645&isNew=N&prcode=DP01001566', 'KB국민은행'),
(4, 'DEPOSIT', 'KB나만의 적금', '기간과 납입방식을 맞춤 설정할 수 있는 자유적금', 'https://obank.kbstar.com/quics?page=C016613&cc=b061496:b061645&isNew=N&prcode=DP01001632', 'KB국민은행'),
(5, 'DEPOSIT', 'KB스타적금Ⅲ', '신규·장기 미거래 고객에게 우대금리를 제공하는 적금', 'https://obank.kbstar.com/quics?page=C016613&cc=b061496:b061645&isNew=N&prcode=DP01001614', 'KB국민은행'),
(6, 'DEPOSIT', 'KB착한 e-plus정기적금', '온라인 가입이 가능한 중장기 정액 적립식 적금(최대 100만원 이하)', 'https://m.kiwimbank.com/mobweb/kiwisavings/kindEplusSavingsProd.do', 'KB저축은행'),
(7, 'DEPOSIT', 'KB일반 e-plus정기적금', '매월 일정 금액을 납입하는 온라인 정기적금(제한 없음)', 'https://m.kiwimbank.com/mobweb/kiwisavings/installmentSavingsProd.do', 'KB저축은행'),
(8, 'TAX_SAVING', 'KB증권 중개형 ISA', '국내 주식과 ETF까지 투자 가능한 절세 종합계좌', 'https://www.kbsec.com/go.able?linkcd=m05080009', 'KB증권'),
(9, 'TAX_SAVING', 'KB국민은행 일임형 ISA', '전문가가 자산을 운용해주는 절세형 ISA 계좌', 'https://obank.kbstar.com/quics?page=C041164', 'KB국민은행'),
(10, 'TAX_SAVING', 'KB증권 개인연금저축', '노후 준비와 세액공제를 동시에 받을 수 있는 연금계좌', 'https://www.kbsec.com/go.able?linkcd=m01010030', 'KB증권'),
(11, 'TAX_SAVING', 'KB증권 IRP', '퇴직금과 추가 납입금을 운용하는 개인형 퇴직연금', 'https://www.kbsec.com/go.able?linkcd=m01010029', 'KB증권'),
(12, 'TAX_SAVING', 'KB국민은행 IRP', '안정적인 노후자금 마련을 위한 개인형 퇴직연금', 'https://okbfex.kbstar.com/quics?page=C019570', 'KB국민은행'),
(13, 'INVESTMENT', 'RISE 단기국공채액티브', '단기 국공채에 투자해 안정적인 수익을 추구하는 ETF', 'https://www.riseetf.co.kr/prod/finderDetail/4460', 'KB자산운용'),
(14, 'INVESTMENT', 'RISE 단기통안채', '단기 통화안정증권에 투자하는 저위험 채권 ETF', 'https://www.riseetf.co.kr/prod/finderDetail/4441', 'KB자산운용'),
(15, 'INVESTMENT', 'RISE 머니마켓액티브', '초단기 금융상품에 투자하는 현금성 자산 ETF', 'https://www.riseetf.co.kr/prod/finderDetail/44E4', 'KB자산운용'),
(16, 'INVESTMENT', 'RISE 종합채권(A-이상)액티브', '우량 회사채와 국채에 분산 투자하는 채권 ETF', 'https://www.riseetf.co.kr/prod/finderDetail/44B5', 'KB자산운용'),
(17, 'INVESTMENT', 'RISE 200채권혼합50', '국내 주식과 채권을 5:5로 투자하는 혼합형 ETF', 'https://www.riseetf.co.kr/prod/finderDetail/4440', 'KB자산운용'),
(18, 'INVESTMENT', 'RISE 글로벌자산배분액티브', '글로벌 주식과 채권에 분산 투자하는 자산배분 ETF', 'https://www.riseetf.co.kr/prod/finderDetail/44E8', 'KB자산운용'),
(19, 'INVESTMENT', 'RISE TDF2030액티브 적격', '2030년 은퇴 목표에 맞춰 자산을 자동 조정하는 TDF', 'https://www.riseetf.co.kr/prod/finderDetail/44D3', 'KB자산운용'),
(20, 'INVESTMENT', 'RISE TDF2040액티브 적격', '2040년 은퇴 목표에 맞춰 자산을 자동 조정하는 TDF', 'https://www.riseetf.co.kr/prod/finderDetail/44D4', 'KB자산운용'),
(21, 'INVESTMENT', 'RISE TDF2050액티브 적격', '2050년 은퇴 목표에 맞춰 자산을 자동 조정하는 TDF', 'https://www.riseetf.co.kr/prod/finderDetail/44D5', 'KB자산운용'),
(22, 'INVESTMENT', 'RISE KIS국고채30년Enhanced', '장기 국고채에 투자해 금리 변동 수익을 추구하는 ETF', 'https://www.riseetf.co.kr/prod/finderDetail/44B7', 'KB자산운용'),
(23, 'INVESTMENT', 'RISE 주식혼합', '국내 주식과 채권을 함께 투자하는 혼합형 ETF', 'https://www.riseetf.co.kr/prod/finderDetail/4439', 'KB자산운용'),
(24, 'INVESTMENT', 'RISE 200', 'KOSPI200 지수를 추종하는 대표 국내 주식 ETF', 'https://www.riseetf.co.kr/prod/finderDetail/4435', 'KB자산운용'),
(25, 'INVESTMENT', 'RISE 미국S&P500', '미국 S&P500 지수를 추종하는 해외 주식 ETF', 'https://www.riseetf.co.kr/prod/finderDetail/44B3', 'KB자산운용'),
(26, 'INVESTMENT', 'RISE 미국AI밸류체인TOP3Plus', '미국 AI 산업 핵심 기업에 집중 투자하는 ETF', 'https://www.riseetf.co.kr/prod/finderDetail/44G9', 'KB자산운용'),
(27, 'INVESTMENT', 'RISE 2차전지액티브', '국내외 2차전지 산업에 적극 투자하는 액티브 ETF', 'https://www.riseetf.co.kr/prod/finderDetail/44C7', 'KB자산운용'),
(28, 'LOAN', '청년전용 버팀목전세자금대출', '청년을 위한 저금리 정부 지원 전세자금대출', 'https://obank.kbstar.com/quics?page=C103998&cc=b104363:b104516&%EB%B8%8C%EB%9E%9C%EB%93%9C%EC%83%81%ED%92%88%EC%BD%94%EB%93%9C=LN02000003&%EB%85%B8%EB%93%9C%EC%BD%94%EB%93%9C=00019&prcode=LN02000003', 'KB국민은행'),
(29, 'LOAN', 'KB스타 전세자금대출(HF)', '한국주택금융공사 보증을 활용한 전세자금대출', 'https://obank.kbstar.com/quics?page=C103507&cc=b104363:b104516&%EB%B8%8C%EB%9E%9C%EB%93%9C%EC%83%81%ED%92%88%EC%BD%94%EB%93%9C=LN20001362&%EB%85%B8%EB%93%9C%EC%BD%94%EB%93%9C=00019&prcode=LN20001362', 'KB국민은행'),
(30, 'LOAN', 'KB스타 전세자금대출(HUG)', '주택도시보증공사 보증을 활용한 전세자금대출', 'https://obank.kbstar.com/quics?page=C103507&cc=b104363:b104516&%EB%B8%8C%EB%9E%9C%EB%93%9C%EC%83%81%ED%92%88%EC%BD%94%EB%93%9C=LN20001364&%EB%85%B8%EB%93%9C%EC%BD%94%EB%93%9C=00019&prcode=LN20001364', 'KB국민은행'),
(31, 'LOAN', 'KB스타 전세자금대출(SGI)', 'SGI서울보증 보증을 활용한 전세자금대출', 'https://obank.kbstar.com/quics?page=C103507&cc=b104363:b104516&%EB%B8%8C%EB%9E%9C%EB%93%9C%EC%83%81%ED%92%88%EC%BD%94%EB%93%9C=LN20001363&%EB%85%B8%EB%93%9C%EC%BD%94%EB%93%9C=00019&prcode=LN20001363', 'KB국민은행'),
(32, 'LOAN', 'KB 신혼부부 전세자금대출', '신혼부부를 위한 우대조건의 전세자금대출', 'https://obank.kbstar.com/quics?page=C103507&cc=b104363:b104516&%EB%B8%8C%EB%9E%9C%EB%93%9C%EC%83%81%ED%92%88%EC%BD%94%EB%93%9C=LN20000166&%EB%85%B8%EB%93%9C%EC%BD%94%EB%93%9C=00019&prcode=LN20000166', 'KB국민은행'),
(33, 'LOAN', 'KB 주택담보대출', '주택을 담보로 생활자금이나 주택자금을 대출받는 상품', 'https://obank.kbstar.com/quics?page=C103557&cc=b104363:b104516&%EB%B8%8C%EB%9E%9C%EB%93%9C%EC%83%81%ED%92%88%EC%BD%94%EB%93%9C=LN20001160&%EB%85%B8%EB%93%9C%EC%BD%94%EB%93%9C=00019&prcode=LN20001160', 'KB국민은행'),
(34, 'LOAN', 'KB스타 아파트담보대출(주택자금)', '아파트 구입 및 주택자금 마련을 위한 담보대출', 'https://obank.kbstar.com/quics?page=C103557&cc=b104363:b104516&%EB%B8%8C%EB%9E%9C%EB%93%9C%EC%83%81%ED%92%88%EC%BD%94%EB%93%9C=LN20001350&%EB%85%B8%EB%93%9C%EC%BD%94%EB%93%9C=00019&prcode=LN20001350', 'KB국민은행'),
(35, 'LOAN', 'KB 매직카대출(신차 구매용)', '신차 구매 시 이용하는 자동차 구매 전용 대출', 'https://obank.kbstar.com/quics?page=C103573&cc=b104363:b104516&%EB%B8%8C%EB%9E%9C%EB%93%9C%EC%83%81%ED%92%88%EC%BD%94%EB%93%9C=LN20000047&%EB%85%B8%EB%93%9C%EC%BD%94%EB%93%9C=00019&prcode=LN20000047', 'KB국민은행'),
(36, 'LOAN', 'KB 친환경 매직카대출(신차 구매용)', '친환경 차량 구매 고객을 위한 우대 자동차 대출', 'https://obank.kbstar.com/quics?page=C103573&cc=b104363:b104516&%EB%B8%8C%EB%9E%9C%EB%93%9C%EC%83%81%ED%92%88%EC%BD%94%EB%93%9C=LN20001390&%EB%85%B8%EB%93%9C%EC%BD%94%EB%93%9C=00019&prcode=LN20001390', 'KB국민은행'),
(37, 'LOAN', 'KB 매직카대출(중고차 구매용)', '중고차 구매 자금을 지원하는 자동차 대출', 'https://obank.kbstar.com/quics?page=C103573&cc=b104363:b104516&%EB%B8%8C%EB%9E%9C%EB%93%9C%EC%83%81%ED%92%88%EC%BD%94%EB%93%9C=LN20000108&%EB%85%B8%EB%93%9C%EC%BD%94%EB%93%9C=00019&prcode=LN20000108', 'KB국민은행'),
(38, 'LOAN', 'KB소상공인 신용대출', '사업기간 1년 이상 개인사업자를 위한 비대면 사업자 신용대출', 'https://zloan.kbstar.com/quics?page=C106666', 'KB국민은행'),
(39, 'LOAN', 'KB사장님+ 마이너스통장', '카드 가맹대금을 KB계좌로 입금받는 개인사업자를 위한 한도대출', 'https://zloan.kbstar.com/quics?page=C110940', 'KB국민은행'),
(40, 'LOAN', '소상공인 정책자금대출', '소상공인시장진흥공단 정책자금 지원대상 소상공인을 위한 보증서 대출', 'https://zloan.kbstar.com/quics?page=C112618', 'KB국민은행');

SELECT * FROM product;

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
(38, 'BUSINESS', 'OTHER', NULL, NULL, 'INDIVIDUAL', NULL, 'MOBILE', 5.32),
(39, 'BUSINESS', 'OTHER', NULL, NULL, 'INDIVIDUAL', NULL, 'MOBILE', 5.72),
(40, 'BUSINESS', 'OTHER', NULL, NULL, 'INDIVIDUAL', NULL, 'MOBILE', 3.39);

-- age_group_asset_median (연령대별 금융자산 중앙값 · KOSIS 가계금융복지조사 2025 · 만원×10000=원)
INSERT INTO age_group_asset_median (age_min,age_max,median_financial_asset,base_year) VALUES
(0,  29,  52450000, 2025),
(30, 39,  93300000, 2025),
(40, 49,  84600000, 2025),
(50, 59,  81000000, 2025),
(60, NULL,41500000, 2025);
