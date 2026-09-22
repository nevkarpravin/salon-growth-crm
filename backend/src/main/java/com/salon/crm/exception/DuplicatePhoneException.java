package com.salon.crm.exception;

public class DuplicatePhoneException extends RuntimeException {
    public DuplicatePhoneException(String phone) {
        super("A client with phone '" + phone + "' already exists");
    }
}
