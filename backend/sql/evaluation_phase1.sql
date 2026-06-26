-- Evaluation voting system phase 1 business tables.
-- Manager reuses system user and role tables for authentication and authorization.
-- The minimal Manager table below is a compatibility mapping to sys_user, not a separate password system.

set foreign_key_checks = 0;
drop table if exists FinalEvaluation;
drop table if exists ExportJob;
drop table if exists ResultAgg;
drop table if exists RuleConfig;
drop table if exists Vote;
drop table if exists ActivityVoter;
drop table if exists Voter;
drop table if exists ActivityCandidate;
drop table if exists Candidate;
drop table if exists Manager;
drop table if exists Activity;
set foreign_key_checks = 1;

create table Activity (
  id bigint(20) not null auto_increment comment 'Activity ID',
  name varchar(100) not null comment 'Activity name',
  type varchar(30) default 'TITLE_REVIEW' comment 'Activity type',
  status varchar(30) default 'DRAFT' comment 'Activity status',
  publish_time datetime default null comment 'Publish time',
  start_time datetime default null comment 'Start time',
  end_time datetime default null comment 'End time',
  vote_entry_key varchar(64) default null comment 'Vote entry key',
  qr_code_url varchar(255) default null comment 'QR code URL',
  archived char(1) default '0' comment 'Archived flag 0 no 1 yes',
  created_by varchar(64) default '' comment 'Created by',
  created_at datetime default current_timestamp comment 'Created at',
  updated_by varchar(64) default '' comment 'Updated by',
  updated_at datetime default null comment 'Updated at',
  remark varchar(500) default null comment 'Remark',
  primary key (id)
) engine=innodb auto_increment=1 comment='Evaluation activity';

create table Candidate (
  id bigint(20) not null auto_increment comment 'Candidate ID',
  import_seq int default null comment 'Import sequence',
  name varchar(100) not null comment 'Candidate name',
  gender varchar(20) default null comment 'Gender',
  birth_date date default null comment 'Birth date',
  education varchar(100) default null comment 'Education',
  company varchar(200) default null comment 'Company',
  first_level_department varchar(200) default null comment 'First level department',
  department varchar(200) default null comment 'Department or review major',
  third_level_department varchar(200) default null comment 'Third level department',
  position varchar(200) default null comment 'Position',
  id_card varchar(32) default null comment 'ID card',
  current_level varchar(100) default null comment 'Current title level',
  applied_level varchar(100) default null comment 'Applied title level',
  last_year_assessment varchar(100) default null comment 'Last year assessment',
  evaluation_score varchar(100) default null comment 'Evaluation score or result',
  create_by varchar(64) default '' comment 'Created by',
  create_time datetime default current_timestamp comment 'Created time',
  update_by varchar(64) default '' comment 'Updated by',
  update_time datetime default null comment 'Updated time',
  primary key (id),
  unique key uk_candidate_import_seq (import_seq),
  unique key uk_candidate_id_card (id_card)
) engine=innodb auto_increment=1 comment='Candidate master data';

create table ActivityCandidate (
  id bigint(20) not null auto_increment comment 'Activity candidate snapshot ID',
  activity_id bigint(20) not null comment 'Activity ID',
  candidate_id bigint(20) default null comment 'Candidate master ID',
  source_type varchar(30) default null comment 'POOL/EXCEL/COPY_ACTIVITY/MANUAL',
  source_activity_id bigint(20) default null comment 'Source activity ID when copied',
  source_activity_candidate_id bigint(20) default null comment 'Source activity candidate snapshot ID when copied',
  import_seq int default null comment 'Import sequence',
  name varchar(100) not null comment 'Candidate name snapshot',
  gender varchar(20) default null comment 'Gender snapshot',
  birth_date date default null comment 'Birth date snapshot',
  education varchar(100) default null comment 'Education snapshot',
  company varchar(200) default null comment 'Company snapshot',
  first_level_department varchar(200) default null comment 'First level department snapshot',
  department varchar(200) default null comment 'Department or review major snapshot',
  third_level_department varchar(200) default null comment 'Third level department snapshot',
  position varchar(200) default null comment 'Position snapshot',
  id_card varchar(32) default null comment 'ID card snapshot',
  current_level varchar(100) default null comment 'Current title level snapshot',
  applied_level varchar(100) default null comment 'Applied title level snapshot',
  fixed_type varchar(30) default null comment 'PASS/VOTE/REJECT range result; null means range not applied',
  last_year_assessment varchar(100) default null comment 'Last year assessment snapshot',
  evaluation_score varchar(100) default null comment 'Evaluation score or result snapshot',
  create_by varchar(64) default '' comment 'Created by',
  create_time datetime default current_timestamp comment 'Created time',
  update_by varchar(64) default '' comment 'Updated by',
  update_time datetime default null comment 'Updated time',
  primary key (id),
  unique key uk_activity_candidate_scope (activity_id, department, applied_level, id_card),
  key idx_activity_candidate_activity (activity_id),
  key idx_activity_candidate_candidate (candidate_id),
  key idx_activity_candidate_source (source_activity_id, source_activity_candidate_id),
  constraint fk_activity_candidate_activity foreign key (activity_id) references Activity(id) on delete cascade,
  constraint fk_activity_candidate_candidate foreign key (candidate_id) references Candidate(id) on delete set null
) engine=innodb auto_increment=1 comment='Activity candidate snapshot';

