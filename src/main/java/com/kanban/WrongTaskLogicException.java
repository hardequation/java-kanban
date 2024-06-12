package com.kanban;

public class WrongTaskLogicException extends RuntimeException {
    public WrongTaskLogicException(String message) {
        super(message);
    }
}
