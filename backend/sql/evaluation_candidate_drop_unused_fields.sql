-- Drop unused candidate ordering/remark columns.
-- Candidate and ActivityCandidate now use import_seq for ordering.

set @schema_name := database();

set @sql := (
  select if(count(*) > 0, 'alter table Candidate drop column sort_order', 'select 1')
  from information_schema.columns
  where table_schema = @schema_name and table_name = 'Candidate' and column_name = 'sort_order'
);
prepare stmt from @sql;
execute stmt;
deallocate prepare stmt;

set @sql := (
  select if(count(*) > 0, 'alter table Candidate drop column remark', 'select 1')
  from information_schema.columns
  where table_schema = @schema_name and table_name = 'Candidate' and column_name = 'remark'
);
prepare stmt from @sql;
execute stmt;
deallocate prepare stmt;

set @sql := (
  select if(count(*) > 0, 'alter table Candidate drop column invalid_remark', 'select 1')
  from information_schema.columns
  where table_schema = @schema_name and table_name = 'Candidate' and column_name = 'invalid_remark'
);
prepare stmt from @sql;
execute stmt;
deallocate prepare stmt;

set @sql := (
  select if(count(*) > 0, 'alter table ActivityCandidate drop column sort_order', 'select 1')
  from information_schema.columns
  where table_schema = @schema_name and table_name = 'ActivityCandidate' and column_name = 'sort_order'
);
prepare stmt from @sql;
execute stmt;
deallocate prepare stmt;

set @sql := (
  select if(count(*) > 0, 'alter table ActivityCandidate drop column remark', 'select 1')
  from information_schema.columns
  where table_schema = @schema_name and table_name = 'ActivityCandidate' and column_name = 'remark'
);
prepare stmt from @sql;
execute stmt;
deallocate prepare stmt;

set @sql := (
  select if(count(*) > 0, 'alter table ActivityCandidate drop column invalid_remark', 'select 1')
  from information_schema.columns
  where table_schema = @schema_name and table_name = 'ActivityCandidate' and column_name = 'invalid_remark'
);
prepare stmt from @sql;
execute stmt;
deallocate prepare stmt;
