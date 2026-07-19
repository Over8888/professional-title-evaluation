-- Repair one explicitly selected activity snapshot from its Candidate source.
-- Set @target_activity_id before executing; no historical activities are changed.
SET @target_activity_id = NULL;

UPDATE `ActivityCandidate` ac
INNER JOIN `Candidate` c ON c.`id` = ac.`candidate_id`
SET ac.`third_level_department` = c.`third_level_department`,
    ac.`update_time` = NOW()
WHERE ac.`activity_id` = @target_activity_id
  AND @target_activity_id IS NOT NULL
  AND NOT (ac.`third_level_department` <=> c.`third_level_department`);
