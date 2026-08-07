ALTER TABLE compatibility_result
    DROP CHECK chk_compat_asset;

ALTER TABLE compatibility_result
    ADD CONSTRAINT chk_compat_asset
        CHECK (asset_stability_score BETWEEN 0 AND 25);

ALTER TABLE compatibility_result
    DROP CHECK chk_compat_goal;

ALTER TABLE compatibility_result
    ADD CONSTRAINT chk_compat_goal
        CHECK (goal_feasibility_score BETWEEN 0 AND 20);

ALTER TABLE compatibility_result
    MODIFY COLUMN asset_stability_score DECIMAL(5,2) NOT NULL DEFAULT 0 COMMENT '자산 25',
    MODIFY COLUMN goal_feasibility_score DECIMAL(5,2) NOT NULL DEFAULT 0 COMMENT '목표 20';
