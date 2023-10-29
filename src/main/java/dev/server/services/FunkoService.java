package dev.server.services;


import dev.common.models.Funko;
import dev.server.database.models.Modelo;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface FunkoService {
    Flux<Funko> findAll() throws SQLException, IOException;

    Mono<Void> importCsv();

    Mono<Funko> findById(UUID id) throws SQLException, IOException;

    Mono<Funko> save(Funko funko) throws SQLException, IOException;

    Mono<Boolean> delete(Funko funko) throws SQLException, IOException;

    Mono<Funko> update(Funko funko) throws SQLException, IOException;
    Mono<Map<Modelo, List<Funko>>> groupedByModel() throws SQLException, IOException;
    Flux<Funko> releasedIn(int year) throws SQLException, IOException;


}