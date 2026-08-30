package com.mentalhealthforum.mentalhealthforum_backend.service.impl;

import com.mentalhealthforum.mentalhealthforum_backend.dto.userProfileAndIdentity.auth.OtpResult;
import com.mentalhealthforum.mentalhealthforum_backend.enums.OtpPurpose;
import com.mentalhealthforum.mentalhealthforum_backend.exception.error.InvalidTokenException;
import com.mentalhealthforum.mentalhealthforum_backend.exception.error.TokenExpiredException;
import com.mentalhealthforum.mentalhealthforum_backend.exception.error.TooManyRequestsException;
import com.mentalhealthforum.mentalhealthforum_backend.model.OtpCredentialEntity;
import com.mentalhealthforum.mentalhealthforum_backend.repository.OtpCredentialRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.security.SecureRandom;
import java.time.Instant;

import static com.mentalhealthforum.mentalhealthforum_backend.contants.OtpConstants.*;

@Service
public class OtpWorkerImpl implements  OtpWorker{
    private final OtpCredentialRepository otpCredentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    private static final int OTP_UPPER_BOUND = (int) Math.pow(10, OTP_LENGTH);
    private static final String OTP_FORMAT = "%0" + OTP_LENGTH + "d";

    public OtpWorkerImpl(
            OtpCredentialRepository otpCredentialRepository,
            PasswordEncoder passwordEncoder) {
        this.otpCredentialRepository = otpCredentialRepository;
        this.passwordEncoder = passwordEncoder;
    }


    @Override
    public Mono<OtpResult> generateAndSaveOtp(String email, OtpPurpose purpose){
        return otpCredentialRepository.findByEmailAndPurpose(email, purpose)
                .flatMap(existingOtp -> {
                    // If the existing OTP was created LESS than 60 seconds ago, reject
                    if(existingOtp.getCreatedAt().isAfter(Instant.now().minus(OTP_RATE_LIMIT_DURATION_SECONDS))){
                        return Mono.error(new TooManyRequestsException(String.format("Please wait %d seconds before requesting a new code.", OTP_RATE_LIMIT_SECONDS)));
                    }
                    return otpCredentialRepository.delete(existingOtp);
                })

                // SwitchIfEmpty guarantees that even if a record doesn't exist,
                // downstream pipelines treat it gracefully
                .switchIfEmpty(Mono.defer(()-> {

                    // Optional: If you want protection against rapid-fire generation for non-existent accounts,
                    // you can cross-reference an IP-based cache bucket here before moving forward.
                    return Mono.empty();
                }))
                .then(Mono.defer(() -> {
                    String rawCode = String.format(OTP_FORMAT, secureRandom.nextInt(OTP_UPPER_BOUND));

                    OtpCredentialEntity newOtp = new OtpCredentialEntity(
                            null,
                            email,
                            passwordEncoder.encode(rawCode),
                            purpose,
                            Instant.now().plus(OTP_EXPIRY_DURATION_MINUTES)
                    );
                    return otpCredentialRepository.save(newOtp).thenReturn(new OtpResult(rawCode, OTP_EXPIRY_MINUTES));
        }));
    }

    @Override
    public Mono<Void> verifyOtp(String email, String rawCode, OtpPurpose purpose){
        return otpCredentialRepository.findByEmailAndPurpose(email, purpose)
                // If no OTP found, throw an error
                .switchIfEmpty(Mono.error(new InvalidTokenException()))
                .flatMap(otpCredential -> {
                    // Check the expiry
                    if(otpCredential.isExpired()){
                        return otpCredentialRepository.delete(otpCredential)
                                .then(Mono.error(new TokenExpiredException()));
                    }
                    // Check the Bycrpt match
                    if(!passwordEncoder.matches(rawCode, otpCredential.getCodeHash())){
                        return Mono.error(new InvalidTokenException());
                    }

                    //Success - Delete the OTP so it's "Single Use"
                    return otpCredentialRepository.delete(otpCredential);
                });
    }

}