create table Voter (
  id bigint(20) not null auto_increment comment 'Voter ID',
  import_seq int default null comment 'Import sequence',
  name varchar(100) not null comment 'Voter name',
  employee_id varchar(64) default null comment 'Employee ID',
  department varchar(200) default null comment 'Department',
  create_by varchar(64) default '' comment 'Created by',
  create_time datetime default current_timestamp comment 'Created time',
  update_by varchar(64) default '' comment 'Updated by',
  update_time datetime default null comment 'Updated time',
  remark varchar(500) default null comment 'Remark',
  primary key (id),
  unique key uk_voter_import_seq (import_seq),
  unique key uk_voter_employee (employee_id)
) engine=innodb auto_increment=1 comment='Voter master data';

create table ActivityVoter (
  id bigint(20) not null auto_increment comment 'Activity voter snapshot ID',
  activity_id bigint(20) not null comment 'Activity ID',
  voter_id bigint(20) default null comment 'Voter master ID',
  import_seq int default null comment 'Import sequence snapshot',
  source_type varchar(30) default null comment 'POOL/EXCEL/COPY_ACTIVITY/MANUAL',
  source_activity_id bigint(20) default null comment 'Source activity ID when copied',
  source_activity_voter_id bigint(20) default null comment 'Source activity voter snapshot ID when copied',
  name varchar(100) not null comment 'Voter name snapshot',
  employee_id varchar(64) default null comment 'Employee ID snapshot',
  department varchar(200) default null comment 'Department snapshot',
  status varchar(30) default 'PENDING' comment 'PENDING/DONE',
  vote_token varchar(128) default null comment 'Unique vote token for activity snapshot',
  submitted_at datetime default null comment 'Submitted at',
  create_by varchar(64) default '' comment 'Created by',
  create_time datetime default current_timestamp comment 'Created time',
  update_by varchar(64) default '' comment 'Updated by',
  update_time datetime default null comment 'Updated time',
  remark varchar(500) default null comment 'Remark',
  primary key (id),
  unique key uk_activity_voter_token (vote_token),
  unique key uk_activity_voter_employee (activity_id, employee_id),
  key idx_activity_voter_activity (activity_id),
  key idx_activity_voter_voter (voter_id),
  key idx_activity_voter_source (source_activity_id, source_activity_voter_id),
  constraint fk_activity_voter_activity foreign key (activity_id) references Activity(id) on delete cascade,
  constraint fk_activity_voter_voter foreign key (voter_id) references Voter(id) on delete set null
) engine=innodb auto_increment=1 comment='Activity voter snapshot';

create table Vote (
  id bigint(20) not null auto_increment comment 'Vote ID',
  activity_id bigint(20) not null comment 'Activity ID',
  activity_voter_id bigint(20) not null comment 'Activity voter snapshot ID',
  activity_candidate_id bigint(20) not null comment 'Activity candidate snapshot ID',
  result varchar(20) not null comment 'PASS/REJECT',
  round_no int default 1 comment 'Round number',
  created_at datetime default current_timestamp comment 'Created at',
  primary key (id),
  unique key uk_vote_once (activity_id, activity_voter_id, activity_candidate_id, round_no),
  key idx_vote_activity_voter (activity_id, activity_voter_id),
  key idx_vote_activity_candidate (activity_id, activity_candidate_id)
) engine=innodb auto_increment=1 comment='Evaluation vote detail';

