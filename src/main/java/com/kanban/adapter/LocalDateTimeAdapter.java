package com.kanban.adapter;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LocalDateTimeAdapter extends TypeAdapter<LocalDateTime> {
    @Override
    public void write(JsonWriter jsonWriter, LocalDateTime value) throws IOException {
        if (value == null) {
            jsonWriter.value("");
        } else {
            String str = value.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            jsonWriter.value(str);
        }
    }

    @Override
    public LocalDateTime read(JsonReader jsonReader) throws IOException {
        String str = jsonReader.nextString();
        if (str.isEmpty()) {
            return null;
        } else {
            return LocalDateTime.parse(str, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
    }
}
