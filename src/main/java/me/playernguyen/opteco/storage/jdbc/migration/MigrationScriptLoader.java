package me.playernguyen.opteco.storage.jdbc.migration;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import me.playernguyen.opteco.core.exception.StorageException;
import me.playernguyen.opteco.storage.jdbc.SqlDialect;

/**
 * Discovers and validates the SQL migrations bundled for one dialect.
 *
 * <p>Scripts live under {@code migration/<dialect>/} and are named
 * {@code <version>_<description>.sql}. Discovery asks the plugin classloader for
 * that exact folder, so it works both inside the packaged jar and in the
 * exploded {@code build/resources/main} used by tests. Jar roots are accepted
 * only when they belong to this plugin's own artifact; a code-source fallback
 * handles repackagers that strip directory entries.</p>
 *
 * <p>Versions must be unique and contiguous from {@code 1}; the result is sorted
 * numerically. Instances are immutable apart from the classloader they read
 * from, which is only used during {@link #load()} on the calling thread.</p>
 */
public final class MigrationScriptLoader {

    /** Root resource folder shared by every dialect. */
    static final String ROOT = "migration";

    /** File-name contract: numeric prefix, lowercase description, {@code .sql}. */
    private static final Pattern FILE_NAME = Pattern.compile("^(\\d{4,})_([a-z0-9_]+)\\.sql$");

    /** Hex alphabet used by {@link #sha256(String)}. */
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    /** Size of the buffer used to read a script from the jar. */
    private static final int READ_BUFFER = 4096;

    /** Folder searched by this loader, for example {@code migration/sqlite}. */
    private final String folder;

    /** Classloader that resolves the folder, normally the plugin's. */
    private final ClassLoader classLoader;

    /** Code source of this plugin, used to reject foreign jar roots. */
    private final URL ownArtifact;

    /**
     * Creates a loader for an explicit folder and classloader.
     *
     * @param folder classpath-relative folder, for example {@code migration/sqlite}
     * @param classLoader loader that resolves the folder
     * @param ownArtifact code source of the owning artifact, or {@code null}
     */
    MigrationScriptLoader(String folder, ClassLoader classLoader, URL ownArtifact) {
        this.folder = folder;
        this.classLoader = classLoader;
        this.ownArtifact = ownArtifact;
    }

    /**
     * Creates a loader for the dialect of the running plugin.
     *
     * @param dialect the storage dialect
     * @return a loader for {@code migration/<dialect>}
     */
    public static MigrationScriptLoader forDialect(SqlDialect dialect) {
        return new MigrationScriptLoader(
                ROOT + "/" + dialect.migrationFolder(), MigrationScriptLoader.class.getClassLoader(), codeSource());
    }

    /**
     * Reads, validates and sorts every script for the configured folder.
     *
     * @return the scripts in ascending version order, unmodifiable and never empty
     * @throws StorageException when the folder cannot be read, is empty, or a script is malformed
     */
    public List<MigrationScript> load() {
        Map<String, String> resources = new TreeMap<>();
        try {
            Enumeration<URL> roots = classLoader.getResources(folder);
            while (roots.hasMoreElements()) {
                URL root = roots.nextElement();
                if ("file".equals(root.getProtocol())) {
                    collectFromDirectory(root, resources);
                } else if ("jar".equals(root.getProtocol()) && isOwnArtifact(root)) {
                    collectFromJar(root, resources);
                }
            }
            if (resources.isEmpty()) {
                collectFromCodeSource(resources);
            }
        } catch (IOException exception) {
            throw new StorageException("Could not read SQL migrations under " + folder, exception);
        } catch (URISyntaxException exception) {
            throw new StorageException("Invalid SQL migration resource path under " + folder, exception);
        }
        return toScripts(resources);
    }

