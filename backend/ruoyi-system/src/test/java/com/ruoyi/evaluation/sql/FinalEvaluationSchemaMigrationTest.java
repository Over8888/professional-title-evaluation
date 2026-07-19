package com.ruoyi.evaluation.sql;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FinalEvaluationSchemaMigrationTest
{
    private static final String MIGRATION_FILE = "evaluation_phase15_finalevaluation_schema_migration.sql";

    @Test
    void migrationAddsFinalEvaluationFieldsUsedByMapperAndCanRunRepeatedly() throws IOException
    {
        String sql = Files.readString(findMigrationFile(), StandardCharsets.UTF_8);

        assertTrue(sql.contains("CREATE PROCEDURE"));
        assertTrue(sql.contains("information_schema.COLUMNS"));
        assertTrue(sql.contains("confirm_status"));
        assertTrue(sql.contains("confirmed_by"));
        assertTrue(sql.contains("confirmed_at"));
        assertTrue(sql.contains("signed_by"));
        assertTrue(sql.contains("signature_status"));
        assertTrue(sql.contains("confirm_remark"));
        assertTrue(sql.contains("create_by"));
        assertTrue(sql.contains("update_time"));
        assertTrue(sql.contains("DROP PROCEDURE"));
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
