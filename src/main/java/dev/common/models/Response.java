package dev.common.models;


import dev.common.utils.LocalDateTimeAdapter;

import java.time.LocalDateTime;

public record Response<T>(Status status, T content, String createdAt) {


    public enum Status {
        OK, ERROR, UNAUTHORIZED, EXIT, TOKEN
    }



}