package com.kanban.adapter;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.Duration;

public class DurationAdapter extends TypeAdapter<Duration> {
    @Override
    public void write(JsonWriter jsonWriter, Duration duration) throws IOException {
        if (duration == null) {
            jsonWriter.value("");
        } else {
            jsonWriter.value(duration.toMinutes());
        }
    }

    @Override
    public Duration read(JsonReader jsonReader) throws IOException {
        String value = jsonReader.nextString();
        if (value.isEmpty()) {
            return null;
        } else {
            long minutes = Long.parseLong(value);
            return Duration.ofMinutes(minutes);
        }
    }
}
