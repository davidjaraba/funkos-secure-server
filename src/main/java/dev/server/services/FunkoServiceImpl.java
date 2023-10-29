package dev.server.services;


import dev.common.models.Funko;
import dev.server.database.models.Modelo;
import dev.server.repositories.FunkosReactiveRepo;
import dev.server.services.cache.FunkosCache;
import dev.server.exceptions.FunkoNoEncontrado;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class FunkoServiceImpl implements FunkoService {
    private final Logger logger = LoggerFactory.getLogger(FunkoServiceImpl.class);
    private final FunkosReactiveRepo funkosReactiveRepo;
    private final FunkosCache<UUID, Funko> funkosCache;

    public FunkoServiceImpl(FunkosReactiveRepo funkosReactiveRepo, FunkosCache<UUID, Funko> funkosCache) {
        this.funkosReactiveRepo = funkosReactiveRepo;
        this.funkosCache = funkosCache;
    }

    @Override
    public Flux<Funko> findAll() throws SQLException, IOException {
        return funkosReactiveRepo.findAll();
    }

    @Override
    public Mono<Void> importCsv() {

            final String filePath = "data" + File.separator + "funkos.csv";
            if (!Files.exists(Path.of(filePath))) {
                logger.error("El fichero " + filePath + " no existe");
                return Mono.empty();
            }
            final String delimiter = ",";
            Flux.using(
                    () -> new BufferedReader(new FileReader(filePath)),
                    reader -> Flux.fromStream(reader.lines().skip(1).map(line -> {
                        String[] values = line.split(delimiter);
                        UUID uuid = UUID.fromString(values[0].substring(0, 35));
                        return new Funko(uuid, values[1], Modelo.valueOf(values[2]), Double.parseDouble(values[3]), LocalDate.parse(values[4]));
                    })),
                    reader -> {
                        try {
                            reader.close();
                        } catch (IOException e) {
                            logger.error("Error al cerrar el reader", e);
                        }
                    }).subscribe(funko -> {
                        try {
                            save(funko).subscribe();
                        } catch (SQLException | IOException e) {
                            logger.error("Error al guardar el funko " + funko, e);
                        }
                    });

            return Mono.empty();


    }

    @Override
    public Mono<Funko> findById(UUID id) throws SQLException, IOException {
        return funkosCache.get(id)
                .switchIfEmpty(
                        funkosReactiveRepo.findById(id)
                                .flatMap(funko -> funkosCache.put(funko.codigo(), funko).then(Mono.just(funko))))
                .switchIfEmpty(Mono.error(new FunkoNoEncontrado("Funko con id " + id + " no encontrado")));
    }


    @Override
    public Mono<Funko> save(Funko funko) throws SQLException, IOException {
        return funkosReactiveRepo.save(funko);
    }

    @Override
    public Mono<Boolean> delete(Funko funko) throws SQLException, IOException {
        return funkosReactiveRepo.delete(funko.codigo()).doOnSuccess(aBoolean -> funkosCache.remove(funko.codigo()));
    }

    @Override
    public Mono<Funko> update(Funko funko) throws SQLException, IOException {
        return funkosReactiveRepo.update(funko);
    }

    @Override
    public Mono<Funko> mostExpensiveFunko() throws SQLException, IOException {
        return findAll().sort(Comparator.comparingDouble(Funko::precio).reversed()).next();
    }

    @Override
    public Mono<Map<Modelo, List<Funko>>> groupedByModel() throws SQLException, IOException {
        return findAll().collect(Collectors.groupingBy(Funko::modelo));
    }

    @Override
    public Mono<Map<Modelo, Long>> countByModel() throws SQLException, IOException {
        return findAll().collect(Collectors.groupingBy(Funko::modelo, Collectors.counting()));
    }

    @Override
    public Flux<Funko> releasedIn(int year) throws SQLException, IOException {
        return findAll().filter(funko -> funko.fechaLanzamiento().getYear() == year);
    }

    @Override
    public Mono<Double> averagePrice() throws SQLException, IOException {
        return findAll().collect(Collectors.averagingDouble(Funko::precio));
    }

    @Override
    public Mono<Long> stitchFunkosCount() throws SQLException, IOException {
        return findAll().count();
    }

    @Override
    public Flux<Funko> stitchFunkos() throws SQLException, IOException {
        return findAll().filter(funko -> funko.nombre().contains("Stitch"));
    }

}