package pl.proggo.bankapp.util;

import java.util.Random;

/**
 * Generates IBAN-like account numbers: PLxxxxxxxxxxxxxxxxxxxxxxxx
 */
public class AccountNumberGenerator {

    private static final String PREFIX = "PL";
    private static final Random random = new Random();
    private static String accountNumber;                       // <<<------ TUTAJ WSTAWIŁEM TO SAM

    public static String generate() {
        StringBuilder sb = new StringBuilder(PREFIX);
        for (int i = 0; i < 24; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}