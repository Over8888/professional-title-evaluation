package com.ruoyi.evaluation.sql;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ActivityCandidateThirdLevelDepartmentRepairSqlTest
{
    private static final String MIGRATION_FILE = "evaluation_phase15_activity_candidate_third_level_department_repair.sql";

    @Test
    void repairRequiresAnExplicitActivityAndCopiesTheCandidateSnapshotField() throws IOException
    {
        String sql = Files.readString(findMigrationFile(), StandardCharsets.UTF_8);

        assertTrue(sql.contains("SET @target_activity_id = NULL"));
        assertTrue(sql.contains("UPDATE `ActivityCandidate` ac"));
        assertTrue(sql.contains("INNER JOIN `Candidate` c ON c.`id` = ac.`candidate_id`"));
        assertTrue(sql.contains("ac.`third_level_department` = c.`third_level_department`"));
        assertTrue(sql.contains("ac.`activity_id` = @target_activity_id"));
        assertTrue(sql.contains("@target_activity_id IS NOT NULL"));
    }

    private Path findMigrationFile()
    {
        Path directory = Path.of("").toAbsolutePath();
        while (directory != null)
        {
            Path candidate = directory.resolve("sql").resolve(MIGRATION_FILE);
            if (Files.isRegularFile(candidate))
            {
                return candidate;
            }
            directory = directory.getParent();
        }
        throw new IllegalStateException("Cannot locate " + MIGRATION_FILE);
    }
}
