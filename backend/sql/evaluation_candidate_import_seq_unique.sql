-- Make Candidate.import_seq a system-maintained unique continuous business sequence.
-- ActivityCandidate snapshots are intentionally not resequenced.

update Candidate c
join (
    select id, row_number() over (order by id asc) as new_seq
    from Candidate
) ranked on ranked.id = c.id
set c.import_seq = -ranked.new_seq,
    c.update_time = sysdate();

update Candidate
set import_seq = -import_seq,
    update_time = sysdate()
where import_seq < 0;

set @idx_exists := (
    select count(1)
    from information_schema.statistics
    where table_schema = database()
      and table_name = 'Candidate'
      and index_name = 'uk_candidate_import_seq'
);

set @sql := if(
    @idx_exists = 0,
    'alter table Candidate add unique key uk_candidate_import_seq (import_seq)',
    'select ''uk_candidate_import_seq already exists'''
);

prepare stmt from @sql;
execute stmt;
deallocate prepare stmt;
