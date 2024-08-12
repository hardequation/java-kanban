package com.kanban.exception;

public class WrongTaskLogicException extends RuntimeException {
    public WrongTaskLogicException(String message) {
        super(message);
    }
}
