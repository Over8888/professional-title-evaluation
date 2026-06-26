-- Expand sample candidates to 100 and attach the same candidate pool to all activities.
-- Distribution keeps the title-review sample characteristics:
-- applied level first, then ship-related groups with larger body/comprehensive groups.

start transaction;

insert into Candidate(
    import_seq, name, gender, company, department, position, id_card,
    current_level, applied_level, level, sort_order, create_by, create_time, remark
)
with recursive seq(n) as (
    select 17
    union all
    select n + 1 from seq where n < 95
),
seed as (
    select
        n,
        concat('样例候选', lpad(n, 3, '0')) as name,
        case when mod(n, 2) = 0 then '女' else '男' end as gender,
        '青岛海洋装备有限公司' as company,
        case
            when n between 17 and 25 then '船体组'
            when n between 26 and 31 then '船机组'
            when n between 32 and 33 then '船电组'
            when n between 34 and 43 then '综合组'
            when n between 44 and 53 then '船体组'
            when n between 54 and 60 then '船机组'
            when n between 61 and 63 then '船电组'
            when n between 64 and 76 then '综合组'
            when n between 77 and 83 then '船体组'
            when n between 84 and 87 then '船机组'
            when n = 88 then '船电组'
            else '综合组'
        end as department,
        case
            when n between 17 and 43 then '副高级'
            when n between 44 and 76 then '中级'
            else '初级'
        end as applied_level,
        case
            when n between 17 and 43 then '中级'
            when n between 44 and 76 then '初级'
            else '员级'
        end as current_level,
        concat('SAMPLE2026', lpad(n, 4, '0')) as id_card
    from seq
)
select
    n,
    name,
    gender,
    company,
    department,
    case
        when department = '船体组' and applied_level = '副高级' then '船体设计高级工程师'
        when department = '船体组' and applied_level = '中级' then '船体制造工程师'
        when department = '船体组' then '船体助理技术员'
        when department = '船机组' and applied_level = '副高级' then '轮机高级工程师'
        when department = '船机组' and applied_level = '中级' then '轮机工程师'
        when department = '船机组' then '设备技术员'
        when department = '船电组' and applied_level = '副高级' then '电气高级工程师'
        when department = '船电组' and applied_level = '中级' then '电气工程师'
        when department = '船电组' then '电气调试员'
        when applied_level = '副高级' then '项目高级工程师'
        when applied_level = '中级' then '项目工程师'
        else '综合资料员'
    end as position,
    id_card,
    current_level,
    applied_level,
    applied_level,
    n,
    'codex',
    sysdate(),
    'range confirmation expanded sample'
from seed
where not exists (
    select 1 from Candidate c where c.id_card = seed.id_card
);

insert into ActivityCandidate(
    activity_id, candidate_id, source_type, import_seq, name, gender, birth_date, education,
    company, department, third_level_department, position, id_card, current_level,
    applied_level, level, sort_order, fixed_type, last_year_assessment, evaluation_score,
    remark, invalid_remark, create_by, create_time
)
select
    a.id,
    c.id,
    'POOL',
    c.import_seq,
    c.name,
    c.gender,
    c.birth_date,
    c.education,
    c.company,
    c.department,
    c.third_level_department,
    c.position,
    c.id_card,
    c.current_level,
    c.applied_level,
    c.level,
    c.sort_order,
    null,
    c.last_year_assessment,
    c.evaluation_score,
    c.remark,
    c.invalid_remark,
    'codex',
    sysdate()
from activity a
join Candidate c
where not exists (
    select 1
    from ActivityCandidate ac
    where ac.activity_id = a.id
      and ac.id_card = c.id_card
);

update Candidate c
join (
    select id,
           row_number() over (
               partition by applied_level, department
               order by sort_order asc, id asc
           ) as group_sort_order
    from Candidate
) ranked on ranked.id = c.id
set c.sort_order = ranked.group_sort_order,
    c.update_by = 'codex',
    c.update_time = sysdate();

update ActivityCandidate ac
join (
    select id,
           row_number() over (
               partition by activity_id, applied_level, department
               order by sort_order asc, id asc
           ) as group_sort_order
    from ActivityCandidate
) ranked on ranked.id = ac.id
set ac.sort_order = ranked.group_sort_order,
    ac.update_by = 'codex',
    ac.update_time = sysdate();

commit;
