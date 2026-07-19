-- Stage 1.5 canonical schema for the evaluation module.
-- Keep table names case-sensitive and aligned with MyBatis mapper XML.

CREATE TABLE IF NOT EXISTS `Activity` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(200) NOT NULL,
  `type` varchar(50) NOT NULL DEFAULT 'TITLE_REVIEW',
  `status` varchar(50) NOT NULL DEFAULT 'CONFIGURED',
  `publish_time` datetime DEFAULT NULL,
  `start_time` datetime DEFAULT NULL,
  `end_time` datetime DEFAULT NULL,
  `vote_entry_key` varchar(64) DEFAULT NULL,
  `qr_code_url` varchar(500) DEFAULT NULL,
  `archived` char(1) NOT NULL DEFAULT '0',
  `created_by` varchar(64) DEFAULT NULL,
  `created_at` datetime DEFAULT NULL,
  `updated_by` varchar(64) DEFAULT NULL,
  `updated_at` datetime DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_Activity_vote_entry_key` (`vote_entry_key`),
  KEY `idx_Activity_status_time` (`status`, `start_time`, `end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS `RuleConfig` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `activity_id` bigint NOT NULL,
  `pass_ratio` decimal(8,4) NOT NULL,
  `reject_ratio` decimal(8,4) NOT NULL,
  `vote_type` varchar(50) NOT NULL DEFAULT 'PASS_REJECT',
  `create_by` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` varchar(64) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_RuleConfig_activity` (`activity_id`),
  CONSTRAINT `fk_RuleConfig_Activity` FOREIGN KEY (`activity_id`) REFERENCES `Activity` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS `Candidate` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `import_seq` int DEFAULT NULL,
  `name` varchar(100) NOT NULL,
  `gender` varchar(20) DEFAULT NULL,
  `birth_date` date DEFAULT NULL,
  `education` varchar(100) DEFAULT NULL,
  `company` varchar(200) DEFAULT NULL,
  `first_level_department` varchar(200) DEFAULT NULL,
  `department` varchar(200) DEFAULT NULL,
  `third_level_department` varchar(200) DEFAULT NULL,
  `position` varchar(200) DEFAULT NULL,
  `id_card` varchar(64) DEFAULT NULL,
  `current_level` varchar(100) DEFAULT NULL,
  `applied_level` varchar(100) DEFAULT NULL,
  `last_year_assessment` varchar(100) DEFAULT NULL,
  `evaluation_score` varchar(100) DEFAULT NULL,
  `create_by` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` varchar(64) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_Candidate_id_card` (`id_card`),
  KEY `idx_Candidate_scope` (`department`, `applied_level`, `import_seq`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS `ActivityCandidate` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `activity_id` bigint NOT NULL,
  `candidate_id` bigint DEFAULT NULL,
  `source_type` varchar(50) DEFAULT NULL,
  `source_activity_id` bigint DEFAULT NULL,
  `source_activity_candidate_id` bigint DEFAULT NULL,
  `import_seq` int DEFAULT NULL,
  `name` varchar(100) NOT NULL,
  `gender` varchar(20) DEFAULT NULL,
  `birth_date` date DEFAULT NULL,
  `education` varchar(100) DEFAULT NULL,
  `company` varchar(200) DEFAULT NULL,
  `first_level_department` varchar(200) DEFAULT NULL,
  `department` varchar(200) DEFAULT NULL,
  `third_level_department` varchar(200) DEFAULT NULL,
  `position` varchar(200) DEFAULT NULL,
  `id_card` varchar(64) DEFAULT NULL,
  `current_level` varchar(100) DEFAULT NULL,
  `applied_level` varchar(100) DEFAULT NULL,
  `fixed_type` varchar(20) DEFAULT NULL,
  `last_year_assessment` varchar(100) DEFAULT NULL,
  `evaluation_score` varchar(100) DEFAULT NULL,
  `create_by` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` varchar(64) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ActivityCandidate_scope_id_card` (`activity_id`, `department`, `applied_level`, `id_card`),
  KEY `idx_ActivityCandidate_activity_scope` (`activity_id`, `department`, `applied_level`, `fixed_type`, `import_seq`),
  KEY `idx_ActivityCandidate_candidate` (`candidate_id`),
  CONSTRAINT `fk_ActivityCandidate_Activity` FOREIGN KEY (`activity_id`) REFERENCES `Activity` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS `Voter` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `import_seq` int DEFAULT NULL,
  `name` varchar(100) NOT NULL,
  `employee_id` varchar(100) DEFAULT NULL,
  `department` varchar(200) DEFAULT NULL,
  `create_by` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` varchar(64) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_Voter_identity` (`name`, `employee_id`),
  KEY `idx_Voter_department` (`department`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS `ActivityVoter` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `activity_id` bigint NOT NULL,
  `voter_id` bigint DEFAULT NULL,
  `import_seq` int DEFAULT NULL,
  `source_type` varchar(50) DEFAULT NULL,
  `source_activity_id` bigint DEFAULT NULL,
  `source_activity_voter_id` bigint DEFAULT NULL,
  `name` varchar(100) NOT NULL,
  `employee_id` varchar(100) DEFAULT NULL,
  `department` varchar(200) DEFAULT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'PENDING',
  `vote_token` varchar(64) NOT NULL,
  `submitted_at` datetime DEFAULT NULL,
  `create_by` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` varchar(64) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ActivityVoter_vote_token` (`vote_token`),
  UNIQUE KEY `uk_ActivityVoter_identity` (`activity_id`, `name`, `employee_id`),
  KEY `idx_ActivityVoter_progress` (`activity_id`, `status`),
  CONSTRAINT `fk_ActivityVoter_Activity` FOREIGN KEY (`activity_id`) REFERENCES `Activity` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS `Vote` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `activity_id` bigint NOT NULL,
  `activity_voter_id` bigint NOT NULL,
  `activity_candidate_id` bigint NOT NULL,
  `result` varchar(20) NOT NULL,
  `round_no` int NOT NULL DEFAULT 1,
  `created_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_Vote_once_per_candidate` (`activity_id`, `activity_voter_id`, `round_no`, `activity_candidate_id`),
  KEY `idx_Vote_candidate_count` (`activity_id`, `activity_candidate_id`, `result`),
  CONSTRAINT `fk_Vote_Activity` FOREIGN KEY (`activity_id`) REFERENCES `Activity` (`id`),
  CONSTRAINT `fk_Vote_ActivityVoter` FOREIGN KEY (`activity_voter_id`) REFERENCES `ActivityVoter` (`id`),
  CONSTRAINT `fk_Vote_ActivityCandidate` FOREIGN KEY (`activity_candidate_id`) REFERENCES `ActivityCandidate` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS `ResultAgg` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `activity_id` bigint NOT NULL,
  `activity_candidate_id` bigint NOT NULL,
  `stat_scope` varchar(50) NOT NULL,
  `stat_key` varchar(255) DEFAULT NULL,
  `vote_PASS_count` int NOT NULL DEFAULT 0,
  `vote_REJECT_count` int NOT NULL DEFAULT 0,
  `total_votes` int NOT NULL DEFAULT 0,
  `pass_rate` decimal(10,4) DEFAULT NULL,
  `reject_rate` decimal(10,4) DEFAULT NULL,
  `rank_no` int DEFAULT NULL,
  `final_result` varchar(20) DEFAULT NULL,
  `update_at` datetime DEFAULT NULL,
  `calculated_by` varchar(64) DEFAULT NULL,
  `calculated_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ResultAgg_candidate_scope` (`activity_id`, `activity_candidate_id`, `stat_scope`),
  KEY `idx_ResultAgg_activity_result` (`activity_id`, `final_result`, `rank_no`),
  CONSTRAINT `fk_ResultAgg_Activity` FOREIGN KEY (`activity_id`) REFERENCES `Activity` (`id`),
  CONSTRAINT `fk_ResultAgg_ActivityCandidate` FOREIGN KEY (`activity_candidate_id`) REFERENCES `ActivityCandidate` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS `FinalEvaluation` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `activity_id` bigint NOT NULL,
  `activity_candidate_id` bigint NOT NULL,
  `final_result` varchar(20) NOT NULL,
  `confirm_status` varchar(20) NOT NULL DEFAULT 'DRAFT',
  `confirmed_by` varchar(64) DEFAULT NULL,
  `confirmed_at` datetime DEFAULT NULL,
  `signed_by` varchar(200) DEFAULT NULL,
  `signature_status` varchar(20) DEFAULT NULL,
  `confirm_remark` varchar(1000) DEFAULT NULL,
  `create_by` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` varchar(64) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_FinalEvaluation_candidate` (`activity_id`, `activity_candidate_id`),
  KEY `idx_FinalEvaluation_status` (`activity_id`, `confirm_status`, `final_result`),
  CONSTRAINT `fk_FinalEvaluation_Activity` FOREIGN KEY (`activity_id`) REFERENCES `Activity` (`id`),
  CONSTRAINT `fk_FinalEvaluation_ActivityCandidate` FOREIGN KEY (`activity_candidate_id`) REFERENCES `ActivityCandidate` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS `ActivityRangeSetting` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `activity_id` bigint NOT NULL,
  `department` varchar(200) NOT NULL,
  `applied_level` varchar(100) NOT NULL,
  `locked_pass_count` int NOT NULL DEFAULT 0,
  `locked_reject_count` int NOT NULL DEFAULT 0,
  `create_by` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT NULL,
  `update_by` varchar(64) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ActivityRangeSetting_scope` (`activity_id`, `department`, `applied_level`),
  CONSTRAINT `fk_ActivityRangeSetting_Activity` FOREIGN KEY (`activity_id`) REFERENCES `Activity` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;

CREATE TABLE IF NOT EXISTS `ExportJob` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `activity_id` bigint DEFAULT NULL,
  `job_type` varchar(50) NOT NULL,
  `export_type` varchar(50) NOT NULL,
  `status` varchar(50) NOT NULL,
  `file_name` varchar(500) DEFAULT NULL,
  `file_url` varchar(1000) DEFAULT NULL,
  `generated_by` varchar(64) DEFAULT NULL,
  `generated_at` datetime DEFAULT NULL,
  `error_message` varchar(1000) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_ExportJob_activity` (`activity_id`, `job_type`, `export_type`, `generated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin;
