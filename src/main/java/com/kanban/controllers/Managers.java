package com.kanban.controllers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kanban.adapter.DurationAdapter;
import com.kanban.adapter.LocalDateTimeAdapter;
import com.kanban.controllers.HistoryManager;
import com.kanban.controllers.InMemoryHistoryManager;
import com.kanban.controllers.InMemoryTaskManager;
import com.kanban.controllers.TaskManager;

import java.time.Duration;
import java.time.LocalDateTime;

public class Managers {
    public static TaskManager getDefault() {
        return new InMemoryTaskManager();
    }

    public static HistoryManager getDefaultHistory() {
        return new InMemoryHistoryManager();
    }

    public static Gson getGson() {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter());
        builder.registerTypeAdapter(Duration.class, new DurationAdapter());
        return builder.create();
    }
}
