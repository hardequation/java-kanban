package com.kanban;
public class TaskNotFoundException extends Exception {

    public TaskNotFoundException(String message) {
        super(message);
    }
}
