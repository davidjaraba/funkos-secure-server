package dev.server.controllers;


import dev.common.models.Funko;
import dev.server.database.models.Modelo;
import dev.server.services.FunkoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class FunkoController {
    private final FunkoService funkoService;
    private final Logger logger = LoggerFactory.getLogger(FunkoController.class);

    public FunkoController(FunkoService funkoService) {
        this.funkoService = funkoService;
    }
    public Mono<Void> importCsv() {
        return funkoService.importCsv().doOnSubscribe(subscription -> logger.info("Importando funkos"));
    }

    public Flux<Funko> findAll() throws SQLException, IOException {
        return funkoService.findAll().doOnSubscribe(subscription -> logger.info("Obteniendo todos los funkos"));
    }

    public Mono<Funko> mostExpensiveFunko() throws SQLException, IOException {
        return funkoService.mostExpensiveFunko().doOnSubscribe(subscription -> logger.info("Obteniendo el funko más caro"));
    }

    public Mono<Map<Modelo, List<Funko>>> groupedByModel() throws SQLException, IOException {
        return funkoService.groupedByModel().doOnSubscribe(subscription -> logger.info("Agrupando funkos por modelo"));
    }

    public Mono<Map<Modelo, Long>> countByModel() throws SQLException, IOException {
        return funkoService.countByModel().doOnSubscribe(subscription -> logger.info("Contando funkos por modelo"));
    }

    public Flux<Funko> releasedIn2023() throws SQLException, IOException {
        return funkoService.releasedIn2023().doOnSubscribe(subscription -> logger.info("Funkos lanzados en 2023"));
    }

    public Mono<Double> averagePrice() throws SQLException, IOException {
        return funkoService.averagePrice().doOnSubscribe(subscription -> logger.info("Precio medio de los funkos"));
    }

    public Mono<Long> stitchFunkosCount() throws SQLException, IOException {
        return funkoService.stitchFunkosCount().doOnSubscribe(subscription -> logger.info("Contando funkos de Stitch"));
    }

    public Flux<Funko> stitchFunkos() throws SQLException, IOException {
        return funkoService.stitchFunkos().doOnSubscribe(subscription -> logger.info("Obteniendo funkos de Stitch"));
    }

}