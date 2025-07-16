package com.acainfo.mvp.exception.auth;

public class EmailAlreadyExistsException extends AuthenticationException {
    public EmailAlreadyExistsException(String message) {
        super(message);
    }
}
