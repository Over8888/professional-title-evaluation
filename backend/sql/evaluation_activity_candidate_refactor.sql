-- Split candidate master data from activity candidate snapshots.
-- Run this after backing up databases created from the earlier Candidate(activity_id) design.

drop table if exists ActivityCandidate;
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
  department varchar(200) default null comment 'Department or review major snapshot',
  third_level_department varchar(200) default null comment 'Third level department snapshot',
  position varchar(200) default null comment 'Position snapshot',
  id_card varchar(32) default null comment 'ID card snapshot',
  level varchar(100) default null comment 'Applied title level snapshot',
  sort_order int default null comment 'Sort order in activity',
  fixed_type varchar(30) default null comment 'PASS/VOTE/REJECT range result; null means range not applied',
  last_year_assessment varchar(100) default null comment 'Last year assessment snapshot',
  evaluation_score varchar(100) default null comment 'Evaluation score or result snapshot',
  remark varchar(500) default null comment 'Remark',
  invalid_remark varchar(500) default null comment 'Invalid remark snapshot',
  create_by varchar(64) default '' comment 'Created by',
  create_time datetime default current_timestamp comment 'Created time',
  update_by varchar(64) default '' comment 'Updated by',
  update_time datetime default null comment 'Updated time',
  primary key (id),
  unique key uk_activity_candidate_scope (activity_id, department, level, id_card),
  key idx_activity_candidate_activity (activity_id),
  key idx_activity_candidate_candidate (candidate_id),
  key idx_activity_candidate_source (source_activity_id, source_activity_candidate_id)
) engine=innodb auto_increment=1 comment='Activity candidate snapshot';

insert into ActivityCandidate(
  activity_id, candidate_id, source_type, source_activity_id, source_activity_candidate_id,
  import_seq, name, gender, birth_date, education, company, department,
  third_level_department, position, id_card, level, sort_order, fixed_type, last_year_assessment, evaluation_score,
  remark, invalid_remark, create_by, create_time, update_by, update_time
)
select
  activity_id,
  (
    select cp.id
    from Candidate cp
    where cp.activity_id is null and cp.id_card = ca.id_card
    order by cp.id
    limit 1
  ) as candidate_id,
  'MIGRATED',
  ca.activity_id,
  ca.id,
  import_seq, name, gender, birth_date, education, company, department,
  third_level_department, position, id_card, level, sort_order, fixed_type, last_year_assessment, evaluation_score,
  remark, invalid_remark, create_by, create_time, update_by, update_time
from Candidate ca
where ca.activity_id is not null;

insert into Candidate(
  import_seq, name, gender, birth_date, education, company, department, third_level_department, position,
  id_card, level, sort_order, last_year_assessment, evaluation_score, remark, invalid_remark,
  create_by, create_time, update_by, update_time
)
select
  ca.import_seq, ca.name, ca.gender, ca.birth_date, ca.education, ca.company, ca.department,
  ca.third_level_department, ca.position, ca.id_card, ca.level, ca.sort_order, ca.last_year_assessment,
  ca.evaluation_score, ca.remark, ca.invalid_remark, ca.create_by, ca.create_time, ca.update_by, ca.update_time
from Candidate ca
join (
  select min(ca2.id) as id
  from Candidate ca2
  where ca2.activity_id is not null
    and ca2.id_card is not null
    and not exists (
      select 1 from Candidate cp where cp.activity_id is null and cp.id_card = ca2.id_card
    )
  group by ca2.id_card
) keep on keep.id = ca.id
where ca.activity_id is not null
  and ca.id_card is not null;

update ActivityCandidate ac
join (
  select id_card, min(id) as id
  from Candidate
  where activity_id is null
    and id_card is not null
  group by id_card
) cp on cp.id_card = ac.id_card
set ac.candidate_id = cp.id;

delete c
from Candidate c
join Candidate keep on keep.activity_id is null and keep.id_card = c.id_card and keep.id < c.id
where c.activity_id is null
  and c.id_card is not null;

alter table Vote add column activity_candidate_id bigint(20) null after voter_id;

update Vote v
join ActivityCandidate ac
  on ac.source_type = 'MIGRATED'
 and ac.activity_id = v.activity_id
 and ac.source_activity_candidate_id = v.candidate_id
set v.activity_candidate_id = ac.id;

alter table Vote drop index uk_vote_once;
alter table Vote drop index idx_vote_candidate;
alter table Vote drop column candidate_id;
alter table Vote modify activity_candidate_id bigint(20) not null comment 'Activity candidate snapshot ID';
alter table Vote
  add unique key uk_vote_once (activity_id, voter_id, activity_candidate_id, round_no),
  add key idx_vote_activity_candidate (activity_id, activity_candidate_id);

alter table ResultAgg add column activity_candidate_id bigint(20) null after activity_id;

update ResultAgg r
join ActivityCandidate ac
  on ac.source_type = 'MIGRATED'
 and ac.activity_id = r.activity_id
 and ac.source_activity_candidate_id = r.candidate_id
set r.activity_candidate_id = ac.id;

alter table ResultAgg drop index idx_result_candidate;
alter table ResultAgg drop column candidate_id;
alter table ResultAgg modify activity_candidate_id bigint(20) not null comment 'Activity candidate snapshot ID';
alter table ResultAgg add key idx_result_activity_candidate (activity_candidate_id);

alter table FinalEvaluation add column activity_candidate_id bigint(20) null after activity_id;

update FinalEvaluation f
join ActivityCandidate ac
  on ac.source_type = 'MIGRATED'
 and ac.activity_id = f.activity_id
 and ac.source_activity_candidate_id = f.candidate_id
set f.activity_candidate_id = ac.id,
    f.candidate_id = ac.candidate_id;

delete from Candidate where activity_id is not null;

alter table Candidate drop index uk_candidate_activity_id_card;
alter table Candidate drop column activity_id;
alter table Candidate drop column fixed_type;
alter table Candidate add unique key uk_candidate_id_card (id_card);

alter table ActivityCandidate
  add constraint fk_activity_candidate_activity foreign key (activity_id) references Activity(id) on delete cascade,
  add constraint fk_activity_candidate_candidate foreign key (candidate_id) references Candidate(id) on delete set null;
