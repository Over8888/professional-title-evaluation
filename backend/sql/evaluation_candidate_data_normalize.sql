-- Normalize candidate sample data for title evaluation range confirmation.
-- The business display order is: applied level first, then group, then in-group sequence.

start transaction;

update Candidate
set name = case id_card
        when '12345678910' then '林静'
        when '12345678912' then '高远'
        when '12345678913' then '王嘉伦'
        when '12345678914' then '陆敏'
        when '12345678915' then '郭伟'
        else name
    end,
    gender = case id_card
        when '12345678910' then '女'
        when '12345678912' then '男'
        when '12345678913' then '男'
        when '12345678914' then '女'
        when '12345678915' then '男'
        else gender
    end,
    company = case
        when id_card in ('12345678910', '12345678912', '12345678913', '12345678914', '12345678915') then '青岛海洋装备有限公司'
        else company
    end,
    position = case id_card
        when '12345678910' then '电气调试员'
        when '12345678912' then '结构助理工程师'
        when '12345678913' then '船体制造工程师'
        when '12345678914' then '船体设计主管'
        when '12345678915' then '综合资料员'
        else position
    end,
    department = case department
        when '船首组' then '船体组'
        when '船尾组' then '船体组'
        else department
    end,
    current_level = case
        when current_level is not null and current_level <> '' then current_level
        when coalesce(applied_level, level) in ('副高级', '高级') then '中级'
        when coalesce(applied_level, level) in ('中级', '技术员') then '初级'
        when coalesce(applied_level, level) in ('初级', '杂工', '助理级') then '员级'
        else current_level
    end,
    applied_level = case coalesce(applied_level, level)
        when '高级' then '副高级'
        when '技术员' then '中级'
        when '杂工' then '初级'
        when '助理级' then '初级'
        else coalesce(applied_level, level)
    end,
    update_by = 'codex',
    update_time = sysdate()
where id_card in ('12345678910', '12345678912', '12345678913', '12345678914', '12345678915')
   or department in ('船首组', '船尾组')
   or coalesce(applied_level, level) in ('高级', '技术员', '杂工', '助理级');

update Candidate
set level = applied_level,
    update_by = 'codex',
    update_time = sysdate()
where applied_level is not null
  and (level is null or level <> applied_level);

update ActivityCandidate
set name = case id_card
        when '12345678910' then '林静'
        when '12345678912' then '高远'
        when '12345678913' then '王嘉伦'
        when '12345678914' then '陆敏'
        when '12345678915' then '郭伟'
        else name
    end,
    gender = case id_card
        when '12345678910' then '女'
        when '12345678912' then '男'
        when '12345678913' then '男'
        when '12345678914' then '女'
        when '12345678915' then '男'
        else gender
    end,
    company = case
        when id_card in ('12345678910', '12345678912', '12345678913', '12345678914', '12345678915') then '青岛海洋装备有限公司'
        else company
    end,
    position = case id_card
        when '12345678910' then '电气调试员'
        when '12345678912' then '结构助理工程师'
        when '12345678913' then '船体制造工程师'
        when '12345678914' then '船体设计主管'
        when '12345678915' then '综合资料员'
        else position
    end,
    department = case department
        when '船首组' then '船体组'
        when '船尾组' then '船体组'
        else department
    end,
    current_level = case
        when current_level is not null and current_level <> '' then current_level
        when coalesce(applied_level, level) in ('副高级', '高级') then '中级'
        when coalesce(applied_level, level) in ('中级', '技术员') then '初级'
        when coalesce(applied_level, level) in ('初级', '杂工', '助理级') then '员级'
        else current_level
    end,
    applied_level = case coalesce(applied_level, level)
        when '高级' then '副高级'
        when '技术员' then '中级'
        when '杂工' then '初级'
        when '助理级' then '初级'
        else coalesce(applied_level, level)
    end,
    update_by = 'codex',
    update_time = sysdate()
where id_card in ('12345678910', '12345678912', '12345678913', '12345678914', '12345678915')
   or department in ('船首组', '船尾组')
   or coalesce(applied_level, level) in ('高级', '技术员', '杂工', '助理级');

update ActivityCandidate
set level = applied_level,
    update_by = 'codex',
    update_time = sysdate()
where applied_level is not null
  and (level is null or level <> applied_level);

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
