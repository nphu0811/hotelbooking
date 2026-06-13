package com.example.demo.service;

public class AccountNotLinkedException extends BusinessException {
    public AccountNotLinkedException(String message) {
        super(message);
    }
}
