-- Hide the standalone range confirmation menu from the left sidebar.
update sys_menu
set visible = '1',
    update_by = 'codex',
    update_time = sysdate()
where menu_id = 2006
   or parent_id = 2006
   or path = 'range'
   or component = 'evaluation/range/index'
   or perms = 'evaluation:range:view';
