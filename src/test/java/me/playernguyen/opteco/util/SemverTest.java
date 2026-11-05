package me.playernguyen.opteco.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SemverTest {

    @Test
    void shouldTreatLegacySuffixAsPreReleaseWhenComparedWithFinalRelease() {
        // Arrange & Act & Assert
        assertTrue(Semver.isNewer("2.1.4", "2.1.4b"));
        assertTrue(Semver.isNewer("2.1.4", "2.1.4 b"));
        assertFalse(Semver.isNewer("2.1.4b", "2.1.4"));
    }

    @Test
    void shouldIgnoreLeadingVAndBuildMetadataWhenVersionsMatch() {
        // Arrange & Act & Assert
        assertEquals(0, Semver.compare("v2.1.4", "2.1.4+build.7"));
        assertFalse(Semver.isNewer("v2.1.4+other", "2.1.4"));
    }

    @Test
    void shouldFollowSemverPreReleasePrecedence() {
        // Arrange
        String[] ordered = {
            "1.0.0-alpha",
            "1.0.0-alpha.1",
            "1.0.0-alpha.beta",
            "1.0.0-beta",
            "1.0.0-beta.2",
            "1.0.0-beta.11",
            "1.0.0-rc.1",
            "1.0.0"
        };

        // Act & Assert
        for (int index = 0; index < ordered.length - 1; index++) {
            assertTrue(
                    Semver.isNewer(ordered[index + 1], ordered[index]),
                    ordered[index] + " should be older than " + ordered[index + 1]);
        }
    }

    @Test
    void shouldCompareNumericPreReleaseIdentifiersNumerically() {
        // Arrange & Act & Assert
        assertTrue(Semver.isNewer("1.0.0-10", "1.0.0-2"));
        assertTrue(Semver.isNewer("1.0.0-alpha.10", "1.0.0-alpha.2"));
        assertTrue(Semver.isNewer("1.0.0-alpha", "1.0.0-1"));
    }

    @Test
    void shouldCompareCoreComponentsNumerically() {
        // Arrange & Act & Assert
        assertTrue(Semver.isNewer("2.0.0", "1.99.99"));
        assertTrue(Semver.isNewer("1.10.0", "1.9.0"));
        assertTrue(Semver.isNewer("1.0.10", "1.0.9"));
        assertTrue(Semver.isNewer("2.1.5", "2.1.4b"));
    }

    @Test
    void shouldPadMissingComponentsWhenParsing() {
        // Arrange & Act & Assert
        assertEquals(0, Semver.compare("1.2", "1.2.0"));
        assertEquals(0, Semver.compare("1", "1.0.0"));
    }

    @Test
    void shouldRejectUnrecognizedVersions() {
        // Arrange & Act & Assert
        assertThrows(IllegalArgumentException.class, () -> Semver.compare("", "1.0.0"));
        assertThrows(IllegalArgumentException.class, () -> Semver.compare("not-a-version", "1.0.0"));
        assertThrows(IllegalArgumentException.class, () -> Semver.compare("1.2.3.4", "1.0.0"));
        assertThrows(IllegalArgumentException.class, () -> Semver.compare("1.0.0-", "1.0.0"));
        assertThrows(IllegalArgumentException.class, () -> Semver.compare("1.0.0-alpha..1", "1.0.0"));
    }
}
