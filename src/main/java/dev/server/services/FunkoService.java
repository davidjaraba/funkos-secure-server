package dev.services;


import dev.database.models.Funko;
import dev.database.models.Modelo;

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

    Mono<Funko> mostExpensiveFunko() throws SQLException, IOException;

    Mono<Map<Modelo, List<Funko>>> groupedByModel() throws SQLException, IOException;

    Mono<Map<Modelo, Long>> countByModel() throws SQLException, IOException;

    Flux<Funko> releasedIn2023() throws SQLException, IOException;

    Mono<Double> averagePrice() throws SQLException, IOException;

    Mono<Long> stitchFunkosCount() throws SQLException, IOException;

    Flux<Funko> stitchFunkos() throws SQLException, IOException;

    Mono<Void> backup();

}