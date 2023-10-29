package dev.server.services.cache;


import dev.common.models.Funko;
import dev.server.database.models.Modelo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class FunkosCacheImplTest {
    private FunkosCacheImpl funkosCache;

    @BeforeEach
    void setUp() {
        funkosCache = new FunkosCacheImpl();
    }

    @Test
    void put() {
        Funko funko = new Funko(UUID.randomUUID(), "Funko", Modelo.OTROS, 10, LocalDate.now());
        funkosCache.put(funko.codigo(), funko).block();
        assertEquals(1, funkosCache.size());
    }

    @Test
    void get() {
        Funko funko = new Funko(UUID.randomUUID(), "Funko", Modelo.OTROS, 10, LocalDate.now());
        funkosCache.put(funko.codigo(), funko).block();
        assertEquals(funko, funkosCache.get(funko.codigo()).block());
    }

    @Test
    void getNotExists() {
        assertNull(funkosCache.get(UUID.randomUUID()).block());
    }

    @Test
    void remove() {
        Funko funko = new Funko(UUID.randomUUID(), "Funko", Modelo.OTROS, 10, LocalDate.now());
        funkosCache.put(funko.codigo(), funko).block();
        funkosCache.remove(funko.codigo()).block();
        assertEquals(0, funkosCache.size());
    }

    @Test
    void clear() {
        Funko funko = new Funko(UUID.randomUUID(), "Funko", Modelo.OTROS, 10, LocalDate.now());
        funkosCache.put(funko.codigo(), funko).block();
        funkosCache.clear();
        assertEquals(0, funkosCache.size());
    }
}