-- Rebuild all candidate data as one clean 100-row sample set.
-- This clears candidate-related vote/results because ActivityCandidate IDs are regenerated.

set foreign_key_checks = 0;

truncate table Vote;
truncate table ResultAgg;
truncate table FinalEvaluation;
truncate table ActivityCandidate;
truncate table Candidate;

set foreign_key_checks = 1;

insert into Candidate(
    import_seq, name, gender, birth_date, education, company, department,
    third_level_department, position, id_card, current_level, applied_level,
    level, sort_order, last_year_assessment, evaluation_score, remark,
    invalid_remark, create_by, create_time
)
with recursive seq(n) as (
    select 1
    union all
    select n + 1 from seq where n < 100
),
profile as (
    select
        n,
        case
            when n <= 36 then '副高级'
            when n <= 76 then '中级'
            else '初级'
        end as applied_level,
        case
            when n <= 36 then n
            when n <= 76 then n - 36
            else n - 76
        end as level_seq
    from seq
),
seed as (
    select
        n,
        applied_level,
        case
            when applied_level = '副高级' and level_seq <= 13 then '船体组'
            when applied_level = '副高级' and level_seq <= 21 then '船机组'
            when applied_level = '副高级' and level_seq <= 24 then '船电组'
            when applied_level = '副高级' then '综合组'
            when applied_level = '中级' and level_seq <= 14 then '船体组'
            when applied_level = '中级' and level_seq <= 22 then '船机组'
            when applied_level = '中级' and level_seq <= 26 then '船电组'
            when applied_level = '中级' then '综合组'
            when applied_level = '初级' and level_seq <= 7 then '船体组'
            when applied_level = '初级' and level_seq <= 12 then '船机组'
            when applied_level = '初级' and level_seq <= 15 then '船电组'
            else '综合组'
        end as department,
        case
            when applied_level = '副高级' then '中级'
            when applied_level = '中级' then '初级'
            else '员级'
        end as current_level
    from profile
),
ranked as (
    select
        n,
        applied_level,
        current_level,
        department,
        row_number() over (partition by applied_level, department order by n) as group_sort_order
    from seed
)
select
    n,
    concat(
        elt(1 + mod(n - 1, 20), '赵', '钱', '孙', '李', '周', '吴', '郑', '王', '冯', '陈', '褚', '卫', '蒋', '沈', '韩', '杨', '朱', '秦', '尤', '许'),
        elt(1 + mod(n + 6, 24), '明', '雨', '航', '然', '宁', '越', '磊', '珊', '涛', '晨', '洋', '琳', '峰', '悦', '旭', '帆', '诚', '璐', '斌', '琪', '昊', '洁', '博', '文')
    ) as name,
    case when mod(n, 2) = 0 then '女' else '男' end as gender,
    date_add('1981-01-01', interval mod(n * 137, 6200) day) as birth_date,
    case
        when applied_level = '副高级' then '本科'
        when applied_level = '中级' and mod(n, 3) = 0 then '硕士'
        when applied_level = '中级' then '本科'
        else '大专'
    end as education,
    '青岛海洋装备有限公司' as company,
    department,
    concat(department, '一部') as third_level_department,
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
    concat('CLEAN2026', lpad(n, 4, '0')) as id_card,
    current_level,
    applied_level,
    applied_level,
    group_sort_order,
    elt(1 + mod(n, 4), '优秀', '良好', '合格', '优秀') as last_year_assessment,
    cast(70 + mod(n * 7, 28) as char) as evaluation_score,
    'clean 100-row range sample' as remark,
    null as invalid_remark,
    'codex' as create_by,
    sysdate() as create_time
from ranked
order by n;

insert into ActivityCandidate(
    activity_id, candidate_id, source_type, import_seq, name, gender, birth_date,
    education, company, department, third_level_department, position, id_card,
    current_level, applied_level, level, sort_order, fixed_type,
    last_year_assessment, evaluation_score, remark, invalid_remark,
    create_by, create_time
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
cross join Candidate c
order by a.id, c.import_seq;
