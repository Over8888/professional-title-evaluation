-- Clean menu, role, account, voter, and candidate structures for title evaluation.
-- Idempotent. Run with --default-character-set=utf8mb4.

set @schema_name = database();

set @sql = (
  select if(count(*) = 0,
    'alter table candidate add column first_level_department varchar(200) null comment ''First level department/import organization'' after company',
    'select 1')
  from information_schema.columns
  where table_schema = @schema_name and table_name = 'candidate' and column_name = 'first_level_department'
);
prepare stmt from @sql;
execute stmt;
deallocate prepare stmt;

set @sql = (
  select if(count(*) = 0,
    'alter table candidate add column second_level_department varchar(200) null comment ''Second level department/import group'' after first_level_department',
    'select 1')
  from information_schema.columns
  where table_schema = @schema_name and table_name = 'candidate' and column_name = 'second_level_department'
);
prepare stmt from @sql;
execute stmt;
deallocate prepare stmt;

set @sql = (
  select if(count(*) = 0,
    'alter table activitycandidate add column first_level_department varchar(200) null comment ''First level department snapshot'' after company',
    'select 1')
  from information_schema.columns
  where table_schema = @schema_name and table_name = 'activitycandidate' and column_name = 'first_level_department'
);
prepare stmt from @sql;
execute stmt;
deallocate prepare stmt;

set @sql = (
  select if(count(*) = 0,
    'alter table activitycandidate add column second_level_department varchar(200) null comment ''Second level department snapshot'' after first_level_department',
    'select 1')
  from information_schema.columns
  where table_schema = @schema_name and table_name = 'activitycandidate' and column_name = 'second_level_department'
);
prepare stmt from @sql;
execute stmt;
deallocate prepare stmt;

update candidate
set first_level_department = coalesce(nullif(first_level_department, ''), '2026年工程系列职称评审'),
    second_level_department = coalesce(nullif(second_level_department, ''), department),
    third_level_department = coalesce(nullif(third_level_department, ''), concat(department, '（',
      case coalesce(applied_level, level)
        when '副高级' then '高级工程师'
        when '中级' then '工程师'
        when '初级' then '助理工程师'
        else coalesce(applied_level, level, '职称')
      end, '）')),
    position = case
      when position is null or position = '' or position like '%工程师' then
        case department
          when '船体组' then '技术研究院设计部'
          when '船机组' then '技术研究院研发部'
          when '船电组' then '总装工程部'
          when '综合组' then '生产管理部'
          else coalesce(position, '')
        end
      else position
    end,
    company = coalesce(nullif(company, ''), '青岛海洋装备有限公司'),
    update_by = 'codex',
    update_time = sysdate();

update activitycandidate ac
join candidate c on c.id = ac.candidate_id
set ac.first_level_department = coalesce(nullif(ac.first_level_department, ''), c.first_level_department),
    ac.second_level_department = coalesce(nullif(ac.second_level_department, ''), c.second_level_department),
    ac.third_level_department = coalesce(nullif(ac.third_level_department, ''), c.third_level_department),
    ac.position = coalesce(nullif(ac.position, ''), c.position),
    ac.update_by = 'codex',
    ac.update_time = sysdate();

update activitycandidate
set first_level_department = coalesce(nullif(first_level_department, ''), '2026年工程系列职称评审'),
    second_level_department = coalesce(nullif(second_level_department, ''), department),
    third_level_department = coalesce(nullif(third_level_department, ''), concat(department, '（',
      case coalesce(applied_level, level)
        when '副高级' then '高级工程师'
        when '中级' then '工程师'
        when '初级' then '助理工程师'
        else coalesce(applied_level, level, '职称')
      end, '）')),
    position = case
      when position is null or position = '' or position like '%工程师' then
        case department
          when '船体组' then '技术研究院设计部'
          when '船机组' then '技术研究院研发部'
          when '船电组' then '总装工程部'
          when '综合组' then '生产管理部'
          else coalesce(position, '')
        end
      else position
    end,
    update_by = 'codex',
    update_time = sysdate();

-- Roles: keep original role keys for compatibility, rename the displayed roles.
update sys_role
set role_name = 'HR',
    role_sort = 1,
    data_scope = '1',
    status = '0',
    del_flag = '0',
    update_by = 'codex',
    update_time = sysdate(),
    remark = 'HR，对应原超级管理员'
