package com.mentalhealthforum.mentalhealthforum_backend.utils;

public final class MaskEmailUtils {

    private static final String MASK = "*****";
    private static final int DEFAULT_VISIBLE_CHARS = 1;

    private MaskEmailUtils(){
        // Prevent instantiation
    }

    /**
     * Masks an email address for privacy.
     * Examples:
     * - john.doe@gmail.com → j****e@gmail.com
     * - alice@domain.com → a****e@domain.com
     * - a@domain.com → a@domain.com (too short to mask)
     * - null → null
     * - invalid@ → invalid@ (no domain)
     *
     * @param email The email to mask
     * @return Masked email, or original if masking isn't possible
     */
    public static String maskEmail(String email){
        return maskEmail(email, DEFAULT_VISIBLE_CHARS);
    }

    public static String maskEmail(String email, int visibleChars){
        if(email == null || !email.contains("@") || email.indexOf("@") == 0){
            return email;
        }

        int atIndex = email.indexOf('@');
        String local = email.substring(0, atIndex);
        String domain = email.substring(atIndex);

        if(local.length() <= visibleChars * 2){
            return email; // Too short to mask
        }

        String start = local.substring(0, visibleChars);
        String end = local.substring(local.length() - visibleChars);

        return start + MASK + end + domain;

    }

    public static String getReadableMaskedEmail(String maskedEmail){
        if(maskedEmail == null || maskedEmail.isBlank()){
            return "email address";
        }

        return maskedEmail.replace("@", " at ")
                .replace(".", " dot ")
                .replace(MASK, " asterisk ");
    }

    public static boolean isMaskedEmail(String email) {
        return email != null && email.contains(MASK);
    }

}
