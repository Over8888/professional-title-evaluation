-- Drop unused Quartz scheduler tables only.
-- Re-run safe: every statement uses IF EXISTS.

drop table if exists qrtz_blob_triggers;
drop table if exists qrtz_cron_triggers;
drop table if exists qrtz_simple_triggers;
drop table if exists qrtz_simprop_triggers;

drop table if exists qrtz_fired_triggers;
drop table if exists qrtz_paused_trigger_grps;
drop table if exists qrtz_scheduler_state;
drop table if exists qrtz_locks;
drop table if exists qrtz_calendars;

drop table if exists qrtz_triggers;
drop table if exists qrtz_job_details;
