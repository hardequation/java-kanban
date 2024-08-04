package com.kanban.exception;

public class WrongFileFormatException extends RuntimeException {

    public WrongFileFormatException(String message) {
        super(message);
    }
}