    /**
     * Reads the direct {@code *.sql} children of one directory root.
     *
     * @param root the directory URL
     * @param resources destination map keyed by resource path
     * @throws IOException when the directory cannot be listed or read
     * @throws URISyntaxException when the URL cannot be converted to a file
     */
    private void collectFromDirectory(URL root, Map<String, String> resources) throws IOException, URISyntaxException {
        File directory = new File(root.toURI());
        if (!directory.isDirectory()) {
            return;
        }
        List<Path> children;
        try (Stream<Path> stream = Files.list(directory.toPath())) {
            children = stream.collect(Collectors.toList());
        }
        for (Path child : children) {
            Path namePath = child.getFileName();
            if (namePath == null) {
                continue;
            }
            String name = namePath.toString();
            if (!Files.isRegularFile(child) || !name.endsWith(".sql")) {
                continue;
            }
            resources.put(folder + "/" + name, new String(Files.readAllBytes(child), StandardCharsets.UTF_8));
        }
    }

    /**
     * Reads the direct {@code *.sql} entries under the folder of one jar root.
     *
     * @param root the jar URL
     * @param resources destination map keyed by resource path
     * @throws IOException when the jar cannot be opened or read
     */
    private void collectFromJar(URL root, Map<String, String> resources) throws IOException {
        JarURLConnection connection = (JarURLConnection) root.openConnection();
        connection.setUseCaches(false);
        try (JarFile jar = connection.getJarFile()) {
            collectFromJar(jar, resources);
        }
    }

