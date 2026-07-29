ALTER TABLE compatibility_result
    ADD COLUMN tax_strategy_calculated TINYINT(1) NOT NULL DEFAULT 1
        COMMENT '절세 활용도 산출 여부'
        AFTER tax_strategy_score;