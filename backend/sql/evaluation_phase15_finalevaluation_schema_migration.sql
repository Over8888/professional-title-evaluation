-- Upgrade legacy FinalEvaluation tables before deploying the Stage 1.5 mapper.
-- The procedure is idempotent and can be executed repeatedly on the same schema.
DELIMITER $$

DROP PROCEDURE IF EXISTS `migrateFinalEvaluationPhase15`$$

CREATE PROCEDURE `migrateFinalEvaluationPhase15`()
BEGIN
    DECLARE column_count INT DEFAULT 0;

    SELECT COUNT(*) INTO column_count
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'FinalEvaluation' AND COLUMN_NAME = 'confirm_status';
    IF column_count = 0 THEN
        ALTER TABLE `FinalEvaluation` ADD COLUMN `confirm_status` varchar(20) NOT NULL DEFAULT 'DRAFT' AFTER `final_result`;
    END IF;

    SELECT COUNT(*) INTO column_count
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'FinalEvaluation' AND COLUMN_NAME = 'confirmed_by';
    IF column_count = 0 THEN
        ALTER TABLE `FinalEvaluation` ADD COLUMN `confirmed_by` varchar(64) DEFAULT NULL AFTER `confirm_status`;
    END IF;

    SELECT COUNT(*) INTO column_count
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'FinalEvaluation' AND COLUMN_NAME = 'confirmed_at';
    IF column_count = 0 THEN
        ALTER TABLE `FinalEvaluation` ADD COLUMN `confirmed_at` datetime DEFAULT NULL AFTER `confirmed_by`;
    END IF;

    SELECT COUNT(*) INTO column_count
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'FinalEvaluation' AND COLUMN_NAME = 'signed_by';
    IF column_count = 0 THEN
        ALTER TABLE `FinalEvaluation` ADD COLUMN `signed_by` varchar(200) DEFAULT NULL AFTER `confirmed_at`;
    END IF;

    SELECT COUNT(*) INTO column_count
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'FinalEvaluation' AND COLUMN_NAME = 'signature_status';
    IF column_count = 0 THEN
        ALTER TABLE `FinalEvaluation` ADD COLUMN `signature_status` varchar(20) DEFAULT NULL AFTER `signed_by`;
    END IF;

    SELECT COUNT(*) INTO column_count
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'FinalEvaluation' AND COLUMN_NAME = 'confirm_remark';
    IF column_count = 0 THEN
        ALTER TABLE `FinalEvaluation` ADD COLUMN `confirm_remark` varchar(1000) DEFAULT NULL AFTER `signature_status`;
    END IF;

    SELECT COUNT(*) INTO column_count
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'FinalEvaluation' AND COLUMN_NAME = 'create_by';
    IF column_count = 0 THEN
        ALTER TABLE `FinalEvaluation` ADD COLUMN `create_by` varchar(64) DEFAULT NULL AFTER `confirm_remark`;
    END IF;

    SELECT COUNT(*) INTO column_count
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'FinalEvaluation' AND COLUMN_NAME = 'create_time';
    IF column_count = 0 THEN
        ALTER TABLE `FinalEvaluation` ADD COLUMN `create_time` datetime DEFAULT NULL AFTER `create_by`;
    END IF;

    SELECT COUNT(*) INTO column_count
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'FinalEvaluation' AND COLUMN_NAME = 'update_by';
    IF column_count = 0 THEN
        ALTER TABLE `FinalEvaluation` ADD COLUMN `update_by` varchar(64) DEFAULT NULL AFTER `create_time`;
    END IF;

    SELECT COUNT(*) INTO column_count
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'FinalEvaluation' AND COLUMN_NAME = 'update_time';
    IF column_count = 0 THEN
        ALTER TABLE `FinalEvaluation` ADD COLUMN `update_time` datetime DEFAULT NULL AFTER `update_by`;
    END IF;
END$$

CALL `migrateFinalEvaluationPhase15`()$$
DROP PROCEDURE IF EXISTS `migrateFinalEvaluationPhase15`$$

DELIMITER ;
