-- Phase 2 vote workflow supporting indexes.
-- Safe to run after evaluation_phase1.sql or existing activity snapshot migrations.

set @schema_name = database();

set @sql = (
  select if(count(*) = 0,
    'alter table ActivityCandidate add key idx_activity_candidate_vote_scope (activity_id, fixed_type)',
    'select 1')
  from information_schema.statistics
  where table_schema = @schema_name
    and table_name = 'ActivityCandidate'
    and index_name = 'idx_activity_candidate_vote_scope'
);
prepare stmt from @sql;
execute stmt;
deallocate prepare stmt;

set @sql = (
  select if(count(*) = 0,
    'alter table Vote add key idx_vote_activity_voter_round (activity_id, activity_voter_id, round_no)',
    'select 1')
  from information_schema.statistics
  where table_schema = @schema_name
    and table_name = 'Vote'
    and index_name = 'idx_vote_activity_voter_round'
);
prepare stmt from @sql;
execute stmt;
deallocate prepare stmt;

set @sql = (
  select if(count(*) = 0,
    'alter table Vote add key idx_vote_activity_candidate_result (activity_id, activity_candidate_id, result)',
    'select 1')
  from information_schema.statistics
  where table_schema = @schema_name
    and table_name = 'Vote'
    and index_name = 'idx_vote_activity_candidate_result'
);
prepare stmt from @sql;
execute stmt;
deallocate prepare stmt;
