-- Refactor candidate master/activity snapshot columns for candidate management.
-- Keep third_level_department, drop second_level_department and compatible level fields.

set @schema_name = database();

set @sql = (
  select if(count(*) = 0,
    'alter table Candidate add column first_level_department varchar(200) null comment ''First level department'' after company',
    'select 1'
  )
  from information_schema.columns
  where table_schema = @schema_name and table_name = 'Candidate' and column_name = 'first_level_department'
);
prepare stmt from @sql;
execute stmt;
deallocate prepare stmt;

set @sql = (
  select if(count(*) = 0,
    'alter table ActivityCandidate add column first_level_department varchar(200) null comment ''First level department snapshot'' after company',
    'select 1'
  )
  from information_schema.columns
  where table_schema = @schema_name and table_name = 'ActivityCandidate' and column_name = 'first_level_department'
);
prepare stmt from @sql;
execute stmt;
deallocate prepare stmt;

set @sql = (
  select if(count(*) > 0,
    'update Candidate set applied_level = level where applied_level is null or length(applied_level) = 0',
    'select 1'
  )
  from information_schema.columns
  where table_schema = @schema_name and table_name = 'Candidate' and column_name = 'level'
);
prepare stmt from @sql;
execute stmt;
deallocate prepare stmt;

set @sql = (
  select if(count(*) > 0,
    'update ActivityCandidate set applied_level = level where applied_level is null or length(applied_level) = 0',
    'select 1'
  )
  from information_schema.columns
  where table_schema = @schema_name and table_name = 'ActivityCandidate' and column_name = 'level'
);
prepare stmt from @sql;
execute stmt;
deallocate prepare stmt;

update Candidate
set third_level_department = concat(coalesce(department, ''), '(', coalesce(current_level, ''), ')');

update ActivityCandidate
set third_level_department = concat(coalesce(department, ''), '(', coalesce(current_level, ''), ')');

set @sql = (
  select if(count(*) > 0, 'alter table Candidate drop column second_level_department', 'select 1')
  from information_schema.columns
  where table_schema = @schema_name and table_name = 'Candidate' and column_name = 'second_level_department'
);
prepare stmt from @sql;
execute stmt;
deallocate prepare stmt;

set @sql = (
  select if(count(*) > 0, 'alter table Candidate drop column level', 'select 1')
  from information_schema.columns
  where table_schema = @schema_name and table_name = 'Candidate' and column_name = 'level'
);
prepare stmt from @sql;
execute stmt;
deallocate prepare stmt;

set @sql = (
  select if(count(*) > 0, 'alter table ActivityCandidate drop column second_level_department', 'select 1')
  from information_schema.columns
  where table_schema = @schema_name and table_name = 'ActivityCandidate' and column_name = 'second_level_department'
);
prepare stmt from @sql;
execute stmt;
deallocate prepare stmt;

set @sql = (
  select if(count(*) > 0, 'alter table ActivityCandidate drop column level', 'select 1')
  from information_schema.columns
  where table_schema = @schema_name and table_name = 'ActivityCandidate' and column_name = 'level'
);
prepare stmt from @sql;
execute stmt;
deallocate prepare stmt;