    /**
     * Reads the direct {@code *.sql} entries under the folder of an open jar.
     *
     * @param jar the open jar
     * @param resources destination map keyed by resource path
     * @throws IOException when an entry cannot be read
     */
    private void collectFromJar(JarFile jar, Map<String, String> resources) throws IOException {
        String prefix = folder + "/";
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String name = entry.getName();
            if (entry.isDirectory()
                    || !name.startsWith(prefix)
                    || !name.endsWith(".sql")
                    || name.indexOf('/', prefix.length()) >= 0) {
                continue;
            }
            try (InputStream input = jar.getInputStream(entry)) {
                resources.put(name, readUtf8(input));
            }
        }
    }

    /**
     * Fallback used when a repackager stripped the directory entry from the jar.
     *
     * @param resources destination map keyed by resource path
     * @throws IOException when the code source cannot be read
     * @throws URISyntaxException when the code source URL cannot be converted to a file
     */
    private void collectFromCodeSource(Map<String, String> resources) throws IOException, URISyntaxException {
        if (ownArtifact == null) {
            return;
        }
        File location = new File(ownArtifact.toURI());
        if (location.isDirectory()) {
            File directory = new File(location, folder);
            collectFromDirectory(directory.toURI().toURL(), resources);
        } else if (location.isFile()) {
            try (JarFile jar = new JarFile(location)) {
                collectFromJar(jar, resources);
            }
        }
    }

    /**
     * Checks whether a jar root belongs to the plugin that owns this loader.
     *
     * @param root the jar URL returned by the classloader
     * @return {@code true} when the jar is this plugin's own artifact
     */
    private boolean isOwnArtifact(URL root) {
        if (ownArtifact == null) {
            return false;
        }
        try {
            File own = new File(ownArtifact.toURI()).getCanonicalFile();
            JarURLConnection connection = (JarURLConnection) root.openConnection();
            connection.setUseCaches(false);
            File jar = new File(connection.getJarFileURL().toURI()).getCanonicalFile();
            return own.equals(jar);
        } catch (IOException exception) {
            return false;
        } catch (URISyntaxException exception) {
            return false;
        }
    }

    /**
     * Converts raw resources into validated, sorted scripts.
     *
     * @param resources resource path to normalized text
     * @return the validated scripts
     * @throws StorageException when no script exists, a name is malformed, a version repeats, or versions have gaps
     */
    private List<MigrationScript> toScripts(Map<String, String> resources) {
        if (resources.isEmpty()) {
            throw new StorageException("No SQL migration scripts found under " + folder);
        }
        List<MigrationScript> scripts = new ArrayList<>(resources.size());
        Map<Integer, String> versions = new HashMap<>();
        for (Map.Entry<String, String> resource : resources.entrySet()) {
            String resourcePath = resource.getKey();
            String fileName = resourcePath.substring(resourcePath.lastIndexOf('/') + 1);
            Matcher matcher = FILE_NAME.matcher(fileName);
            if (!matcher.matches()) {
                throw new StorageException(
                        "Migration file " + resourcePath + " must be named <version>_<description>.sql");
            }
            int version = parseVersion(matcher.group(1), resourcePath);
            String previous = versions.put(version, resourcePath);
            if (previous != null) {
                throw new StorageException(
                        "Duplicate migration version " + version + ": " + previous + " and " + resourcePath);
            }
            String template = normalize(resource.getValue());
            scripts.add(new MigrationScript(version, fileName, resourcePath, sha256(template), template));
        }
        Collections.sort(scripts, Comparator.comparingInt(MigrationScript::version));
        for (int index = 0; index < scripts.size(); index++) {
            int expected = index + 1;
            if (scripts.get(index).version() != expected) {
                throw new StorageException(
                        "Migration versions must be contiguous from 1 under " + folder + "; expected version "
                                + expected + " but found " + scripts.get(index).version());
            }
        }
        return Collections.unmodifiableList(scripts);
    }

    /**
     * Parses the numeric prefix of a migration file name.
     *
     * @param raw the matched digits
     * @param resourcePath path used in the error message
     * @return the version as an {@code int}
     * @throws StorageException when the number does not fit in an {@code int}
     */
    private static int parseVersion(String raw, String resourcePath) {
        long parsed;
        try {
            parsed = Long.parseLong(raw);
        } catch (NumberFormatException exception) {
            throw new StorageException("Migration version is not a number in " + resourcePath, exception);
        }
        if (parsed < 1L || parsed > Integer.MAX_VALUE) {
            throw new StorageException("Migration version out of range in " + resourcePath + ": " + raw);
        }
        return (int) parsed;
    }

    /**
     * Normalizes line endings and removes a UTF-8 byte-order mark so the hash
     * does not depend on the checkout or packaging platform.
     *
     * @param text the raw script text
     * @return the normalized text
     */
    static String normalize(String text) {
        String value = text.replace("\r\n", "\n").replace('\r', '\n');
        if (value.startsWith("\uFEFF")) {
            value = value.substring(1);
        }
        return value;
    }

    /**
     * Hashes normalized script text with SHA-256.
     *
     * @param text the normalized text
     * @return lowercase hexadecimal digest
     */
    static String sha256(String text) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is required by the Java platform", impossible);
        }
        byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
        char[] encoded = new char[hash.length * 2];
        for (int index = 0; index < hash.length; index++) {
            encoded[index * 2] = HEX[(hash[index] >> 4) & 0xF];
            encoded[index * 2 + 1] = HEX[hash[index] & 0xF];
        }
        return new String(encoded);
    }

    /**
     * Reads a stream as UTF-8 without closing it.
     *
     * @param input the stream to read
     * @return the decoded text
     * @throws IOException when the stream cannot be read
     */
    private static String readUtf8(InputStream input) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
        StringBuilder text = new StringBuilder();
        char[] buffer = new char[READ_BUFFER];
        int read;
        while ((read = reader.read(buffer)) != -1) {
            text.append(buffer, 0, read);
        }
        return text.toString();
    }

    /**
     * Resolves the code source of this plugin.
     *
     * @return the code source location, or {@code null} when the JVM does not expose one
     */
    private static URL codeSource() {
        ProtectionDomain domain = MigrationScriptLoader.class.getProtectionDomain();
        CodeSource source = domain == null ? null : domain.getCodeSource();
        return source == null ? null : source.getLocation();
    }
}
