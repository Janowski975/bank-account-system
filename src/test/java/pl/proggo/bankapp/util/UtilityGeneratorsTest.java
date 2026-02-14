package pl.proggo.bankapp.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Utility Generators Unit Tests")
class UtilityGeneratorsTest {

    @Test
    @DisplayName("Should generate unique account numbers")
    void testAccountNumberGenerator_Uniqueness() {
        // Arrange
        Set<String> generatedNumbers = new HashSet<>();
        int numberOfGenerations = 1000;

        // Act
        for (int i = 0; i < numberOfGenerations; i++) {
            String accountNumber = AccountNumberGenerator.generate();
            generatedNumbers.add(accountNumber);

            // Assert individual properties
            assertNotNull(accountNumber);
            assertTrue(accountNumber.startsWith("PL"), "Account number should start with 'PL'");
            assertEquals(26, accountNumber.length(), "Account number should be 26 characters long");
        }

        // Assert uniqueness (high probability with 1000 random generations)
        assertTrue(generatedNumbers.size() > 900, 
                "Should generate mostly unique numbers (at least 90% unique out of 1000)");
    }

    @Test
    @DisplayName("Should generate reference numbers with correct format")
    void testReferenceNumberGenerator_Format() {
        // Arrange & Act
        String referenceNumber = ReferenceNumberGenerator.generate();

        // Assert
        assertNotNull(referenceNumber);
        assertTrue(referenceNumber.startsWith("REF-"), "Reference number should start with 'REF-'");
        assertTrue(referenceNumber.length() >= 16, "Reference number should be at least 16 characters long");
        assertTrue(referenceNumber.matches("REF-[A-Z0-9\\-]+"), 
                "Reference number should match pattern 'REF-' followed by alphanumeric characters and hyphens");
    }

    @Test
    @DisplayName("Should generate different reference numbers on multiple calls")
    void testReferenceNumberGenerator_Uniqueness() {
        // Arrange & Act
        String ref1 = ReferenceNumberGenerator.generate();
        String ref2 = ReferenceNumberGenerator.generate();
        String ref3 = ReferenceNumberGenerator.generate();

        // Assert
        assertNotNull(ref1);
        assertNotNull(ref2);
        assertNotNull(ref3);
        assertNotEquals(ref1, ref2);
        assertNotEquals(ref2, ref3);
        assertNotEquals(ref1, ref3);
    }

    @Test
    @DisplayName("Should generate account numbers with only digits after prefix")
    void testAccountNumberGenerator_OnlyDigits() {
        // Arrange & Act
        String accountNumber = AccountNumberGenerator.generate();
        String digitsOnly = accountNumber.substring(2); // Remove "PL" prefix

        // Assert
        assertTrue(digitsOnly.matches("\\d+"), "Account number should contain only digits after 'PL' prefix");
        assertEquals(24, digitsOnly.length(), "Should have 24 digits after prefix");
    }
}
