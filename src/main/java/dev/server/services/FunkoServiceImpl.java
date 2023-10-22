package dev.server.services;


import dev.common.models.Funko;
import dev.server.database.models.Modelo;
import dev.server.exceptions.FunkoNoEncontrado;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class FunkosServiceImpl {

    private final Logger logger = LoggerFactory.getLogger(FunkosServiceImpl.class);

    private static FunkosServiceImpl instance;
    private final FunkosAsyncRepoImpl repository;
    private final FunkosCacheImpl cache;
    private final DatabaseManager databaseManager;
    private final FunkosStorageImpl storage;
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);



    private FunkosServiceImpl(FunkosAsyncRepoImpl repo, DatabaseManager databaseManager, FunkosCacheImpl cache, FunkosStorageImpl storage) {

        this.cache = cache;
        this.repository = repo;
        this.databaseManager = databaseManager;
        this.storage = storage;

    }

    public static synchronized FunkosServiceImpl getInstance(FunkosAsyncRepoImpl repo, DatabaseManager databaseManager, FunkosCacheImpl cache, FunkosStorageImpl storage){
        if (instance == null) {
            instance = new FunkosServiceImpl(repo, databaseManager, cache, storage);
        }
        return instance;
    }

    public List<Funko> findAll() throws ExecutionException, InterruptedException {
        logger.info("Obteniendo todos los funkos");
        return repository.findAll().get();
    }

    public Optional<Funko> findByName(String name) throws ExecutionException, InterruptedException {
        logger.info("Obteniendo funko con nombre: "+name);
        return repository.findByName(name).get();
    }

    public Optional<Funko> findById(UUID id) throws ExecutionException, InterruptedException {
        logger.info("Obtenido funko con id "+id);
        Funko funko = cache.get(id);
        if (funko != null){
            return Optional.of(funko);
        }else{
            return repository.findById(id).get();
        }
    }

    public Funko save(Funko funko) throws ExecutionException, InterruptedException {
        logger.info("Guardando funko con id "+funko.codigo());
        repository.save(funko).get();
        cache.put(funko.codigo(), funko);
        return funko;
    }

    public Funko update(UUID id, Funko funko) throws ExecutionException, InterruptedException {
        logger.info("Actualizando funko con id "+funko.codigo());
        repository.update(id, funko).get();
        cache.put(funko.codigo(), funko);
        return funko;
    }

    public boolean delete(UUID id) throws ExecutionException, InterruptedException {
        logger.info("Eliminando funko con id "+id);
        boolean deleted = repository.delete(id).get();
        if (deleted){
            cache.remove(id);
        }
        return deleted;
    }

    public Optional<Funko> mostExpensiveFunko() throws ExecutionException, InterruptedException {
        List<Funko> funkos = this.findAll();
        return funkos.stream().max(Comparator.comparingDouble(Funko::precio));
    }

    public double averagePrice() throws ExecutionException, InterruptedException {
        List<Funko> funkos = this.findAll();
        return funkos.stream().mapToDouble(Funko::precio).average().orElse(0.0);
    }

    public Map<Modelo, List<Funko>> funkosGroupedByModel() throws ExecutionException, InterruptedException {
        List<Funko> funkos = this.findAll();
        return funkos.stream().collect(Collectors.groupingBy(Funko::modelo));
    }

    public Map<Modelo, Integer> numFunkosGroupedByModel() throws ExecutionException, InterruptedException {
        List<Funko> funkos = this.findAll();
        return funkos.stream().collect(Collectors.groupingBy(Funko::modelo, Collectors.summingInt(e -> 1)));
    }

    public List<Funko> funkosReleasedInYear(int ano) throws ExecutionException, InterruptedException {
        List<Funko> funkos = this.findAll();
        return funkos.stream().filter(fk -> fk.fechaLanzamiento().getYear() == ano).toList();
    }

    public List<Funko> funkosContainWord(String palabra) throws ExecutionException, InterruptedException {
        List<Funko> funkos = this.findAll();
        return funkos.stream().filter(fk -> fk.nombre().toLowerCase().contains(palabra.toLowerCase())).toList();
    }

}
