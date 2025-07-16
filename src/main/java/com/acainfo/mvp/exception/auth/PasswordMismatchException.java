package com.acainfo.mvp.exception.auth;

public class PasswordMismatchException extends AuthenticationException {
    public PasswordMismatchException(String message) {
        super(message);
    }
}
