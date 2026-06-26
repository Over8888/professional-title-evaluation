-- Cleanup unused template features for the evaluation MVP.
-- Keep reusable foundation: login, users, roles, menus, dict/config, logs, Excel/file, Druid datasource config.

delete from sys_role_menu
where menu_id in (
  2, 3, 4,
  109, 110, 111, 112, 113, 114, 115, 116, 117,
  1046, 1047, 1048, 1049, 1050, 1051, 1052, 1053, 1054, 1055, 1056, 1057, 1058, 1059, 1060
);

delete from sys_menu
where menu_id in (
  2, 3, 4,
  109, 110, 111, 112, 113, 114, 115, 116, 117,
  1046, 1047, 1048, 1049, 1050, 1051, 1052, 1053, 1054, 1055, 1056, 1057, 1058, 1059, 1060
);

delete from sys_dict_data
where dict_type in ('sys_job_status', 'sys_job_group');

delete from sys_dict_type
where dict_type in ('sys_job_status', 'sys_job_group');

drop table if exists sys_job_log;
drop table if exists sys_job;
drop table if exists gen_table_column;
drop table if exists gen_table;
