package dev.server.repositories;

import dev.common.models.Funko;
import dev.server.database.models.Modelo;
import dev.server.services.database.DatabaseManager;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;

public class FunkosReactiveRepoImpl implements FunkosReactiveRepo {

    private final Logger logger = LoggerFactory.getLogger(FunkosReactiveRepoImpl.class);
    private static FunkosReactiveRepoImpl instance;
    private final DatabaseManager databaseManager;

    public static synchronized FunkosReactiveRepoImpl getInstance(DatabaseManager databaseManager) {
        if (instance == null) {
            instance = new FunkosReactiveRepoImpl(databaseManager);
        }
        return instance;
    }

    private FunkosReactiveRepoImpl(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    @Override
    public Flux<Funko> findAll() {
        logger.info("Obteniendo todos los funkos");
        String sql = "SELECT * FROM funkos";

        return Flux.usingWhen(
            databaseManager.getConnectionPool().create(),
            connection -> Flux.from(connection.createStatement(sql).execute()).flatMap(res->  res.map((row, rm)-> new Funko(row.get("cod", UUID.class),
                    row.get("nombre", String.class),
                    row.get("modelo", Modelo.class),
                    row.get("precio", Double.class),
                    row.get("fecha_lanzamiento", java.time.LocalDate.class)))
            ),
            Connection::close
        );
    }

    @Override
    public Mono<Funko> findById(UUID id) {
        logger.info("Buscando Funko con id "+id);
        String sql = "SELECT * FROM funkos WHERE cod = ?";

        return Mono.usingWhen(databaseManager.getConnectionPool().create(),
                connection -> Mono.from(connection.createStatement(sql).bind(0, id).execute()).flatMap(res->  Mono.from(res.map((row, rm)-> new Funko(row.get("cod", UUID.class),
                        row.get("nombre", String.class),
                        row.get("modelo", Modelo.class),
                        row.get("precio", Double.class),
                        row.get("fecha_lanzamiento", java.time.LocalDate.class)))
                )),
                Connection::close
                );

    }

    @Override
    public Mono<Funko> save(Funko entity) {
        logger.info("Guardando funko ");
        String sql = "INSERT INTO funkos (cod, nombre, modelo, precio, fecha_lanzamiento) VALUES (?, ?, ?, ?, ?)";

        return Mono.usingWhen(databaseManager.getConnectionPool().create(),
                connection -> Flux.from(connection.createStatement(sql).bind(0, entity.codigo()).bind(1, entity.nombre())
                        .bind(2, entity.modelo()).bind(3, entity.precio()).bind(4, entity.fechaLanzamiento()).execute()).then(Mono.just(entity)), Connection::close);

    }

    @Override
    public Mono<Funko> update(Funko entity) throws SQLException, IOException {
        logger.info("Actualizando funko ");
        String sql = "UPDATE funkos SET nombre = ?, modelo = ?, precio = ?, fecha_lanzamiento = ? WHERE cod = ?";

        return Mono.usingWhen(databaseManager.getConnectionPool().create(),
                connection -> Flux.from(connection.createStatement(sql).bind(4, entity.codigo()).bind(0, entity.nombre())
                        .bind(1, entity.modelo()).bind(2, entity.precio()).bind(3, entity.fechaLanzamiento()).execute()).then(Mono.just(entity)), Connection::close);

    }

    @Override
    public Mono<Boolean> delete(UUID id) throws SQLException, IOException {
        logger.info("Eliminado funko con id "+id);
        String sql = "DELETE FROM funko WHERE cod = ?";

        return Mono.usingWhen(databaseManager.getConnectionPool().create(),
                connection -> Flux.from(connection.createStatement(sql).bind(0, id).execute()).flatMap(Result::getRowsUpdated).hasElements() , Connection::close);


    }

    @Override
    public Mono<Void> deleteAll() {
        logger.info("Eliminando todos los funkos");
        String sql = "DELETE FROM funkos";

        return Mono.usingWhen(databaseManager.getConnectionPool().create(),
                connection -> Flux.from(connection.createStatement(sql).execute()).then(), Connection::close);

    }

    @Override
    public Mono<Funko> findByName(String name) {
        logger.info("Buscando funko por nombre");

        return Mono.usingWhen(databaseManager.getConnectionPool().create(),
                connection -> Mono.from(connection.createStatement("SELECT * FROM funkos WHERE nombre = ?").bind(0, name).execute()).flatMap(res->  Mono.from(res.map((row, rm)-> new Funko(row.get("cod", UUID.class),
                        row.get("nombre", String.class),
                        row.get("modelo", Modelo.class),
                        row.get("precio", Double.class),
                        row.get("fecha_lanzamiento", java.time.LocalDate.class)))
                )),
                Connection::close
        );

    }
}
