package me.playernguyen.opteco.storage.jdbc.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import me.playernguyen.opteco.core.exception.StorageException;
import me.playernguyen.opteco.storage.jdbc.SqlDialect;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MigrationScriptLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldLoadScriptsSortedByVersionWhenFilesAreUnordered() throws IOException {
        // Arrange
        Path root = directory("migration/sqlite");
        write(root.resolve("0002_second.sql"), "SELECT 2;");
        write(root.resolve("0001_first.sql"), "SELECT 1;");
        MigrationScriptLoader loader = loader(tempDir);

        // Act
        List<MigrationScript> scripts = loader.load();

        // Assert
        assertEquals(2, scripts.size());
        assertEquals(1, scripts.get(0).version());
        assertEquals(2, scripts.get(1).version());
    }

    @Test
    void shouldRejectGapWhenVersionIsMissing() throws IOException {
        // Arrange
        Path root = directory("migration/sqlite");
        write(root.resolve("0001_first.sql"), "SELECT 1;");
        write(root.resolve("0003_third.sql"), "SELECT 3;");
        MigrationScriptLoader loader = loader(tempDir);

        // Act and Assert
        assertThrows(StorageException.class, loader::load);
    }

    @Test
    void shouldRejectDuplicateVersionWhenTwoFilesShareIt() throws IOException {
        // Arrange
        Path root = directory("migration/sqlite");
        write(root.resolve("0001_first.sql"), "SELECT 1;");
        write(root.resolve("0001_second.sql"), "SELECT 2;");
        MigrationScriptLoader loader = loader(tempDir);

        // Act and Assert
        assertThrows(StorageException.class, loader::load);
    }

    @Test
    void shouldRejectInvalidFileNameWhenVersionPrefixIsMissing() throws IOException {
        // Arrange
        Path root = directory("migration/sqlite");
        write(root.resolve("create_schema.sql"), "SELECT 1;");
        MigrationScriptLoader loader = loader(tempDir);

        // Act and Assert
        assertThrows(StorageException.class, loader::load);
    }

    @Test
    void shouldIgnoreNonSqlFilesWhenFolderHasThem() throws IOException {
        // Arrange
        Path root = directory("migration/sqlite");
        write(root.resolve("0001_first.sql"), "SELECT 1;");
        write(root.resolve("README.txt"), "not a script");
        write(root.resolve("notes.md"), "not a script");
        MigrationScriptLoader loader = loader(tempDir);

        // Act
        List<MigrationScript> scripts = loader.load();

        // Assert
        assertEquals(1, scripts.size());
    }

    @Test
    void shouldFailWhenFolderHasNoScripts() throws IOException {
        // Arrange
        directory("migration/sqlite");
        MigrationScriptLoader loader = loader(tempDir);

        // Act and Assert
        assertThrows(StorageException.class, loader::load);
    }

    @Test
    void shouldDiscoverScriptsWhenRootIsAJar() throws Exception {
        // Arrange
        Path jar = createJar("migrations.jar", "migration/sqlite/0001_first.sql", "SELECT 1;");
        URL jarUrl = jar.toUri().toURL();
        try (URLClassLoader classLoader = new URLClassLoader(new URL[] {jarUrl}, null)) {
            MigrationScriptLoader loader = new MigrationScriptLoader("migration/sqlite", classLoader, jarUrl);

            // Act
            List<MigrationScript> scripts = loader.load();

            // Assert
            assertEquals(1, scripts.size());
            assertEquals(1, scripts.get(0).version());
        }
    }

    @Test
    void shouldIgnoreForeignJarWhenOwnArtifactDoesNotMatch() throws Exception {
        // Arrange
        Path jar = createJar("foreign.jar", "migration/sqlite/0001_first.sql", "SELECT 1;");
        URL jarUrl = jar.toUri().toURL();
        URL otherArtifact = tempDir.resolve("other.jar").toUri().toURL();
        try (URLClassLoader classLoader = new URLClassLoader(new URL[] {jarUrl}, null)) {
            MigrationScriptLoader loader = new MigrationScriptLoader("migration/sqlite", classLoader, otherArtifact);

            // Act and Assert
            assertThrows(StorageException.class, loader::load);
        }
    }

    @Test
    void shouldMatchBundledVersionsWhenComparingDialects() {
        // Act
        List<MigrationScript> sqlite =
                MigrationScriptLoader.forDialect(SqlDialect.SQLITE).load();
        List<MigrationScript> mysql =
                MigrationScriptLoader.forDialect(SqlDialect.MYSQL).load();

        // Assert
        assertEquals(sqlite.size(), mysql.size());
        for (int index = 0; index < sqlite.size(); index++) {
            assertEquals(sqlite.get(index).version(), mysql.get(index).version());
        }
    }

    @Test
    void shouldComputeSameChecksumWhenLineEndingsDiffer() {
        // Arrange
        String unix = "SELECT 1;\nSELECT 2;\n";
        String windows = "SELECT 1;\r\nSELECT 2;\r\n";

        // Act
        String unixHash = MigrationScriptLoader.sha256(MigrationScriptLoader.normalize(unix));
        String windowsHash = MigrationScriptLoader.sha256(MigrationScriptLoader.normalize(windows));

        // Assert
        assertEquals(unixHash, windowsHash);
    }

    private Path directory(String relative) throws IOException {
        Path directory = tempDir.resolve(relative);
        Files.createDirectories(directory);
        return directory;
    }

    private MigrationScriptLoader loader(Path classpathRoot) throws IOException {
        URL root = classpathRoot.toUri().toURL();
        return new MigrationScriptLoader("migration/sqlite", new URLClassLoader(new URL[] {root}, null), root);
    }

    private void write(Path file, String content) throws IOException {
        Files.write(file, content.getBytes(StandardCharsets.UTF_8));
    }

    private Path createJar(String name, String entryName, String content) throws IOException {
        Path jar = tempDir.resolve(name);
        try (OutputStream output = Files.newOutputStream(jar);
                JarOutputStream jarOutput = new JarOutputStream(output)) {
            addDirectory(jarOutput, "migration/");
            addDirectory(jarOutput, "migration/sqlite/");
            JarEntry entry = new JarEntry(entryName);
            jarOutput.putNextEntry(entry);
            jarOutput.write(content.getBytes(StandardCharsets.UTF_8));
            jarOutput.closeEntry();
        }
        return jar;
    }

    private void addDirectory(JarOutputStream jarOutput, String name) throws IOException {
        jarOutput.putNextEntry(new JarEntry(name));
        jarOutput.closeEntry();
    }
}
