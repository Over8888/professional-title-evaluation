-- Repair ActivityCandidate.department for sorted Excel imports when the target group
-- can be inferred safely from a single sorted import file per activity.
--
-- Rules:
--   * Only ActivityCandidate rows with source_type = 'SORTED_EXCEL' are updated.
--   * Candidate import files that are not system-entry files and have an inferred
--     group in the file name are treated as sorted-file import jobs.
--   * Only activities with exactly one inferred sorted-file group are updated.
--   * Activities with multiple sorted-file groups, or updates that would collide
--     with the unique activity/department/level/id-card key, are reported and skipped.
--   * A timestamped backup table is created before updates.

set names utf8mb4;

set @backup_suffix = date_format(now(6), '%Y%m%d%H%i%s%f');

set @sql = concat('create table ActivityCandidate_sorted_department_backup_', @backup_suffix, ' as select * from ActivityCandidate');
prepare stmt from @sql;
execute stmt;
deallocate prepare stmt;

drop temporary table if exists tmp_sorted_import_jobs;
create temporary table tmp_sorted_import_jobs as
select
    activity_id,
    file_name,
    cast(case
        when replace(file_name, '组组', '组') like '%船体组%' or file_name like '%船体%' then '船体组'
        when replace(file_name, '组组', '组') like '%船机组%' or file_name like '%船机%' then '船机组'
        when replace(file_name, '组组', '组') like '%船电组%' or file_name like '%船电%' then '船电组'
        when replace(file_name, '组组', '组') like '%综合组%' or file_name like '%综合%' then '综合组'
        else null
    end as char character set utf8mb4) collate utf8mb4_general_ci as target_department
from ExportJob
where export_type = 'CANDIDATE_IMPORT'
  and status in ('SUCCESS', 'PARTIAL_SUCCESS')
  and activity_id is not null
  and file_name not like '%系统导入%';

drop temporary table if exists tmp_sorted_import_targets;
create temporary table tmp_sorted_import_targets as
select
    activity_id,
    min(target_department) as target_department,
    count(distinct target_department) as department_count,
    group_concat(distinct file_name order by file_name separator ' | ') as file_names
from tmp_sorted_import_jobs
where target_department is not null
group by activity_id;

select
    'will_update' as report_type,
    t.activity_id,
    t.target_department,
    count(*) as rows_to_update,
    min(ac.import_seq) as min_import_seq,
    max(ac.import_seq) as max_import_seq,
    t.file_names
from tmp_sorted_import_targets t
join ActivityCandidate ac on ac.activity_id = t.activity_id
where t.department_count = 1
  and ac.source_type = 'SORTED_EXCEL'
  and coalesce(ac.department, '') <> t.target_department
group by t.activity_id, t.target_department, t.file_names
order by t.activity_id;

select
    'skipped_multi_group_activity' as report_type,
    activity_id,
    file_names
from tmp_sorted_import_targets
where department_count > 1
order by activity_id;

drop temporary table if exists tmp_sorted_import_conflicts;
create temporary table tmp_sorted_import_conflicts as
select distinct
    ac.activity_id,
    t.target_department,
    ac.applied_level,
    ac.id_card
from ActivityCandidate ac
join tmp_sorted_import_targets t on t.activity_id = ac.activity_id
join ActivityCandidate existing
    on existing.activity_id = ac.activity_id
   and existing.department = t.target_department
   and existing.applied_level = ac.applied_level
   and existing.id_card = ac.id_card
   and existing.id <> ac.id
where t.department_count = 1
  and ac.source_type = 'SORTED_EXCEL'
  and coalesce(ac.department, '') <> t.target_department;

select
    'skipped_unique_key_conflict' as report_type,
    activity_id,
    target_department,
    count(*) as conflict_rows
from tmp_sorted_import_conflicts
group by activity_id, target_department
order by activity_id;

start transaction;

update ActivityCandidate ac
join tmp_sorted_import_targets t on t.activity_id = ac.activity_id
set ac.department = t.target_department,
    ac.update_time = sysdate()
where t.department_count = 1
  and ac.source_type = 'SORTED_EXCEL'
  and coalesce(ac.department, '') <> t.target_department
  and not exists (
      select 1
      from tmp_sorted_import_conflicts c
      where c.activity_id = ac.activity_id
  );

select row_count() as updated_rows;

commit;

select
    ac.activity_id,
    ac.department,
    ac.applied_level,
    count(*) as row_count,
    min(ac.import_seq) as min_import_seq,
    max(ac.import_seq) as max_import_seq
from ActivityCandidate ac
join tmp_sorted_import_targets t on t.activity_id = ac.activity_id
where t.department_count = 1
  and ac.source_type = 'SORTED_EXCEL'
group by ac.activity_id, ac.department, ac.applied_level
order by ac.activity_id,
         case ac.department
           when '船体组' then 1
           when '船机组' then 2
           when '船电组' then 3
           when '综合组' then 4
           else 99
         end,
         ac.department,
         ac.applied_level;
