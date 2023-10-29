package dev.server;

import dev.server.repositories.FunkosReactiveRepoImpl;
import dev.server.services.FunkoServiceImpl;
import dev.server.services.cache.FunkosCacheImpl;
import dev.server.services.database.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicLong;


public class Server {

    public static int PORT = 3000;
    public static String SECRET = "";
    public static long EXPIRATION_TIME = 100;
    private static final AtomicLong clientNumber = new AtomicLong(0);
    private static Logger logger = LoggerFactory.getLogger(Server.class);
    private static SSLServerSocket serverSocket;


    public static Properties loadProperties() {

        logger.info("cargando properties");

        Properties appProps = new Properties();

        try (InputStream file = Server.class.getClassLoader().getResourceAsStream("server.properties")){

            appProps.load(file);

        }catch (Exception e) {
            logger.error("Error al cargar la configuracion " + e);
        }

        PORT = Integer.parseInt(appProps.getProperty("server.port"));

        return appProps;

    }

    public static void initServer() throws IOException {

        Properties props = loadProperties();

        System.setProperty("javax.net.ssl.keyStore", props.getProperty("keyFile"));
        System.setProperty("javax.net.ssl.keyStorePassword", props.getProperty("keyPassword"));

        SSLServerSocketFactory serverFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        serverSocket = (SSLServerSocket) serverFactory.createServerSocket(PORT);

        serverSocket.setEnabledCipherSuites(new String[]{"TLS_AES_128_GCM_SHA256"});
        serverSocket.setEnabledProtocols(new String[]{"TLSv1.3"});

        SECRET = props.getProperty("tokenSecret");
        EXPIRATION_TIME = Long.parseLong(props.getProperty("tokenExpiration"));

    }

    public static void run() {

        logger.info("Iniciando servidor en el puerto " + PORT);

        try {

            initServer();

            logger.info("Servidor iniciado :)");

            while (true) {
                new ClientHandler(serverSocket.accept() , clientNumber.incrementAndGet()).start();
            }

        }catch (Exception e){
            logger.error("Error al iniciar el servidor", e);
        }

    }

    public static void main(String[] args) {

        logger.info("Inicializando servidor...");

        DatabaseManager databaseManager = DatabaseManager.getInstance();

        FunkosReactiveRepoImpl funkosReactiveRepo = FunkosReactiveRepoImpl.getInstance(databaseManager);

        FunkosCacheImpl funkoCache = new FunkosCacheImpl();

        FunkoServiceImpl funkoService = new FunkoServiceImpl(funkosReactiveRepo, funkoCache);

        funkoService.importCsv().block();


        Server.run();


    }



}
