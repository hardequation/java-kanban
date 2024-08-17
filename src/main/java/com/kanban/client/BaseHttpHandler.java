package com.kanban.client;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public abstract class BaseHttpHandler implements HttpHandler {

    protected static final int SUCCESS = 200;

    protected static final int SUCCESS_NO_DATA = 201;

    protected static final int NOT_FOUND = 404;

    protected static final int NOT_ACCEPTABLE = 406;

    protected static final int INTERNAL_SERVER_ERROR = 500;

    private static final String CONTENT_TYPE = "Content-Type";

    private static final String CONTENT_TYPE_VALUE = "application/json;charset=utf-8";

    protected void sendText(HttpExchange h, String text, int statusCode) throws IOException {
        byte[] resp = text.getBytes(StandardCharsets.UTF_8);
        h.getResponseHeaders().add(CONTENT_TYPE, CONTENT_TYPE_VALUE);
        h.sendResponseHeaders(statusCode, resp.length);
        h.getResponseBody().write(resp);
        h.close();
    }

    protected Integer getIdFromPath(String path) {
        String[] parts = path.split("/");
        if (parts.length >= 3) {
            return Integer.parseInt(parts[2]);
        }
        return null;
    }
}
