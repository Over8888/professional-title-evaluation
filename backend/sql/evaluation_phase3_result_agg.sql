create table if not exists ResultAgg (
  id bigint(20) not null auto_increment comment 'Result aggregate ID',
  activity_id bigint(20) not null comment 'Activity ID',
  activity_candidate_id bigint(20) not null comment 'Activity candidate snapshot ID',
  stat_scope varchar(50) not null default 'CANDIDATE_GROUP' comment 'Stat scope',
  stat_key varchar(100) not null default '' comment 'Stat key',
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
  unique key uk_result_activity_candidate_scope (activity_id, activity_candidate_id, stat_scope, stat_key),
  key idx_result_activity (activity_id),
  key idx_result_activity_candidate (activity_candidate_id)
) engine=innodb auto_increment=1 comment='Evaluation result aggregate';

update ResultAgg set stat_scope = 'CANDIDATE_GROUP' where stat_scope is null or stat_scope = '';
update ResultAgg set stat_key = '' where stat_key is null;

alter table ResultAgg modify stat_scope varchar(50) not null default 'CANDIDATE_GROUP' comment 'Stat scope';
alter table ResultAgg modify stat_key varchar(100) not null default '' comment 'Stat key';

set @schema_name = database();

set @sql = (
  select if(count(*) = 0,
    'alter table ResultAgg add unique key uk_result_activity_candidate_scope (activity_id, activity_candidate_id, stat_scope, stat_key)',
    'select 1')
  from information_schema.statistics
  where table_schema = @schema_name
    and table_name = 'ResultAgg'
    and index_name = 'uk_result_activity_candidate_scope'
);
prepare stmt from @sql;
execute stmt;
deallocate prepare stmt;
