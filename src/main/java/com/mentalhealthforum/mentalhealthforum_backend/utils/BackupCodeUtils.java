package com.mentalhealthforum.mentalhealthforum_backend.utils;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.mentalhealthforum.mentalhealthforum_backend.contants.MfaConstants.MFA_BACKUP_CODE_COUNT;
import static com.mentalhealthforum.mentalhealthforum_backend.contants.MfaConstants.MFA_BACKUP_CODE_LENGTH;


public final class BackupCodeUtils {

    private BackupCodeUtils(){
        // Prevent instantiation
    }

    // Pre-build format string to avoid building it inside the loop
    private static final String FORMAT_STRING = "%0" + MFA_BACKUP_CODE_LENGTH + "d";

    // Calculate upper bound based on the length (e.g., 10^8 = 100,000,000)
    // Using long to prevent int overflow up to 18 digits
    private static final long UPPER_BOUND = (long) Math.pow(10, MFA_BACKUP_CODE_LENGTH);

    public static List<String> generateBackupCodes(){
        SecureRandom random = new SecureRandom();
        return IntStream.range(0, MFA_BACKUP_CODE_COUNT)
                .mapToObj(i -> generateBackupCode(random))
                .collect(Collectors.toList());
    }

    private static String generateBackupCode(SecureRandom random) {
        return String.format(FORMAT_STRING, random.nextLong(UPPER_BOUND));
    }

    public static List<String> hashBackupCodes(List<String> codes, PasswordEncoder encoder){
        return codes.stream()
                .map(encoder::encode)
                .collect(Collectors.toList());
    }
}
