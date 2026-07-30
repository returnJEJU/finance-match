ALTER TABLE couple
    ADD COLUMN profile_message VARCHAR(50) NULL
        COMMENT '커플 한줄 소개'
        AFTER investment_type;
