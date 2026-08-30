package com.mentalhealthforum.mentalhealthforum_backend.validation.otp;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import static com.mentalhealthforum.mentalhealthforum_backend.contants.OtpConstants.OTP_LENGTH;

public class OtpValidator implements ConstraintValidator<ValidOtp, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // @NotBlank handle null or empty values to avoid overlapping
        if(value == null || value.isBlank()){
            return true;
        }

        boolean isValidLength = value.length() == OTP_LENGTH;
        boolean isNumeric = value.matches("^\\d+$");

        if(!isValidLength || !isNumeric){

            // Disable standard static default message
            context.disableDefaultConstraintViolation();

            context.buildConstraintViolationWithTemplate(String.format("OTP must be exactly %d digits", OTP_LENGTH))
                    .addConstraintViolation();

            return false;
        }

        return true;
    }

}
