ALTER TABLE member
    ADD COLUMN kb_star_savings_eligible TINYINT(1) NOT NULL DEFAULT 0
        COMMENT 'KB스타적금Ⅲ 가입 가능 여부'
        AFTER investment_type;
