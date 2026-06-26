-- Add title evaluation pages under the existing System menu.
-- Reuse the original User menu slot as Candidate Management.
-- Idempotent for the 2000-2099 evaluation menu id range.

delete from sys_role_menu where menu_id = 1006;
delete from sys_menu where menu_id = 1006;

delete from sys_role_menu where menu_id between 2000 and 2099;
delete from sys_menu where menu_id between 2000 and 2099;

update sys_menu
set menu_name = '候选人管理',
    parent_id = 1,
    order_num = 1,
    path = 'user',
    component = 'evaluation/candidate/index',
    query = null,
    route_name = 'EvaluationCandidate',
    is_frame = 1,
    is_cache = 0,
    menu_type = 'C',
    visible = '0',
    status = '0',
    perms = 'evaluation:candidate:list',
    icon = 'people',
    update_by = 'codex',
    update_time = sysdate(),
    remark = ''
where menu_id = 100;

update sys_menu
set menu_name = '候选人查询',
    parent_id = 100,
    order_num = 1,
    path = '#',
    component = null,
    query = null,
    route_name = '',
    is_frame = 1,
    is_cache = 0,
    menu_type = 'F',
    visible = '0',
    status = '0',
    perms = 'evaluation:candidate:query',
    icon = '#',
    update_by = 'codex',
    update_time = sysdate(),
    remark = ''
where menu_id = 1000;

update sys_menu
set menu_name = '候选人新增',
    parent_id = 100,
    order_num = 2,
    path = '#',
    component = null,
    query = null,
    route_name = '',
    is_frame = 1,
    is_cache = 0,
    menu_type = 'F',
    visible = '0',
    status = '0',
    perms = 'evaluation:candidate:add',
    icon = '#',
    update_by = 'codex',
    update_time = sysdate(),
    remark = ''
where menu_id = 1001;

update sys_menu
set menu_name = '候选人修改',
    parent_id = 100,
    order_num = 3,
    path = '#',
    component = null,
    query = null,
    route_name = '',
    is_frame = 1,
    is_cache = 0,
    menu_type = 'F',
    visible = '0',
    status = '0',
    perms = 'evaluation:candidate:edit',
    icon = '#',
    update_by = 'codex',
    update_time = sysdate(),
    remark = ''
where menu_id = 1002;

update sys_menu
set menu_name = '候选人删除',
    parent_id = 100,
    order_num = 4,
    path = '#',
    component = null,
    query = null,
    route_name = '',
    is_frame = 1,
    is_cache = 0,
    menu_type = 'F',
    visible = '0',
    status = '0',
    perms = 'evaluation:candidate:remove',
    icon = '#',
    update_by = 'codex',
    update_time = sysdate(),
    remark = ''
where menu_id = 1003;

update sys_menu
set menu_name = '候选人导出',
    parent_id = 100,
    order_num = 5,
    path = '#',
    component = null,
    query = null,
    route_name = '',
    is_frame = 1,
    is_cache = 0,
    menu_type = 'F',
    visible = '0',
    status = '0',
    perms = 'evaluation:candidate:export',
    icon = '#',
    update_by = 'codex',
    update_time = sysdate(),
    remark = ''
where menu_id = 1004;

update sys_menu
set menu_name = '候选人导入',
    parent_id = 100,
    order_num = 6,
    path = '#',
    component = null,
    query = null,
    route_name = '',
    is_frame = 1,
    is_cache = 0,
    menu_type = 'F',
    visible = '0',
    status = '0',
    perms = 'evaluation:candidate:import',
    icon = '#',
    update_by = 'codex',
    update_time = sysdate(),
    remark = ''
where menu_id = 1005;

insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values
  (2001, '活动管理', 1, 10, 'activity', 'evaluation/activity/index', null, 'EvaluationActivity', 1, 0, 'C', '0', '0', 'evaluation:activity:list', 'list', 'codex', sysdate(), ''),
  (2002, '配置向导', 1, 11, 'wizard', 'evaluation/wizard/index', null, 'EvaluationWizard', 1, 1, 'C', '0', '0', 'evaluation:activity:add', 'form', 'codex', sysdate(), ''),
  (2004, '评委管理', 1, 12, 'voter', 'evaluation/voter/index', null, 'EvaluationVoter', 1, 0, 'C', '0', '0', 'evaluation:voter:list', 'peoples', 'codex', sysdate(), ''),
  (2006, '范围确认', 1, 13, 'range', 'evaluation/range/index', null, 'EvaluationRange', 1, 0, 'C', '1', '0', 'evaluation:range:view', 'table', 'codex', sysdate(), ''),
  (2007, '结果统计', 1, 14, 'result', 'evaluation/result/index', null, 'EvaluationResult', 1, 0, 'C', '0', '0', 'evaluation:result:list', 'chart', 'codex', sysdate(), '');

insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
values
  (2010, '活动查询', 2001, 1, '#', null, null, '', 1, 0, 'F', '0', '0', 'evaluation:activity:query', '#', 'codex', sysdate(), ''),
  (2011, '活动新增', 2001, 2, '#', null, null, '', 1, 0, 'F', '0', '0', 'evaluation:activity:add', '#', 'codex', sysdate(), ''),
  (2012, '活动修改', 2001, 3, '#', null, null, '', 1, 0, 'F', '0', '0', 'evaluation:activity:edit', '#', 'codex', sysdate(), ''),
  (2013, '活动删除', 2001, 4, '#', null, null, '', 1, 0, 'F', '0', '0', 'evaluation:activity:remove', '#', 'codex', sysdate(), ''),
  (2014, '活动导出', 2001, 5, '#', null, null, '', 1, 0, 'F', '0', '0', 'evaluation:activity:export', '#', 'codex', sysdate(), ''),
  (2015, '活动发布', 2001, 6, '#', null, null, '', 1, 0, 'F', '0', '0', 'evaluation:activity:publish', '#', 'codex', sysdate(), ''),
  (2016, '活动关闭', 2001, 7, '#', null, null, '', 1, 0, 'F', '0', '0', 'evaluation:activity:close', '#', 'codex', sysdate(), ''),
  (2017, '结果计算', 2001, 8, '#', null, null, '', 1, 0, 'F', '0', '0', 'evaluation:activity:calculate', '#', 'codex', sysdate(), ''),
  (2018, '活动归档', 2001, 9, '#', null, null, '', 1, 0, 'F', '0', '0', 'evaluation:activity:archive', '#', 'codex', sysdate(), ''),
  (2019, '结果查询', 2007, 1, '#', null, null, '', 1, 0, 'F', '0', '0', 'evaluation:result:list', '#', 'codex', sysdate(), ''),
  (2020, '结果计算', 2007, 2, '#', null, null, '', 1, 0, 'F', '0', '0', 'evaluation:result:calculate', '#', 'codex', sysdate(), ''),
  (2021, '结果导出', 2007, 3, '#', null, null, '', 1, 0, 'F', '0', '0', 'evaluation:result:export', '#', 'codex', sysdate(), ''),
  (2022, '导出记录', 2007, 4, '#', null, null, '', 1, 0, 'F', '0', '0', 'evaluation:export:list', '#', 'codex', sysdate(), ''),
  (2030, '评委查询', 2004, 1, '#', null, null, '', 1, 0, 'F', '0', '0', 'evaluation:voter:query', '#', 'codex', sysdate(), ''),
  (2031, '评委新增', 2004, 2, '#', null, null, '', 1, 0, 'F', '0', '0', 'evaluation:voter:add', '#', 'codex', sysdate(), ''),
  (2032, '评委修改', 2004, 3, '#', null, null, '', 1, 0, 'F', '0', '0', 'evaluation:voter:edit', '#', 'codex', sysdate(), ''),
  (2033, '评委删除', 2004, 4, '#', null, null, '', 1, 0, 'F', '0', '0', 'evaluation:voter:remove', '#', 'codex', sysdate(), ''),
  (2034, '评委导入', 2004, 5, '#', null, null, '', 1, 0, 'F', '0', '0', 'evaluation:voter:import', '#', 'codex', sysdate(), ''),
  (2035, '评委导出', 2004, 6, '#', null, null, '', 1, 0, 'F', '0', '0', 'evaluation:voter:export', '#', 'codex', sysdate(), ''),
  (2036, '重置投票口令', 2004, 7, '#', null, null, '', 1, 0, 'F', '0', '0', 'evaluation:voter:token', '#', 'codex', sysdate(), ''),
  (2040, '规则查询', 2002, 1, '#', null, null, '', 1, 0, 'F', '0', '0', 'evaluation:rule:query', '#', 'codex', sysdate(), ''),
  (2041, '规则新增', 2002, 2, '#', null, null, '', 1, 0, 'F', '0', '0', 'evaluation:rule:add', '#', 'codex', sysdate(), ''),
  (2042, '规则修改', 2002, 3, '#', null, null, '', 1, 0, 'F', '0', '0', 'evaluation:rule:edit', '#', 'codex', sysdate(), ''),
  (2043, '规则删除', 2002, 4, '#', null, null, '', 1, 0, 'F', '0', '0', 'evaluation:rule:remove', '#', 'codex', sysdate(), ''),
  (2044, '规则预览', 2002, 5, '#', null, null, '', 1, 0, 'F', '0', '0', 'evaluation:rule:preview', '#', 'codex', sysdate(), ''),
  (2050, '范围查看', 2006, 1, '#', null, null, '', 1, 0, 'F', '0', '0', 'evaluation:range:view', '#', 'codex', sysdate(), '');

insert into sys_role_menu(role_id, menu_id)
select r.role_id, m.menu_id
from sys_role r
join sys_menu m on m.menu_id between 100 and 1005
where r.role_key = 'common'
  and not exists (
    select 1 from sys_role_menu rm where rm.role_id = r.role_id and rm.menu_id = m.menu_id
  );

insert into sys_role_menu(role_id, menu_id)
select r.role_id, m.menu_id
from sys_role r
join sys_menu m on m.menu_id between 2000 and 2099
where r.role_key = 'common'
  and not exists (
    select 1 from sys_role_menu rm where rm.role_id = r.role_id and rm.menu_id = m.menu_id
  );
