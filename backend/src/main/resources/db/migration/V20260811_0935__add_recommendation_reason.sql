
ALTER TABLE recommendation_product
    ADD COLUMN recommendation_reason VARCHAR(500) NULL COMMENT '추천순 1등 상품 추천 이유' AFTER is_selected,
    ADD CONSTRAINT chk_rp_reason_rank
        CHECK (recommendation_reason IS NULL OR (rank_no = 1 AND is_selected = 1));

ALTER TABLE personal_recommendation_tax_saving
    ADD COLUMN recommendation_reason VARCHAR(500) NULL COMMENT '추천순 1등 상품 추천 이유' AFTER rank_no,
    ADD CONSTRAINT chk_prts_reason_rank
        CHECK (recommendation_reason IS NULL OR rank_no = 1);

ALTER TABLE personal_recommendation_investment
    ADD COLUMN recommendation_reason VARCHAR(500) NULL COMMENT '추천순 1등 상품 추천 이유' AFTER rank_no,
    ADD CONSTRAINT chk_prin_reason_rank
        CHECK (recommendation_reason IS NULL OR rank_no = 1);