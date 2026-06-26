-- Allow sorted ranking files to contain the same ID card in different group/title scopes.
-- Run this after backing up the database and after ActivityCandidate has department/applied_level columns.

set @schema_name = database();
set @target_scope_columns = 'activity_id,department,applied_level,id_card';

set @sql = (
  select if(count(*) > 0,
    'alter table ActivityCandidate drop index uk_activity_candidate_id_card',
    'select 1')
  from information_schema.statistics
  where table_schema = @schema_name
    and table_name = 'ActivityCandidate'
    and index_name = 'uk_activity_candidate_id_card'
);
prepare stmt from @sql;
execute stmt;
deallocate prepare stmt;

set @scope_columns = (
  select group_concat(column_name order by seq_in_index separator ',')
  from information_schema.statistics
  where table_schema = @schema_name
    and table_name = 'ActivityCandidate'
    and index_name = 'uk_activity_candidate_scope'
);

set @sql = if(@scope_columns is not null and @scope_columns <> @target_scope_columns,
  'alter table ActivityCandidate drop index uk_activity_candidate_scope',
  'select 1');
prepare stmt from @sql;
execute stmt;
deallocate prepare stmt;

set @sql = (
  select if(count(*) = 0,
    'alter table ActivityCandidate add unique key uk_activity_candidate_scope (activity_id, department, applied_level, id_card)',
    'select 1')
  from information_schema.statistics
  where table_schema = @schema_name
    and table_name = 'ActivityCandidate'
    and index_name = 'uk_activity_candidate_scope'
);
prepare stmt from @sql;
execute stmt;
deallocate prepare stmt;