where role_id = 1;

update sys_role
set role_name = '评委',
    role_sort = 2,
    data_scope = '2',
    status = '0',
    del_flag = '0',
    update_by = 'codex',
    update_time = sysdate(),
    remark = '评委，对应原普通角色'
where role_id = 2;

-- Hide/disable unneeded original menus. Notice remains active.
update sys_menu
set visible = '1', status = '1', update_by = 'codex', update_time = sysdate()
where menu_id in (101,102,103,104,105,106,108,500,501);

update sys_menu
set visible = '1', status = '1', update_by = 'codex', update_time = sysdate()
where parent_id in (101,102,103,104,105,106,108,500,501);

-- Make the existing system root the evaluation sidebar root.
update sys_menu
set menu_name = '职称评审',
    parent_id = 0,
    order_num = 1,
    path = 'system',
    component = null,
    query = null,
    route_name = 'System',
    is_frame = 1,
    is_cache = 0,
    menu_type = 'M',
    visible = '0',
    status = '0',
    perms = '',
    icon = 'system',
    update_by = 'codex',
    update_time = sysdate(),
    remark = '职称评审主菜单'
where menu_id = 1;

delete from sys_role_menu where menu_id between 2000 and 2099;
delete from sys_menu where menu_id between 2000 and 2099;

update sys_menu
set menu_name = '候选人管理',
    parent_id = 1,
    order_num = 2,
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
    remark = '候选人管理'
where menu_id = 100;

update sys_menu
set menu_name = case menu_id
      when 1000 then '候选人查询'
      when 1001 then '候选人新增'
      when 1002 then '候选人修改'
      when 1003 then '候选人删除'
      when 1004 then '候选人导出'
      when 1005 then '候选人导入'
    end,
    parent_id = 100,
    order_num = case menu_id
      when 1000 then 1
      when 1001 then 2
      when 1002 then 3
      when 1003 then 4
      when 1004 then 5
      when 1005 then 6
    end,
    path = '#',
    component = null,
    query = null,
    route_name = '',
    is_frame = 1,
    is_cache = 0,
    menu_type = 'F',
    visible = '0',
    status = '0',
    perms = case menu_id
      when 1000 then 'evaluation:candidate:query'
      when 1001 then 'evaluation:candidate:add'
      when 1002 then 'evaluation:candidate:edit'
      when 1003 then 'evaluation:candidate:remove'
      when 1004 then 'evaluation:candidate:export'
      when 1005 then 'evaluation:candidate:import'
    end,
    icon = '#',
    update_by = 'codex',
    update_time = sysdate()
where menu_id between 1000 and 1005;

insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values
  (2001, '活动管理', 1, 1, 'activity', '', null, 'EvaluationActivityRoot', 1, 0, 'M', '0', '0', '', 'list', 'codex', sysdate(), 'codex', sysdate(), '活动管理目录'),
  (2002, '配置向导', 2001, 1, 'wizard', 'evaluation/wizard/index', null, 'EvaluationWizard', 1, 1, 'C', '0', '0', 'evaluation:activity:add', 'form', 'codex', sysdate(), 'codex', sysdate(), ''),
  (2003, '活动列表', 2001, 2, 'index', 'evaluation/activity/index', null, 'EvaluationActivity', 1, 0, 'C', '0', '0', 'evaluation:activity:list', 'list', 'codex', sysdate(), 'codex', sysdate(), ''),
  (2004, '评委管理', 1, 3, 'voter', 'evaluation/voter/index', null, 'EvaluationVoter', 1, 0, 'C', '0', '0', 'evaluation:voter:list', 'peoples', 'codex', sysdate(), 'codex', sysdate(), ''),
  (2006, '范围确认', 2001, 3, 'range', 'evaluation/range/index', null, 'EvaluationRange', 1, 0, 'C', '1', '0', 'evaluation:range:view', 'table', 'codex', sysdate(), 'codex', sysdate(), ''),
  (2007, '结果统计', 1, 4, 'result', 'evaluation/result/index', null, 'EvaluationResult', 1, 0, 'C', '0', '0', 'evaluation:result:list', 'chart', 'codex', sysdate(), 'codex', sysdate(), '')
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

