-- Reset HR and judge login accounts to password: 123456
-- BCrypt hash generated for plaintext 123456.
update sys_user
set password = '$2b$10$.9GfpBN7/l.iIaghlWq4Z.gBBixv46bDZjd3PiM.I9Ip.hJyj7SXq',
    status = '0',
    del_flag = '0',
    update_time = now()
where user_name in ('hr01', 'hr02')
   or user_name regexp '^judge[0-9]+$';

select user_id, user_name, status, del_flag, length(password) as pwd_len, password
from sys_user
where user_name in ('hr01', 'hr02', 'judge01', 'judge25')
order by user_id;
