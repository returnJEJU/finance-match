ALTER TABLE report
    ADD COLUMN expected_asset DECIMAL(18, 0) NULL
        COMMENT '목표 달성 가능성 축 예상 자산(리포트 화면 목표 카드 표시용)'
        AFTER goal_feasibility_reason;
