package me.playernguyen.opteco.util;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;

/**
 * Compares versions using Semantic Versioning 2.0.0 precedence rules.
 *
 * <p>Release tags may be prefixed with {@code v}. Legacy OptEco versions such
 * as {@code 2.1.4b} or {@code 2.1.4 b} are read as the pre-release
 * {@code 2.1.4-b}, so the final {@code 2.1.4} release is considered newer.
 * Build metadata after {@code +} is ignored, as the specification requires.</p>
 *
 * <p>The class holds no state, so all methods are thread-safe.</p>
 */
public final class Semver {

    /** Numeric prefix followed by an optional legacy suffix, for example {@code 2.1.4b}. */
    private static final Pattern CORE = Pattern.compile("^(\\d+(?:\\.\\d+)*)(.*)$");

    /** Utility class; not instantiable. */
    private Semver() {}

    /**
     * Compares two versions by SemVer precedence.
     *
     * @param left first version, optionally prefixed with {@code v}
     * @param right second version, optionally prefixed with {@code v}
     * @return a negative value, zero, or a positive value when {@code left} is
     *     older than, equal to, or newer than {@code right}
     * @throws IllegalArgumentException when either version cannot be parsed
     */
    public static int compare(@NotNull String left, @NotNull String right) {
        return parse(left).compare(parse(right));
    }

    /**
     * Checks whether {@code candidate} has higher SemVer precedence than {@code current}.
     *
     * @param candidate version to test, for example a GitHub release tag
     * @param current version currently running
     * @return {@code true} when {@code candidate} is strictly newer
     * @throws IllegalArgumentException when either version cannot be parsed
     */
    public static boolean isNewer(@NotNull String candidate, @NotNull String current) {
        return compare(candidate, current) > 0;
    }

    /**
     * Parses a version, accepting the legacy formats used by older OptEco tags.
     *
     * @param raw version text
     * @return the parsed version
     * @throws IllegalArgumentException when the text is not a recognizable version
     */
    @NotNull
    private static Version parse(@NotNull String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("Version cannot be null");
        }
        String text = raw.trim();
        if (text.isEmpty()) {
            throw new IllegalArgumentException("Version is empty");
        }
        if (text.charAt(0) == 'v' || text.charAt(0) == 'V') {
            text = text.substring(1).trim();
        }
        int metadata = text.indexOf('+');
        if (metadata >= 0) {
            text = text.substring(0, metadata);
        }
        if (text.isEmpty()) {
            throw new IllegalArgumentException("Version is empty");
        }

        String core;
        String preRelease;
        int dash = text.indexOf('-');
        if (dash >= 0) {
            core = text.substring(0, dash);
            preRelease = text.substring(dash + 1);
            if (preRelease.isEmpty()) {
                throw new IllegalArgumentException("Unrecognized version: " + raw);
            }
        } else {
            Matcher matcher = CORE.matcher(text);
            if (!matcher.matches()) {
                throw new IllegalArgumentException("Unrecognized version: " + raw);
            }
            core = matcher.group(1);
            preRelease = matcher.group(2).trim().replaceAll("\\s+", ".");
        }

