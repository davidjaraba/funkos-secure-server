package dev.common.models;

public record Request(Type type, String content, String token, String createdAt) {
    public enum Type {
        LOGIN, FECHA, UUID, SALIR, OTRO, GETALL, GETBYID, GETBYUUID, POST, UPDATE, DELETE, DELETEALL
    }
}