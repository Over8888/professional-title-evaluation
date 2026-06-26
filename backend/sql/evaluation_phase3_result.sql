-- Phase 3 result calculation and export support.
-- Safe to run after evaluation_phase1.sql.

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

delete from sys_role_menu where menu_id between 2060 and 2069;
delete from sys_menu where menu_id between 2060 and 2069;

insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values
  (2007, '结果统计', 1, 4, 'result', 'evaluation/result/index', null, 'EvaluationResult', 1, 0, 'C', '0', '0', 'evaluation:result:list', 'chart', 'codex', sysdate(), 'codex', sysdate(), '阶段3结果统计'),
  (2019, '结果查询', 2007, 1, '#', null, null, '', 1, 0, 'F', '0', '0', 'evaluation:result:list', '#', 'codex', sysdate(), 'codex', sysdate(), ''),
  (2020, '结果计算', 2007, 2, '#', null, null, '', 1, 0, 'F', '0', '0', 'evaluation:result:calculate', '#', 'codex', sysdate(), 'codex', sysdate(), ''),
  (2021, '结果导出', 2007, 3, '#', null, null, '', 1, 0, 'F', '0', '0', 'evaluation:result:export', '#', 'codex', sysdate(), 'codex', sysdate(), ''),
  (2022, '导出记录', 2007, 4, '#', null, null, '', 1, 0, 'F', '0', '0', 'evaluation:export:list', '#', 'codex', sysdate(), 'codex', sysdate(), '')
on duplicate key update
  menu_name = values(menu_name),
  parent_id = values(parent_id),
  order_num = values(order_num),
  path = values(path),
  component = values(component),
  query = values(query),
  route_name = values(route_name),
  is_frame = values(is_frame),
  is_cache = values(is_cache),
  menu_type = values(menu_type),
  visible = values(visible),
  status = values(status),
  perms = values(perms),
  icon = values(icon),
  update_by = 'codex',
  update_time = sysdate(),
  remark = values(remark);

insert into sys_role_menu(role_id, menu_id)
select 1, menu_id from sys_menu where menu_id in (2007, 2019, 2020, 2021, 2022)
on duplicate key update menu_id = values(menu_id);