        String[] coreParts = core.split("\\.", -1);
        if (coreParts.length > 3) {
            throw new IllegalArgumentException("Unrecognized version: " + raw);
        }
        long major = parseNumber(coreParts[0], raw);
        long minor = coreParts.length > 1 ? parseNumber(coreParts[1], raw) : 0L;
        long patch = coreParts.length > 2 ? parseNumber(coreParts[2], raw) : 0L;
        return new Version(major, minor, patch, parsePreRelease(preRelease, raw));
    }

    /**
     * Parses a numeric version component.
     *
     * @param value digits to parse
     * @param original full version text, used in the error message
     * @return the component as a number
     * @throws IllegalArgumentException when the component is not numeric
     */
    private static long parseNumber(@NotNull String value, @NotNull String original) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid version component '" + value + "' in " + original, exception);
        }
    }

    /**
     * Splits a pre-release string into its dot-separated identifiers.
     *
     * @param preRelease pre-release text, possibly empty
     * @param original full version text, used in the error message
     * @return immutable identifiers in declaration order
     * @throws IllegalArgumentException when an identifier is empty or contains invalid characters
     */
    @NotNull
    private static List<String> parsePreRelease(@NotNull String preRelease, @NotNull String original) {
        if (preRelease.isEmpty()) {
            return Collections.emptyList();
        }
        String[] parts = preRelease.split("\\.", -1);
        List<String> identifiers = new ArrayList<>(parts.length);
        for (String part : parts) {
            if (!isIdentifier(part)) {
                throw new IllegalArgumentException("Invalid pre-release '" + preRelease + "' in " + original);
            }
            identifiers.add(part);
        }
        return Collections.unmodifiableList(identifiers);
    }

    /**
     * Checks whether text is a valid SemVer identifier.
     *
     * @param value identifier to validate
     * @return {@code true} when the identifier is non-empty and alphanumeric with hyphens
     */
    private static boolean isIdentifier(@NotNull String value) {
        if (value.isEmpty()) {
            return false;
        }
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            boolean valid = (character >= '0' && character <= '9')
                    || (character >= 'A' && character <= 'Z')
                    || (character >= 'a' && character <= 'z')
                    || character == '-';
            if (!valid) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks whether a pre-release identifier is numeric.
     *
     * @param value identifier to inspect
     * @return {@code true} when every character is a digit
     */
    private static boolean isNumeric(@NotNull String value) {
        for (int index = 0; index < value.length(); index++) {
            if (!Character.isDigit(value.charAt(index))) {
                return false;
            }
        }
        return true;
    }

    /** Parsed version with SemVer precedence. */
    private static final class Version {

        /** Major component. */
        private final long major;

        /** Minor component. */
        private final long minor;

        /** Patch component. */
        private final long patch;

        /** Pre-release identifiers; empty for a final release. */
        private final List<String> preRelease;

        /**
         * Creates a parsed version.
         *
         * @param major major component
         * @param minor minor component
         * @param patch patch component
         * @param preRelease pre-release identifiers, possibly empty
         */
        private Version(long major, long minor, long patch, @NotNull List<String> preRelease) {
            this.major = major;
            this.minor = minor;
            this.patch = patch;
            this.preRelease = preRelease;
        }

        /**
         * Compares this version with another by SemVer precedence.
         *
         * @param other version to compare against
         * @return a negative value, zero, or a positive value when this version is
         *     older than, equal to, or newer than {@code other}
         */
        private int compare(@NotNull Version other) {
            int comparison = Long.compare(major, other.major);
            if (comparison != 0) {
                return comparison;
            }
            comparison = Long.compare(minor, other.minor);
            if (comparison != 0) {
                return comparison;
            }
            comparison = Long.compare(patch, other.patch);
            if (comparison != 0) {
                return comparison;
            }
            return comparePreRelease(preRelease, other.preRelease);
        }

        /**
         * Compares pre-release identifier lists.
         *
         * @param left first identifier list
         * @param right second identifier list
         * @return a negative value, zero, or a positive value when {@code left} has lower precedence
         */
        private static int comparePreRelease(@NotNull List<String> left, @NotNull List<String> right) {
            if (left.isEmpty() && right.isEmpty()) {
                return 0;
            }
            if (left.isEmpty()) {
                return 1;
            }
            if (right.isEmpty()) {
                return -1;
            }
            int common = Math.min(left.size(), right.size());
            for (int index = 0; index < common; index++) {
                String first = left.get(index);
                String second = right.get(index);
                boolean firstNumeric = isNumeric(first);
                boolean secondNumeric = isNumeric(second);
                int comparison;
                if (firstNumeric && secondNumeric) {
                    comparison = new BigInteger(first).compareTo(new BigInteger(second));
                } else if (firstNumeric) {
                    comparison = -1;
                } else if (secondNumeric) {
                    comparison = 1;
                } else {
                    comparison = first.compareTo(second);
                }
                if (comparison != 0) {
                    return comparison;
                }
            }
            return Integer.compare(left.size(), right.size());
        }
    }
}
