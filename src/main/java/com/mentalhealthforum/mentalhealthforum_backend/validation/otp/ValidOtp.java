package com.mentalhealthforum.mentalhealthforum_backend.validation.otp;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Size;

import java.lang.annotation.*;

@Target({ElementType.FIELD,  ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = OtpValidator.class)
@Documented
public @interface ValidOtp {
    String message() default "Invalid OTP code";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
