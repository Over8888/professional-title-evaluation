-- Activity range lock settings.
-- Safe to run after evaluation_phase1.sql.

create table if not exists ActivityRangeSetting (
  id bigint(20) not null auto_increment comment 'Activity range setting ID',
  activity_id bigint(20) not null comment 'Activity ID',
  department varchar(200) not null default '' comment 'Department or review major',
  applied_level varchar(100) not null default '' comment 'Applied title level',
  locked_pass_count int default 0 comment 'Locked pass count',
  locked_reject_count int default 0 comment 'Locked reject count',
  create_by varchar(64) default '' comment 'Created by',
  create_time datetime default current_timestamp comment 'Created time',
  update_by varchar(64) default '' comment 'Updated by',
  update_time datetime default null comment 'Updated time',
  primary key (id),
  unique key uk_activity_range_group (activity_id, department, applied_level),
  key idx_activity_range_activity (activity_id),
  constraint fk_activity_range_activity foreign key (activity_id) references Activity(id) on delete cascade
) engine=innodb auto_increment=1 comment='Activity range lock setting';
