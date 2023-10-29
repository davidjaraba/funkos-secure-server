package dev.server.services.database;

import dev.server.Server;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.io.*;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;



public class DatabaseManager {

    private static DatabaseManager instance;
    private final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
    String url = "";
    boolean initTables = false;

    private final List<String> initScripts = List.of("remove.sql", "init.sql");
    private final ConnectionFactory connectionFactory;
    private final ConnectionPool connectionPool;


    private DatabaseManager() {

        loadProperties();

        connectionFactory = ConnectionFactories.get(url);

        ConnectionPoolConfiguration configuration = ConnectionPoolConfiguration.builder(connectionFactory)
                .maxIdleTime(java.time.Duration.ofMinutes(3))
                .maxSize(20)
                .build();

        connectionPool = new ConnectionPool(configuration);


        if (initTables){
            initTables();
        }


    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public void loadProperties() {

        try {
            Properties appProps = new Properties();
            appProps.load(getClass().getClassLoader().getResourceAsStream("database.properties"));
            url = appProps.getProperty("db.stringDB");
            initTables = Boolean.parseBoolean(appProps.getProperty("db.loadTables", "false"));
        } catch (Exception e) {
            logger.error("Error al cargar el fichero de propiedades ",e);
        }

    }

    public void initTables(){

        Flux.fromIterable(initScripts).concatMap(sc -> Mono.usingWhen(connectionFactory.create(),
                connection -> {
                    logger.debug("Creando conexión con la base de datos");
                    String scriptContent = null;
                    try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(sc)) {
                        if (inputStream == null) {
                            return Mono.error(new IOException("No se ha encontrado el fichero de script de inicialización de la base de datos"));
                        } else {
                            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                                scriptContent = reader.lines().collect(Collectors.joining("\n"));
                            }
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    Statement statement = connection.createStatement(scriptContent);
                    return Mono.from(statement.execute());
                },
                Connection::close).then()).doOnNext(res -> logger.info("Script ejecutado correctamente")

        ).doOnComplete(()-> logger.info("Se han ejecutado todos los scripts")).subscribe();

    }


    public ConnectionPool getConnectionPool() {
        return connectionPool;
    }




}
