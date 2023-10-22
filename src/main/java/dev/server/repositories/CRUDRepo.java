package dev.server.repositories;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.sql.SQLException;

public interface CRUDRepo<T, ID> {

    Flux<T> findAll() throws SQLException, IOException;

    Mono<T> findById(ID id) throws SQLException, IOException;

    Mono<T> save(T entity) throws SQLException, IOException;

    Mono<T> update(T entity) throws SQLException, IOException;

    Mono<Boolean> delete(ID id) throws SQLException, IOException;

    Mono<Void> deleteAll();

}