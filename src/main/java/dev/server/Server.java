package dev.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;


public class Server {

    public static int PORT = 3000;
    public static String SECRET = "";
    public static long EXPIRATION_TIME = 0;
    private static final AtomicLong clientNumber = new AtomicLong(0);
    private static Logger logger = LoggerFactory.getLogger(Server.class);
    private static SSLServerSocket serverSocket;


    public static Properties loadProperties() {

        logger.info("cargando properties");

        Properties appProps = new Properties();

        try (FileInputStream file = new FileInputStream(Objects.requireNonNull(Server.class.getClassLoader().getResource("server.properties")).getPath())){

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
            logger.error("Error al iniciar el servidor");
            logger.error(e.getMessage());
        }

    }



}
