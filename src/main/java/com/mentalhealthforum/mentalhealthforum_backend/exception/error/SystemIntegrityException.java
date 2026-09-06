package com.mentalhealthforum.mentalhealthforum_backend.exception.error;

import com.mentalhealthforum.mentalhealthforum_backend.enums.ErrorCode;

public class SystemIntegrityException extends ApiException {

    public SystemIntegrityException(String message) {

        super(message, ErrorCode.SYSTEM_INTEGRITY_VIOLATION);
    }

    public SystemIntegrityException(String message, Throwable cause) {
        super(message, ErrorCode.SYSTEM_INTEGRITY_VIOLATION, cause);
    }
}