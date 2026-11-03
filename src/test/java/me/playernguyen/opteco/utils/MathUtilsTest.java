package me.playernguyen.opteco.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MathUtilsTest {

    @Test
    void shouldAcceptNumbersWhenGivenValidIntegerAndDecimalStrings() {
        // Arrange
        String integer = "42";
        String negativeInteger = "-7";
        String decimal = "3.14";
        String negativeDecimal = "-0.5";

        // Act
        boolean integerResult = MathUtils.isNotNumber(integer);
        boolean negativeIntegerResult = MathUtils.isNotNumber(negativeInteger);
        boolean decimalResult = MathUtils.isNotNumber(decimal);
        boolean negativeDecimalResult = MathUtils.isNotNumber(negativeDecimal);

        // Assert
        assertFalse(integerResult);
        assertFalse(negativeIntegerResult);
        assertFalse(decimalResult);
        assertFalse(negativeDecimalResult);
    }

    @Test
    void shouldRejectValuesWhenGivenTextOrMalformedStrings() {
        // Arrange
        String text = "abc";
        String empty = "";
        String malformed = "1.2.3";
        String trailingText = "12a";
        String whitespace = " ";

        // Act
        boolean textResult = MathUtils.isNotNumber(text);
        boolean emptyResult = MathUtils.isNotNumber(empty);
        boolean malformedResult = MathUtils.isNotNumber(malformed);
        boolean trailingTextResult = MathUtils.isNotNumber(trailingText);
        boolean whitespaceResult = MathUtils.isNotNumber(whitespace);

        // Assert
        assertTrue(textResult);
        assertTrue(emptyResult);
        assertTrue(malformedResult);
        assertTrue(trailingTextResult);
        assertTrue(whitespaceResult);
    }

    @Test
    void shouldDetectIntegersWhenGivenIntegerStrings() {
        // Arrange
        String zero = "0";
        String negative = "-12";
        String decimal = "3.14";
        String text = "abc";

        // Act
        boolean zeroResult = MathUtils.isInteger(zero);
        boolean negativeResult = MathUtils.isInteger(negative);
        boolean decimalResult = MathUtils.isInteger(decimal);
        boolean textResult = MathUtils.isInteger(text);

        // Assert
        assertTrue(zeroResult);
        assertTrue(negativeResult);
        assertFalse(decimalResult);
        assertFalse(textResult);
    }
}
