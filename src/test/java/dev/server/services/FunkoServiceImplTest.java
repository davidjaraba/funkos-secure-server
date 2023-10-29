package dev.server.services;

import dev.common.models.Funko;
import dev.server.database.models.Modelo;
import dev.server.exceptions.FunkoNoEncontrado;
import dev.server.exceptions.FunkoNoGuardado;
import dev.server.repositories.FunkosReactiveRepo;
import dev.server.services.FunkoServiceImpl;
import dev.server.services.cache.FunkosCacheImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FunkoServiceImplTest {


    @InjectMocks
    private FunkoServiceImpl funkoService;

    @Mock
    private FunkosReactiveRepo funkosReactiveRepo;

    @Mock
    private FunkosCacheImpl funkosCache;


    List<Funko> funkos;


    @BeforeEach
    void setUp() {



        funkos = List.of(
                new Funko(UUID.randomUUID(), "Funko 2", Modelo.OTROS, 10, LocalDate.now()),
                new Funko(UUID.randomUUID(), "Funko 3", Modelo.OTROS, 10, LocalDate.of(2020, 1, 1)),
                new Funko(UUID.randomUUID(), "Funko 4", Modelo.MARVEL, 20, LocalDate.of(2020, 1, 1))
        );
    }



    @Test
    public void findAll() throws SQLException, IOException {

        when(funkosReactiveRepo.findAll()).thenReturn(Flux.fromIterable(funkos));
        List<Funko> funkosList = funkoService.findAll().collectList().block();
        assertAll(() -> {
            assertEquals(3, funkosList.size());
            assertEquals("Funko 3", funkosList.get(1).nombre());
        });

        verify(funkosReactiveRepo, times(1)).findAll();

    }

    @Test
    public void findById() throws SQLException, IOException {

        when(funkosReactiveRepo.findById(any(UUID.class))).thenReturn(Mono.just(funkos.get(0)));
        when(funkosCache.get(any(UUID.class))).thenReturn(Mono.just(funkos.get(0)));

        assertAll(() -> {
            assertEquals("Funko 2", funkoService.findById(UUID.randomUUID()).block().nombre());
        });

        verify(funkosReactiveRepo, times(1)).findById(any(UUID.class));

    }

    @Test
    public void findByIdNotFound() throws SQLException, IOException {

        when(funkosReactiveRepo.findById(any(UUID.class))).thenReturn(Mono.empty());
        when(funkosCache.get(any(UUID.class))).thenReturn(Mono.empty());

        assertAll(() -> {
            assertThrows(FunkoNoEncontrado.class, () -> funkoService.findById(UUID.randomUUID()).block());
        });

        verify(funkosReactiveRepo, times(1)).findById(any(UUID.class));

    }

    @Test
    public void save() throws SQLException, IOException {

        when(funkosReactiveRepo.save(any(Funko.class))).thenReturn(Mono.just(funkos.get(0)));

        assertAll(() -> {
            assertEquals("Funko 2", funkoService.save(funkos.get(0)).block().nombre());
        });

        verify(funkosReactiveRepo, times(1)).save(any(Funko.class));

    }

    @Test
    public void update() throws SQLException, IOException {

        when(funkosReactiveRepo.update(any(Funko.class))).thenReturn(Mono.just(funkos.get(0)));
        when(funkosReactiveRepo.findById(any(UUID.class))).thenReturn(Mono.just(funkos.get(0)));
        when(funkosCache.get(any(UUID.class))).thenReturn(Mono.just(funkos.get(0)));

        assertAll(() -> {
            assertEquals("Funko 2", funkoService.update(funkos.get(0)).block().nombre());
        });

        verify(funkosReactiveRepo, times(1)).update(any(Funko.class));

    }

    @Test
    public void updateNotFound() throws SQLException, IOException {

        when(funkosReactiveRepo.findById(any(UUID.class))).thenReturn(Mono.empty());
        when(funkosCache.get(any(UUID.class))).thenReturn(Mono.empty());

        assertAll(() -> {
            assertThrows(FunkoNoEncontrado.class, () -> funkoService.update(funkos.get(0)).block());
        });

        verify(funkosReactiveRepo, times(0)).update(any(Funko.class));

    }

    @Test
    public void updateError() throws SQLException, IOException {

        when(funkosReactiveRepo.update(any(Funko.class))).thenThrow(new SQLException());
        when(funkosReactiveRepo.findById(any(UUID.class))).thenReturn(Mono.just(funkos.get(0)));
        when(funkosCache.get(any(UUID.class))).thenReturn(Mono.just(funkos.get(0)));

        assertAll(() -> {
            assertThrows(FunkoNoGuardado.class, () -> funkoService.update(funkos.get(0)).block());
        });

        verify(funkosReactiveRepo, times(1)).update(any(Funko.class));

    }

    @Test
    public void deleteById() throws SQLException, IOException {

        when(funkosReactiveRepo.delete(any(UUID.class))).thenReturn(Mono.just(true));
        when(funkosReactiveRepo.findById(any(UUID.class))).thenReturn(Mono.just(funkos.get(0)));
        when(funkosCache.get(any(UUID.class))).thenReturn(Mono.just(funkos.get(0)));

        assertAll(() -> {
            assertTrue(funkoService.delete(funkos.get(0)).block());
        });

        verify(funkosReactiveRepo, times(1)).delete(any(UUID.class));

    }

    @Test
    public void deleteError() throws SQLException, IOException {

        when(funkosReactiveRepo.delete(any(UUID.class))).thenThrow(new SQLException());
        when(funkosReactiveRepo.findById(any(UUID.class))).thenReturn(Mono.just(funkos.get(0)));
        when(funkosCache.get(any(UUID.class))).thenReturn(Mono.just(funkos.get(0)));

        assertAll(() -> {
            assertThrows(FunkoNoGuardado.class, () -> funkoService.delete(funkos.get(0)).block());
        });


    }


    @Test
    public void deleteByIdNotFound() throws SQLException, IOException {

        when(funkosReactiveRepo.findById(any(UUID.class))).thenReturn(Mono.empty());
        when(funkosCache.get(any(UUID.class))).thenReturn(Mono.empty());

        assertAll(() -> {
            assertThrows(FunkoNoEncontrado.class, () -> funkoService.delete(funkos.get(0)).block());
        });

        verify(funkosReactiveRepo, times(0)).delete(any(UUID.class));

    }

    @Test
    public void releaseInTest() throws SQLException, IOException {

        when(funkosReactiveRepo.findAll()).thenReturn(Flux.fromIterable(funkos));

        assertAll(() -> {
            assertEquals(2, funkoService.releasedIn(2020).collectList().block().size());
        });

        verify(funkosReactiveRepo, times(1)).findAll();


    }

    @Test
    public void groupedByModelTest() throws SQLException, IOException {

        when(funkosReactiveRepo.findAll()).thenReturn(Flux.fromIterable(funkos));

        assertAll(() -> {
            assertEquals(1, funkoService.groupedByModel().block().get(Modelo.MARVEL).size());
        });

        verify(funkosReactiveRepo, times(1)).findAll();


    }

    @Test
    public void importCSVTest() throws IOException, SQLException {

        when(funkosReactiveRepo.save(any(Funko.class))).thenReturn(Mono.just(funkos.get(0)));

        funkoService.importCsv().block();

        verify(funkosReactiveRepo, times(90)).save(any(Funko.class));

    }


}