create table RuleConfig (
  id bigint(20) not null auto_increment comment 'Rule config ID',
  activity_id bigint(20) not null comment 'Activity ID',
  pass_ratio decimal(8,4) default null comment 'Pass ratio',
  reject_ratio decimal(8,4) default null comment 'Reject ratio',
  vote_type varchar(30) default 'PASS_REJECT' comment 'Vote type',
  create_by varchar(64) default '' comment 'Created by',
  create_time datetime default current_timestamp comment 'Created time',
  update_by varchar(64) default '' comment 'Updated by',
  update_time datetime default null comment 'Updated time',
  remark varchar(500) default null comment 'Remark',
  primary key (id),
  unique key uk_rule_activity (activity_id)
) engine=innodb auto_increment=1 comment='Evaluation rule config';

create table ResultAgg (
  id bigint(20) not null auto_increment comment 'Result aggregate ID',
  activity_id bigint(20) not null comment 'Activity ID',
  activity_candidate_id bigint(20) not null comment 'Activity candidate snapshot ID',
  stat_scope varchar(50) default 'TOTAL' comment 'Stat scope',
  stat_key varchar(100) default null comment 'Stat key',
  vote_PASS_count int default 0 comment 'Pass count',
  vote_REJECT_count int default 0 comment 'Reject count',
  total_votes int default 0 comment 'Total votes',
  pass_rate decimal(8,4) default 0 comment 'Pass rate',
  reject_rate decimal(8,4) default 0 comment 'Reject rate',
  rank_no int default null comment 'Rank number',
  final_result varchar(30) default null comment 'Final result',
  update_at datetime default null comment 'Updated at',
  calculated_by varchar(64) default '' comment 'Calculated by',
  calculated_at datetime default null comment 'Calculated at',
  primary key (id),
  key idx_result_activity (activity_id),
  key idx_result_activity_candidate (activity_candidate_id)
) engine=innodb auto_increment=1 comment='Evaluation result aggregate';

create table ExportJob (
  id bigint(20) not null auto_increment comment 'Job ID',
  activity_id bigint(20) default null comment 'Activity ID',
  job_type varchar(30) default 'EXPORT' comment 'IMPORT/EXPORT',
  export_type varchar(50) default null comment 'Import or export type',
  status varchar(30) default 'SUCCESS' comment 'Job status',
  file_name varchar(255) default null comment 'File name',
  file_url varchar(500) default null comment 'File URL',
  generated_by varchar(64) default '' comment 'Generated by',
  generated_at datetime default current_timestamp comment 'Generated at',
  error_message text null comment 'Error message',
  primary key (id),
  key idx_export_activity (activity_id)
) engine=innodb auto_increment=1 comment='Evaluation import/export job';

create table FinalEvaluation (
  id bigint(20) not null auto_increment comment 'Final evaluation ID',
  activity_id bigint(20) not null comment 'Activity ID',
  activity_candidate_id bigint(20) default null comment 'Activity candidate snapshot ID',
  candidate_id bigint(20) default null comment 'Candidate master ID',
  final_rank int default null comment 'Final rank',
  company varchar(200) default null comment 'Company',
  id_card varchar(32) default null comment 'ID card',
  candidate_name varchar(100) default null comment 'Candidate name',
  applied_title varchar(100) default null comment 'Applied title',
  review_major varchar(200) default null comment 'Review major',
  evaluation_score varchar(100) default null comment 'Evaluation score or result',
  final_result varchar(50) default null comment 'Final result',
  source_type varchar(30) default null comment 'Source type',
  imported_by varchar(64) default '' comment 'Imported by',
  confirmed_by varchar(64) default '' comment 'Confirmed by',
  confirmed_at datetime default null comment 'Confirmed at',
  primary key (id),
  key idx_final_activity (activity_id),
  key idx_final_id_card (activity_id, id_card)
) engine=innodb auto_increment=1 comment='Final evaluation result';

create table Manager (
  id bigint(20) not null auto_increment comment 'Manager mapping ID',
  user_id bigint(20) not null comment 'System sys_user.user_id',
  username varchar(64) not null comment 'System sys_user.user_name snapshot',
  manager_type varchar(30) default 'HR' comment 'Manager type',
  create_time datetime default current_timestamp comment 'Created time',
  remark varchar(500) default 'Manager reuses system user and role tables; password is not stored here.' comment 'Compatibility note',
  primary key (id),
  unique key uk_manager_user (user_id)
) engine=innodb auto_increment=1 comment='Evaluation manager mapping';
