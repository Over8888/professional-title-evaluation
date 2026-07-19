-- Restore the repository snapshot to a target MySQL database.
-- Run with the mysql client from the repository root:
--   mysql -u USER -p DB_NAME < backend/sql/0720_local_snapshot_sync.sql
-- This is an overwrite-style sync. It clears the target tables first.

SET FOREIGN_KEY_CHECKS=0;

SOURCE backend/sql/schema.sql;
SOURCE backend/sql/evaluation_phase15_finalevaluation_schema_migration.sql;
SOURCE backend/sql/evaluation_phase15_activity_candidate_third_level_department_repair.sql;

TRUNCATE TABLE `Vote`;
TRUNCATE TABLE `ResultAgg`;
TRUNCATE TABLE `FinalEvaluation`;
TRUNCATE TABLE `ExportJob`;
TRUNCATE TABLE `ActivityVoter`;
TRUNCATE TABLE `ActivityCandidate`;
TRUNCATE TABLE `ActivityRangeSetting`;
TRUNCATE TABLE `RuleConfig`;
TRUNCATE TABLE `Activity`;
TRUNCATE TABLE `Voter`;
TRUNCATE TABLE `Candidate`;
TRUNCATE TABLE `manager`;
TRUNCATE TABLE `sys_role_menu`;
TRUNCATE TABLE `sys_role_dept`;
TRUNCATE TABLE `sys_user_role`;
TRUNCATE TABLE `sys_user_post`;
TRUNCATE TABLE `sys_logininfor`;
TRUNCATE TABLE `sys_oper_log`;
TRUNCATE TABLE `sys_notice_read`;
TRUNCATE TABLE `sys_notice`;
TRUNCATE TABLE `sys_post`;
TRUNCATE TABLE `sys_dict_data`;
TRUNCATE TABLE `sys_dict_type`;
TRUNCATE TABLE `sys_menu`;
TRUNCATE TABLE `sys_role`;
TRUNCATE TABLE `sys_dept`;
TRUNCATE TABLE `sys_config`;
TRUNCATE TABLE `sys_user`;

SET FOREIGN_KEY_CHECKS=1;

SOURCE backend/sql/sys_dept.sql;
SOURCE backend/sql/sys_role.sql;
SOURCE backend/sql/sys_menu.sql;
SOURCE backend/sql/sys_role_menu.sql;
SOURCE backend/sql/sys_role_dept.sql;
SOURCE backend/sql/sys_user.sql;
SOURCE backend/sql/sys_user_role.sql;
SOURCE backend/sql/sys_user_post.sql;
SOURCE backend/sql/sys_post.sql;
SOURCE backend/sql/sys_dict_type.sql;
SOURCE backend/sql/sys_dict_data.sql;
SOURCE backend/sql/sys_config.sql;
SOURCE backend/sql/sys_notice.sql;
SOURCE backend/sql/sys_notice_read.sql;
SOURCE backend/sql/sys_oper_log.sql;
SOURCE backend/sql/sys_logininfor.sql;

SOURCE backend/sql/evaluation_phase15_finalevaluation_menu.sql;
SOURCE backend/sql/activity.sql;
SOURCE backend/sql/ruleconfig.sql;
SOURCE backend/sql/candidate.sql;
SOURCE backend/sql/voter.sql;
SOURCE backend/sql/activitycandidate.sql;
SOURCE backend/sql/activityvoter.sql;
SOURCE backend/sql/vote.sql;
SOURCE backend/sql/resultagg.sql;
SOURCE backend/sql/finalevaluation.sql;
SOURCE backend/sql/activityrangesetting.sql;
SOURCE backend/sql/exportjob.sql;
SOURCE backend/sql/manager.sql;
