package pl.proggo.bankapp.util;

import java.util.UUID;

/**
 * Generates unique reference numbers for transactions
 */
public class ReferenceNumberGenerator {

    public static String generate() {
        return "REF-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
    }
}