package dev.server.services.cache;


import dev.common.models.Funko;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FunkosCacheImpl implements FunkosCache<UUID, Funko>{

    private final Logger logger = LoggerFactory.getLogger(FunkosCacheImpl.class);

    private final int CACHE_SIZE = 15;
    private final Map<UUID, Funko> cache;
    private final ScheduledExecutorService executorService;


    public FunkosCacheImpl(){
        this.cache = Collections.synchronizedMap(new LinkedHashMap<>(CACHE_SIZE, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<UUID, Funko> eldest) {
                return size() > CACHE_SIZE;
            }
        });

        this.executorService = Executors.newSingleThreadScheduledExecutor();
        this.executorService.scheduleAtFixedRate(this::clear, 90, 90, TimeUnit.SECONDS);

    }

    @Override
    public Mono<Void> put(UUID id, Funko value) {
        logger.info("Añadiendo funko a la cache con id: "+id);
        return Mono.fromRunnable(() -> cache.put(id, value));
    }

    @Override
    public Mono<Funko> get(UUID id) {
        logger.info("Obteniendo funko de la cache con id: "+id);
        return Mono.fromCallable(() -> cache.get(id));
    }

    @Override
    public Mono<Void> remove(UUID id) {
        logger.info("Eliminando funko de la cache con id: "+id);
        return Mono.fromRunnable(() -> cache.remove(id));
    }

    @Override
    public void clear() {
        logger.info("Vaciando cache");
        cache.clear();
    }

    @Override
    public void shutdown() {
        logger.info("quitando cache");
        executorService.shutdown();
    }

    public int size() {
        return cache.size();
    }
}
