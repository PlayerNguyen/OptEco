package me.playernguyen.opteco.storage.jdbc.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class SqlScriptSplitterTest {

    @Test
    void shouldSplitStatementsWhenSemicolonsSeparateThem() {
        // Arrange
        String sql = "CREATE TABLE a (id INT);\nCREATE TABLE b (id INT);";

        // Act
        List<String> statements = SqlScriptSplitter.split(sql);

        // Assert
        assertEquals(2, statements.size());
        assertEquals("CREATE TABLE a (id INT)", statements.get(0));
        assertEquals("CREATE TABLE b (id INT)", statements.get(1));
    }

    @Test
    void shouldKeepSemicolonWhenItIsInsideQuotes() {
        // Arrange
        String sql = "INSERT INTO notes (value) VALUES ('a;b'); SELECT 1;";

        // Act
        List<String> statements = SqlScriptSplitter.split(sql);

        // Assert
        assertEquals(2, statements.size());
        assertEquals("INSERT INTO notes (value) VALUES ('a;b')", statements.get(0));
        assertEquals("SELECT 1", statements.get(1));
    }

    @Test
    void shouldStripLineCommentsWhenPresent() {
        // Arrange
        String sql = "SELECT 1 -- trailing comment\nFROM dual;";

        // Act
        List<String> statements = SqlScriptSplitter.split(sql);

        // Assert
        assertEquals(1, statements.size());
        assertEquals("SELECT 1  FROM dual", statements.get(0));
    }

    @Test
    void shouldKeepTrailingStatementWhenItHasNoSemicolon() {
        // Arrange
        String sql = "SELECT 1;\nSELECT 2";

        // Act
        List<String> statements = SqlScriptSplitter.split(sql);

        // Assert
        assertEquals(2, statements.size());
        assertEquals("SELECT 2", statements.get(1));
    }

    @Test
    void shouldReturnEmptyListWhenScriptHasNoStatements() {
        // Arrange
        String sql = "  -- only a comment\n\n  ;  ";

        // Act
        List<String> statements = SqlScriptSplitter.split(sql);

        // Assert
        assertTrue(statements.isEmpty());
    }

    @Test
    void shouldSplitComplexScriptWhenItMixesCommentsQuotesAndSemicolons() {
        // Arrange
        String sql = "-- bootstrap; script\n"
                + "CREATE TABLE IF NOT EXISTS `opteco_audit` (\n"
                + "    `id` BIGINT NOT NULL AUTO_INCREMENT,\n"
                + "    `message` VARCHAR(255) NOT NULL DEFAULT 'n/a; pending',\n"
                + "    `owner` VARCHAR(64) NOT NULL,\n"
                + "    PRIMARY KEY (`id`),\n"
                + "    KEY `idx_owner` (`owner`)\n"
                + ");\n"
                + "\n"
                + "INSERT INTO `opteco_audit` (`message`, `owner`)\n"
                + "    VALUES ('it''s; done -- still text', 'console');\n"
                + "\n"
                + "CREATE TABLE IF NOT EXISTS `weird``table` (\n"
                + "    `value` VARCHAR(32) NOT NULL\n"
                + ");\n"
                + "\n"
                + "-- trailing statement without a semicolon\n"
                + "UPDATE `opteco_accounts` SET `player` = 'a;b'\n"
                + "    WHERE `uuid` = '00000000-0000-0000-0000-000000000000'";

        String createAudit = "CREATE TABLE IF NOT EXISTS `opteco_audit` (\n"
                + "    `id` BIGINT NOT NULL AUTO_INCREMENT,\n"
                + "    `message` VARCHAR(255) NOT NULL DEFAULT 'n/a; pending',\n"
                + "    `owner` VARCHAR(64) NOT NULL,\n"
                + "    PRIMARY KEY (`id`),\n"
                + "    KEY `idx_owner` (`owner`)\n"
                + ")";
        String insertAudit = "INSERT INTO `opteco_audit` (`message`, `owner`)\n"
                + "    VALUES ('it''s; done -- still text', 'console')";
        String createWeird =
                "CREATE TABLE IF NOT EXISTS `weird``table` (\n" + "    `value` VARCHAR(32) NOT NULL\n" + ")";
        String update = "UPDATE `opteco_accounts` SET `player` = 'a;b'\n"
                + "    WHERE `uuid` = '00000000-0000-0000-0000-000000000000'";

        // Act
        List<String> statements = SqlScriptSplitter.split(sql);

        // Assert
        assertEquals(4, statements.size());
        assertEquals(createAudit, statements.get(0));
        assertEquals(insertAudit, statements.get(1));
        assertEquals(createWeird, statements.get(2));
        assertEquals(update, statements.get(3));
    }
}