insert into sys_menu(menu_id, menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, update_by, update_time, remark)
values
  (2010, '活动查询', 2003, 1, '#', null, null, '', 1, 0, 'F', '0', '0', 'evaluation:activity:query', '#', 'codex', sysdate(), 'codex', sysdate(), ''),
  (2011, '活动新增', 2003, 2, '#', null, null, '', 1, 0, 'F', '0', '0', 'evaluation:activity:add', '#', 'codex', sysdate(), 'codex', sysdate(), ''),
  (2012, '活动修改', 2003, 3, '#', null, null, '', 1, 0, 'F', '0', '0', 'evaluation:activity:edit', '#', 'codex', sysdate(), 'codex', sysdate(), ''),
  (2013, '活动删除', 2003, 4, '#', null, null, '', 1, 0, 'F', '0', '0', 'evaluation:activity:remove', '#', 'codex', sysdate(), 'codex', sysdate(), ''),
  (2014, '活动导出', 2003, 5, '#', null, null, '', 1, 0, 'F', '0', '0', 'evaluation:activity:export', '#', 'codex', sysdate(), 'codex', sysdate(), ''),
  (2015, '活动发布', 2003, 6, '#', null, null, '', 1, 0, 'F', '0', '0', 'evaluation:activity:publish', '#', 'codex', sysdate(), 'codex', sysdate(), ''),
  (2016, '活动关闭', 2003, 7, '#', null, null, '', 1, 0, 'F', '0', '0', 'evaluation:activity:close', '#', 'codex', sysdate(), 'codex', sysdate(), ''),
  (2017, '结果计算', 2003, 8, '#', null, null, '', 1, 0, 'F', '0', '0', 'evaluation:activity:calculate', '#', 'codex', sysdate(), 'codex', sysdate(), ''),
  (2018, '活动归档', 2003, 9, '#', null, null, '', 1, 0, 'F', '0', '0', 'evaluation:activity:archive', '#', 'codex', sysdate(), 'codex', sysdate(), ''),
  (2019, '结果查询', 2007, 1, '#', null, null, '', 1, 0, 'F', '0', '0', 'evaluation:result:list', '#', 'codex', sysdate(), 'codex', sysdate(), ''),
  (2020, '结果计算', 2007, 2, '#', null, null, '', 1, 0, 'F', '0', '0', 'evaluation:result:calculate', '#', 'codex', sysdate(), 'codex', sysdate(), ''),
  (2021, '结果导出', 2007, 3, '#', null, null, '', 1, 0, 'F', '0', '0', 'evaluation:result:export', '#', 'codex', sysdate(), 'codex', sysdate(), ''),
  (2022, '导出记录', 2007, 4, '#', null, null, '', 1, 0, 'F', '0', '0', 'evaluation:export:list', '#', 'codex', sysdate(), 'codex', sysdate(), ''),
  (2030, '评委查询', 2004, 1, '#', null, null, '', 1, 0, 'F', '0', '0', 'evaluation:voter:query', '#', 'codex', sysdate(), 'codex', sysdate(), ''),
  (2031, '评委新增', 2004, 2, '#', null, null, '', 1, 0, 'F', '0', '0', 'evaluation:voter:add', '#', 'codex', sysdate(), 'codex', sysdate(), ''),
  (2032, '评委修改', 2004, 3, '#', null, null, '', 1, 0, 'F', '0', '0', 'evaluation:voter:edit', '#', 'codex', sysdate(), 'codex', sysdate(), ''),
  (2033, '评委删除', 2004, 4, '#', null, null, '', 1, 0, 'F', '0', '0', 'evaluation:voter:remove', '#', 'codex', sysdate(), 'codex', sysdate(), ''),
  (2034, '评委导入', 2004, 5, '#', null, null, '', 1, 0, 'F', '0', '0', 'evaluation:voter:import', '#', 'codex', sysdate(), 'codex', sysdate(), ''),
  (2035, '评委导出', 2004, 6, '#', null, null, '', 1, 0, 'F', '0', '0', 'evaluation:voter:export', '#', 'codex', sysdate(), 'codex', sysdate(), ''),
  (2036, '重置投票口令', 2004, 7, '#', null, null, '', 1, 0, 'F', '0', '0', 'evaluation:voter:token', '#', 'codex', sysdate(), 'codex', sysdate(), ''),
  (2040, '规则查询', 2002, 10, '#', null, null, '', 1, 0, 'F', '0', '0', 'evaluation:rule:query', '#', 'codex', sysdate(), 'codex', sysdate(), ''),
  (2041, '规则新增', 2002, 11, '#', null, null, '', 1, 0, 'F', '0', '0', 'evaluation:rule:add', '#', 'codex', sysdate(), 'codex', sysdate(), ''),
  (2042, '规则修改', 2002, 12, '#', null, null, '', 1, 0, 'F', '0', '0', 'evaluation:rule:edit', '#', 'codex', sysdate(), 'codex', sysdate(), ''),
  (2043, '规则删除', 2002, 13, '#', null, null, '', 1, 0, 'F', '0', '0', 'evaluation:rule:remove', '#', 'codex', sysdate(), 'codex', sysdate(), ''),
  (2044, '规则预览', 2002, 14, '#', null, null, '', 1, 0, 'F', '0', '0', 'evaluation:rule:preview', '#', 'codex', sysdate(), 'codex', sysdate(), ''),
  (2050, '范围查看', 2006, 1, '#', null, null, '', 1, 0, 'F', '0', '0', 'evaluation:range:view', '#', 'codex', sysdate(), 'codex', sysdate(), '')
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
  update_time = sysdate();

