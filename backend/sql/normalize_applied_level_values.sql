-- Normalize applied title levels to the four allowed business values.
-- Allowed values: 技术员, 助理工程师, 工程师, 高级工程师
-- Legacy mapping:
--   员级   -> 技术员
--   初级   -> 助理工程师
--   中级   -> 工程师
--   副高级 -> 高级工程师
--
-- This script creates timestamped backup tables before updating data.
-- It updates only applied_level in Candidate, ActivityCandidate, and ActivityRangeSetting.
-- If invalid values remain after the updates, the transaction is rolled back.

set names utf8mb4;

set @backup_suffix = date_format(now(6), '%Y%m%d%H%i%s%f');

set @sql = concat('create table Candidate_applied_level_backup_', @backup_suffix, ' as select * from Candidate');
prepare stmt from @sql;
execute stmt;
deallocate prepare stmt;

set @sql = concat('create table ActivityCandidate_applied_level_backup_', @backup_suffix, ' as select * from ActivityCandidate');
prepare stmt from @sql;
execute stmt;
deallocate prepare stmt;

set @sql = concat('create table ActivityRangeSetting_applied_level_backup_', @backup_suffix, ' as select * from ActivityRangeSetting');
prepare stmt from @sql;
execute stmt;
deallocate prepare stmt;

drop procedure if exists normalize_applied_level_values;

delimiter //

create procedure normalize_applied_level_values()
begin
    declare v_invalid_count int default 0;

    declare exit handler for sqlexception
    begin
        rollback;
        resignal;
    end;

    start transaction;

    create temporary table tmp_activity_range_setting_merge as
    select
        coalesce(
            min(case
                when applied_level in ('技术员', '助理工程师', '工程师', '高级工程师') then id
                else null
            end),
            min(id)
        ) as keep_id,
        activity_id,
        department,
        case applied_level
            when '员级' then '技术员'
            when '初级' then '助理工程师'
            when '中级' then '工程师'
            when '副高级' then '高级工程师'
            else applied_level
        end as normalized_level,
        max(coalesce(locked_pass_count, 0)) as locked_pass_count,
        max(coalesce(locked_reject_count, 0)) as locked_reject_count,
        count(*) as row_count
    from ActivityRangeSetting
    group by activity_id,
             department,
             case applied_level
                when '员级' then '技术员'
                when '初级' then '助理工程师'
                when '中级' then '工程师'
                when '副高级' then '高级工程师'
                else applied_level
             end;

    update Candidate
    set applied_level = case applied_level
        when '员级' then '技术员'
        when '初级' then '助理工程师'
        when '中级' then '工程师'
        when '副高级' then '高级工程师'
        else applied_level
    end
    where applied_level in ('员级', '初级', '中级', '副高级');

    update ActivityCandidate
    set applied_level = case applied_level
        when '员级' then '技术员'
        when '初级' then '助理工程师'
        when '中级' then '工程师'
        when '副高级' then '高级工程师'
        else applied_level
    end
    where applied_level in ('员级', '初级', '中级', '副高级');

    delete ars
    from ActivityRangeSetting ars
    join tmp_activity_range_setting_merge merged
      on merged.activity_id = ars.activity_id
     and merged.department = ars.department
     and merged.normalized_level = case ars.applied_level
        when '员级' then '技术员'
        when '初级' then '助理工程师'
        when '中级' then '工程师'
        when '副高级' then '高级工程师'
        else ars.applied_level
     end
    where ars.id <> merged.keep_id;

    update ActivityRangeSetting ars
    join tmp_activity_range_setting_merge merged
      on merged.keep_id = ars.id
    set ars.applied_level = merged.normalized_level,
        ars.locked_pass_count = merged.locked_pass_count,
        ars.locked_reject_count = merged.locked_reject_count,
        ars.update_time = sysdate()
    where ars.applied_level <> merged.normalized_level
       or ars.locked_pass_count <> merged.locked_pass_count
       or ars.locked_reject_count <> merged.locked_reject_count
       or merged.row_count > 1;

    select count(*)
      into v_invalid_count
    from (
        select id
        from Candidate
        where applied_level is null
           or applied_level = ''
           or applied_level not in ('技术员', '助理工程师', '工程师', '高级工程师')
        union all
        select id
        from ActivityCandidate
        where applied_level is null
           or applied_level = ''
           or applied_level not in ('技术员', '助理工程师', '工程师', '高级工程师')
        union all
        select id
        from ActivityRangeSetting
        where applied_level is null
           or applied_level = ''
           or applied_level not in ('技术员', '助理工程师', '工程师', '高级工程师')
    ) invalid_rows;

    if v_invalid_count > 0 then
        select 'INVALID_AFTER_UPDATE' as check_name,
               'Candidate' as table_name,
               coalesce(applied_level, '<NULL>') as applied_level,
               count(*) as row_count
        from Candidate
        where applied_level is null
           or applied_level = ''
           or applied_level not in ('技术员', '助理工程师', '工程师', '高级工程师')
        group by applied_level
        union all
        select 'INVALID_AFTER_UPDATE',
               'ActivityCandidate',
               coalesce(applied_level, '<NULL>'),
               count(*)
        from ActivityCandidate
        where applied_level is null
           or applied_level = ''
           or applied_level not in ('技术员', '助理工程师', '工程师', '高级工程师')
        group by applied_level
        union all
        select 'INVALID_AFTER_UPDATE',
               'ActivityRangeSetting',
               coalesce(applied_level, '<NULL>'),
               count(*)
        from ActivityRangeSetting
        where applied_level is null
           or applied_level = ''
           or applied_level not in ('技术员', '助理工程师', '工程师', '高级工程师')
        group by applied_level;

        rollback;
        signal sqlstate '45000'
            set message_text = 'Invalid applied_level values remain; transaction rolled back';
    else
        commit;
    end if;
end//

delimiter ;

call normalize_applied_level_values();

drop procedure normalize_applied_level_values;

select 'FINAL_DISTRIBUTION' as check_name,
       'Candidate' as table_name,
       applied_level,
       count(*) as row_count
from Candidate
group by applied_level
union all
select 'FINAL_DISTRIBUTION',
       'ActivityCandidate',
       applied_level,
       count(*)
from ActivityCandidate
group by applied_level
union all
select 'FINAL_DISTRIBUTION',
       'ActivityRangeSetting',
       applied_level,
       count(*)
from ActivityRangeSetting
group by applied_level
order by table_name, applied_level;

select concat('Backup suffix: ', @backup_suffix) as backup_info;
