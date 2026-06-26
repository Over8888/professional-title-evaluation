-- Add voter import sequence fields and backfill existing rows.

set @sql := (
  select if(
    count(*) = 0,
    'alter table Voter add column import_seq int default null comment ''Import sequence'' after id',
    'select 1'
  )
  from information_schema.columns
  where table_schema = database()
    and table_name = 'Voter'
    and column_name = 'import_seq'
);
prepare stmt from @sql;
execute stmt;
deallocate prepare stmt;

set @voter_seq := 0;
update Voter
set import_seq = (@voter_seq := @voter_seq + 1)
where import_seq is null
order by id asc;

set @sql := (
  select if(
    count(*) = 0,
    'alter table Voter add unique key uk_voter_import_seq (import_seq)',
    'select 1'
  )
  from information_schema.statistics
  where table_schema = database()
    and table_name = 'Voter'
    and index_name = 'uk_voter_import_seq'
);
prepare stmt from @sql;
execute stmt;
deallocate prepare stmt;

set @sql := (
  select if(
    count(*) = 0,
    'alter table ActivityVoter add column import_seq int default null comment ''Import sequence snapshot'' after voter_id',
    'select 1'
  )
  from information_schema.columns
  where table_schema = database()
    and table_name = 'ActivityVoter'
    and column_name = 'import_seq'
);
prepare stmt from @sql;
execute stmt;
deallocate prepare stmt;

update ActivityVoter av
left join Voter v on v.id = av.voter_id
set av.import_seq = v.import_seq
where av.import_seq is null
  and v.import_seq is not null;

set @activity_voter_seq := 0;
update ActivityVoter
set import_seq = (@activity_voter_seq := @activity_voter_seq + 1)
where import_seq is null
order by activity_id asc, id asc;
