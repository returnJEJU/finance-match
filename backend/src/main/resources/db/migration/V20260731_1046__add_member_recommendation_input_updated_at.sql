ALTER TABLE member
    ADD COLUMN recommendation_input_updated_at DATETIME NOT NULL
        DEFAULT CURRENT_TIMESTAMP
        COMMENT '추천에 사용하는 회원정보 최종 수정 시각'
        AFTER kb_star_savings_eligible;
