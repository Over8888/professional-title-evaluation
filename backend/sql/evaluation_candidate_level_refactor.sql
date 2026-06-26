-- Add current/applied level fields for title evaluation range confirmation.
-- Existing level values are kept for compatibility and copied into applied_level.

alter table Candidate
  add column current_level varchar(100) null comment 'Current title level' after id_card,
  add column applied_level varchar(100) null comment 'Applied title level' after current_level;

alter table ActivityCandidate
  add column current_level varchar(100) null comment 'Current title level snapshot' after id_card,
  add column applied_level varchar(100) null comment 'Applied title level snapshot' after current_level;

update Candidate
set applied_level = coalesce(applied_level, level),
    current_level = coalesce(current_level,
      case
        when level in ('副高级', '高级工程师') then '中级'
        when level in ('中级', '工程师') then '初级'
        when level in ('初级', '助理级', '助理工程师') then '员级'
        else current_level
      end);

update ActivityCandidate
set applied_level = coalesce(applied_level, level),
    current_level = coalesce(current_level,
      case
        when level in ('副高级', '高级工程师') then '中级'
        when level in ('中级', '工程师') then '初级'
        when level in ('初级', '助理级', '助理工程师') then '员级'
        else current_level
      end);

insert into Candidate(import_seq, name, gender, company, department, position, id_card, current_level, applied_level, level, sort_order, create_by, create_time, remark)
select seq, name, gender, company, department, position, id_card, current_level, applied_level, applied_level, seq, 'sample', sysdate(), 'range confirmation sample'
from (
  select 101 seq, '赵明' name, '男' gender, '青岛海洋装备有限公司' company, '船体组' department, '结构设计师' position, 'SAMPLE2026001' id_card, '中级' current_level, '副高级' applied_level union all
  select 102, '钱雨', '女', '青岛海洋装备有限公司', '船体组', '工艺工程师', 'SAMPLE2026002', '中级', '副高级' union all
  select 103, '孙航', '男', '青岛海洋装备有限公司', '船体组', '生产主管', 'SAMPLE2026003', '中级', '副高级' union all
  select 104, '李然', '女', '青岛海洋装备有限公司', '船体组', '质量工程师', 'SAMPLE2026004', '初级', '中级' union all
  select 105, '周宁', '男', '青岛海洋装备有限公司', '船体组', '助理工程师', 'SAMPLE2026005', '初级', '中级' union all
  select 106, '吴越', '女', '青岛海洋装备有限公司', '船机组', '轮机工程师', 'SAMPLE2026006', '中级', '副高级' union all
  select 107, '郑磊', '男', '青岛海洋装备有限公司', '船机组', '设备主管', 'SAMPLE2026007', '中级', '副高级' union all
  select 108, '王珊', '女', '青岛海洋装备有限公司', '船机组', '维修工程师', 'SAMPLE2026008', '初级', '中级' union all
  select 109, '冯涛', '男', '青岛海洋装备有限公司', '船机组', '工艺员', 'SAMPLE2026009', '员级', '初级' union all
  select 110, '陈晨', '女', '青岛海洋装备有限公司', '船电组', '电气工程师', 'SAMPLE2026010', '中级', '副高级' union all
  select 111, '褚洋', '男', '青岛海洋装备有限公司', '船电组', '自动化工程师', 'SAMPLE2026011', '初级', '中级' union all
  select 112, '卫琳', '女', '青岛海洋装备有限公司', '船电组', '调试员', 'SAMPLE2026012', '员级', '初级' union all
  select 113, '蒋峰', '男', '青岛海洋装备有限公司', '综合组', '项目经理', 'SAMPLE2026013', '中级', '副高级' union all
  select 114, '沈悦', '女', '青岛海洋装备有限公司', '综合组', '计划工程师', 'SAMPLE2026014', '中级', '副高级' union all
  select 115, '韩旭', '男', '青岛海洋装备有限公司', '综合组', '安全工程师', 'SAMPLE2026015', '初级', '中级' union all
  select 116, '杨帆', '女', '青岛海洋装备有限公司', '综合组', '资料员', 'SAMPLE2026016', '员级', '初级'
) seed
where not exists (select 1 from Candidate c where c.id_card = seed.id_card);

insert into ActivityCandidate(activity_id, candidate_id, source_type, import_seq, name, gender, company, department, position, id_card, current_level, applied_level, level, sort_order, fixed_type, create_by, create_time, remark)
select 2, c.id, 'SAMPLE', c.import_seq, c.name, c.gender, c.company, c.department, c.position, c.id_card, c.current_level, c.applied_level, c.level, c.sort_order, null, 'sample', sysdate(), c.remark
from Candidate c
where c.id_card like 'SAMPLE2026%'
  and not exists (select 1 from ActivityCandidate ac where ac.activity_id = 2 and ac.id_card = c.id_card);
