-- Split Voter into voter master data and ActivityVoter activity snapshots.
-- Existing activity-scoped Voter rows are migrated into ActivityVoter and deduplicated into Voter master rows.

drop procedure if exists migrate_activity_voter_refactor;

delimiter //
create procedure migrate_activity_voter_refactor()
begin
  declare has_voter_activity_id int default 0;
  declare has_vote_voter_id int default 0;
  declare has_vote_activity_voter_id int default 0;

  select count(1) into has_voter_activity_id
  from information_schema.columns
  where table_schema = database() and table_name = 'Voter' and column_name = 'activity_id';

  create table if not exists ActivityVoter (
    id bigint(20) not null auto_increment comment 'Activity voter snapshot ID',
    activity_id bigint(20) not null comment 'Activity ID',
    voter_id bigint(20) default null comment 'Voter master ID',
    source_type varchar(30) default null comment 'POOL/EXCEL/COPY_ACTIVITY/MANUAL/MIGRATED',
    source_activity_id bigint(20) default null comment 'Source activity ID when copied',
    source_activity_voter_id bigint(20) default null comment 'Source activity voter snapshot ID or legacy Voter ID',
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
    key idx_activity_voter_source (source_activity_id, source_activity_voter_id)
  ) engine=innodb auto_increment=1 comment='Activity voter snapshot';

  if has_voter_activity_id > 0 then
    drop table if exists VoterLegacyBeforeActivityVoterRefactor;
    create table VoterLegacyBeforeActivityVoterRefactor as select * from Voter;

    drop table if exists VoterNew;
    create table VoterNew (
      id bigint(20) not null auto_increment comment 'Voter ID',
      name varchar(100) not null comment 'Voter name',
      employee_id varchar(64) default null comment 'Employee ID',
      department varchar(200) default null comment 'Department',
      create_by varchar(64) default '' comment 'Created by',
      create_time datetime default current_timestamp comment 'Created time',
      update_by varchar(64) default '' comment 'Updated by',
      update_time datetime default null comment 'Updated time',
      remark varchar(500) default null comment 'Remark',
      primary key (id),
      unique key uk_voter_employee (employee_id)
    ) engine=innodb auto_increment=1 comment='Voter master data';

    insert into VoterNew(name, employee_id, department, create_by, create_time, update_by, update_time, remark)
    select
      min(name) as name,
      nullif(employee_id, '') as employee_id,
      max(department) as department,
      max(create_by) as create_by,
      min(create_time) as create_time,
      max(update_by) as update_by,
      max(update_time) as update_time,
      max(remark) as remark
    from VoterLegacyBeforeActivityVoterRefactor
    group by
      case
        when employee_id is not null and employee_id <> '' then concat('EMP|', employee_id)
        else concat('NAME|', name, '|DEPT|', coalesce(department, ''))
      end,
      nullif(employee_id, '');

    insert into ActivityVoter(activity_id, voter_id, source_type, source_activity_id, source_activity_voter_id, name, employee_id, department, status, vote_token, submitted_at, create_by, create_time, update_by, update_time, remark)
    select
      old.activity_id,
      master.id,
      'MIGRATED',
      old.activity_id,
      old.id,
      old.name,
      nullif(old.employee_id, ''),
      old.department,
      coalesce(old.status, 'PENDING'),
      old.vote_token,
      old.submitted_at,
      old.create_by,
      old.create_time,
      old.update_by,
      old.update_time,
      old.remark
    from VoterLegacyBeforeActivityVoterRefactor old
    join VoterNew master
      on (
        old.employee_id is not null and old.employee_id <> '' and master.employee_id = old.employee_id
      )
      or (
        (old.employee_id is null or old.employee_id = '')
        and master.employee_id is null
        and master.name = old.name
        and coalesce(master.department, '') = coalesce(old.department, '')
      )
    where old.activity_id is not null;

    select count(1) into has_vote_voter_id
    from information_schema.columns
    where table_schema = database() and table_name = 'Vote' and column_name = 'voter_id';

    select count(1) into has_vote_activity_voter_id
    from information_schema.columns
    where table_schema = database() and table_name = 'Vote' and column_name = 'activity_voter_id';

    if has_vote_voter_id > 0 and has_vote_activity_voter_id = 0 then
      alter table Vote add column activity_voter_id bigint(20) null comment 'Activity voter snapshot ID' after activity_id;
      update Vote v
      join ActivityVoter av on av.source_activity_voter_id = v.voter_id and av.activity_id = v.activity_id
      set v.activity_voter_id = av.id;
      alter table Vote drop index uk_vote_once;
      alter table Vote drop column voter_id;
      alter table Vote modify activity_voter_id bigint(20) not null comment 'Activity voter snapshot ID';
      alter table Vote add unique key uk_vote_once (activity_id, activity_voter_id, activity_candidate_id, round_no);
      alter table Vote add key idx_vote_activity_voter (activity_id, activity_voter_id);
    end if;

    drop table Voter;
    rename table VoterNew to Voter;
  end if;
end//
delimiter ;

call migrate_activity_voter_refactor();
drop procedure if exists migrate_activity_voter_refactor;
