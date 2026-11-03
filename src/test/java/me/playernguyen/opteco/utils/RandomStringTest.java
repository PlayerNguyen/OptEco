package me.playernguyen.opteco.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RandomStringTest {

    @Test
    void shouldGenerateRequestedLengthWhenGivenPositiveSize() {
        // Arrange
        int size = 16;

        // Act
        String value = RandomString.rand(size);

        // Assert
        assertEquals(size, value.length());
    }

    @Test
    void shouldGenerateEmptyStringWhenGivenZeroSize() {
        // Arrange
        int size = 0;

        // Act
        String value = RandomString.rand(size);

        // Assert
        assertEquals(0, value.length());
    }

    @Test
    void shouldGenerateAlphanumericCharactersWhenGeneratingString() {
        // Arrange
        int size = 256;

        // Act
        String value = RandomString.rand(size);

        // Assert
        for (int i = 0; i < value.length(); i++) {
            assertTrue(Character.isLetterOrDigit(value.charAt(i)), "Unexpected character at index " + i);
        }
    }
}