-- Notice stays active, after requested business menus.
update sys_menu
set parent_id = 1,
    order_num = 5,
    visible = '0',
    status = '0',
    update_by = 'codex',
    update_time = sysdate()
where menu_id = 107;

-- HR gets all active business and notice permissions. Evaluator gets read-oriented access.
delete from sys_role_menu where role_id in (1, 2);

insert into sys_role_menu(role_id, menu_id)
select 1, menu_id
from sys_menu
where status = '0'
  and (menu_id = 1 or menu_id between 100 and 1005 or menu_id = 107 or parent_id = 107 or menu_id between 2000 and 2099);

insert into sys_role_menu(role_id, menu_id)
select 2, menu_id
from sys_menu
where status = '0'
  and menu_id in (1, 100, 1000, 107, 1035, 2001, 2004, 2030, 2035, 2006, 2050);

-- HR accounts: keep original admin, convert original ry into HR02.
update sys_user
set user_name = 'hr01',
    nick_name = 'HR01',
    email = 'hr01@example.com',
    phonenumber = '15888888888',
    status = '0',
    del_flag = '0',
    update_by = 'codex',
    update_time = sysdate(),
    remark = 'HR账号，对应原超级管理员'
where user_id = 1;

update sys_user
set user_name = 'hr02',
    nick_name = 'HR02',
    email = 'hr02@example.com',
    phonenumber = '15888888889',
    status = '0',
    del_flag = '0',
    update_by = 'codex',
    update_time = sysdate(),
    remark = 'HR账号，对应原超级管理员'
where user_id = 2;

delete from sys_user_role where user_id in (1, 2);
insert into sys_user_role(user_id, role_id) values (1, 1), (2, 1);

insert into sys_user_post(user_id, post_id)
values (1, 3), (2, 3)
on duplicate key update post_id = values(post_id);

-- Voter accounts and master voter records.
delete ur from sys_user_role ur
join sys_user u on u.user_id = ur.user_id
where u.user_name regexp '^judge[0-9]{2}$';

delete up from sys_user_post up
join sys_user u on u.user_id = up.user_id
where u.user_name regexp '^judge[0-9]{2}$';

delete from sys_user
where user_name regexp '^judge[0-9]{2}$';

delete from voter;

