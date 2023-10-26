package dev.common.models;


public record Response<T>(Status status, T content, String createdAt) {
    public enum Status {
        OK, ERROR, UNAUTHORIZED, EXIT, TOKEN
    }
}