package com.manager.util.json;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

public class JsonProtocol<T> {
    private String command;
    private String to;
    private String from;
    @JsonTypeInfo(use= JsonTypeInfo.Id.CLASS)
    private T attachment;

    public JsonProtocol() {
    }

    public JsonProtocol(String command, T attachment) {
        this.command = command;
        this.attachment = attachment;
    }

    public String getCommand() {
        return command;
    }

    public T getAttachment() {
        return attachment;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JsonProtocol<?> that = (JsonProtocol<?>) o;

        if (!command.equals(that.command)) return false;
        if (!to.equals(that.to)) return false;
        if (!from.equals(that.from)) return false;
        return attachment.equals(that.attachment);

    }

    @Override
    public int hashCode() {
        int result = command.hashCode();
        result = 31 * result + to.hashCode();
        result = 31 * result + from.hashCode();
        result = 31 * result + attachment.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return JsonObjectFactory.getJsonString(this);
    }
}