insert into voter(id, name, employee_id, department, create_by, create_time, remark)
values
  (1, '评委01', 'JUDGE001', '船体组', 'codex', sysdate(), '评委账号 judge01'),
  (2, '评委02', 'JUDGE002', '船体组', 'codex', sysdate(), '评委账号 judge02'),
  (3, '评委03', 'JUDGE003', '船体组', 'codex', sysdate(), '评委账号 judge03'),
  (4, '评委04', 'JUDGE004', '船体组', 'codex', sysdate(), '评委账号 judge04'),
  (5, '评委05', 'JUDGE005', '船体组', 'codex', sysdate(), '评委账号 judge05'),
  (6, '评委06', 'JUDGE006', '船体组', 'codex', sysdate(), '评委账号 judge06'),
  (7, '评委07', 'JUDGE007', '船机组', 'codex', sysdate(), '评委账号 judge07'),
  (8, '评委08', 'JUDGE008', '船机组', 'codex', sysdate(), '评委账号 judge08'),
  (9, '评委09', 'JUDGE009', '船机组', 'codex', sysdate(), '评委账号 judge09'),
  (10, '评委10', 'JUDGE010', '船机组', 'codex', sysdate(), '评委账号 judge10'),
  (11, '评委11', 'JUDGE011', '船机组', 'codex', sysdate(), '评委账号 judge11'),
  (12, '评委12', 'JUDGE012', '船机组', 'codex', sysdate(), '评委账号 judge12'),
  (13, '评委13', 'JUDGE013', '船电组', 'codex', sysdate(), '评委账号 judge13'),
  (14, '评委14', 'JUDGE014', '船电组', 'codex', sysdate(), '评委账号 judge14'),
  (15, '评委15', 'JUDGE015', '船电组', 'codex', sysdate(), '评委账号 judge15'),
  (16, '评委16', 'JUDGE016', '船电组', 'codex', sysdate(), '评委账号 judge16'),
  (17, '评委17', 'JUDGE017', '船电组', 'codex', sysdate(), '评委账号 judge17'),
  (18, '评委18', 'JUDGE018', '船电组', 'codex', sysdate(), '评委账号 judge18'),
  (19, '评委19', 'JUDGE019', '综合组', 'codex', sysdate(), '评委账号 judge19'),
  (20, '评委20', 'JUDGE020', '综合组', 'codex', sysdate(), '评委账号 judge20'),
  (21, '评委21', 'JUDGE021', '综合组', 'codex', sysdate(), '评委账号 judge21'),
  (22, '评委22', 'JUDGE022', '综合组', 'codex', sysdate(), '评委账号 judge22'),
  (23, '评委23', 'JUDGE023', '综合组', 'codex', sysdate(), '评委账号 judge23'),
  (24, '评委24', 'JUDGE024', '综合组', 'codex', sysdate(), '评委账号 judge24'),
  (25, '评委25', 'JUDGE025', '综合组', 'codex', sysdate(), '评委账号 judge25')
on duplicate key update
  name = values(name),
  employee_id = values(employee_id),
  department = values(department),
  update_by = 'codex',
  update_time = sysdate(),
  remark = values(remark);

insert into sys_user(user_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex, avatar, password, status, del_flag, login_ip, login_date, pwd_update_date, create_by, create_time, update_by, update_time, remark)
select 2000 + v.id,
       105,
       concat('judge', lpad(v.id, 2, '0')),
       v.name,
       '00',
       concat('judge', lpad(v.id, 2, '0'), '@example.com'),
       concat('1560000', lpad(v.id, 4, '0')),
       '2',
       '',
       '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2',
       '0',
       '0',
       '',
       null,
       sysdate(),
       'codex',
       sysdate(),
       'codex',
       sysdate(),
       '评委账号，对应原普通角色'
from voter v
on duplicate key update
  user_name = values(user_name),
  nick_name = values(nick_name),
  email = values(email),
  phonenumber = values(phonenumber),
  status = '0',
  del_flag = '0',
  update_by = 'codex',
  update_time = sysdate(),
  remark = values(remark);

insert into sys_user_role(user_id, role_id)
select 2000 + id, 2 from voter
on duplicate key update role_id = values(role_id);

insert into sys_user_post(user_id, post_id)
select 2000 + id, 4 from voter
on duplicate key update post_id = values(post_id);

-- Keep manager compatibility mapping for HR accounts.
insert into manager(user_id, username, manager_type, create_time, remark)
select user_id, user_name, 'HR', sysdate(), 'HR uses sys_user/sys_role for authentication.'
from sys_user
where user_id in (1, 2)
on duplicate key update
  username = values(username),
  manager_type = values(manager_type),
  remark = values(remark);

-- Rebuild voter snapshots for existing title-review activities to align with the 25-voter master list.
delete from activityvoter;

insert into activityvoter(activity_id, voter_id, source_type, name, employee_id, department, status, vote_token, create_by, create_time, remark)
select a.id,
       v.id,
       'POOL',
       v.name,
       v.employee_id,
       v.department,
       'PENDING',
       lower(hex(md5(concat(a.id, '-', v.id, '-', v.employee_id)))),
       'codex',
       sysdate(),
       v.remark
from activity a
join voter v
where a.type = 'TITLE_REVIEW';
