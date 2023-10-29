package dev.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.common.models.*;
import dev.common.utils.LocalDateAdapter;
import dev.common.utils.LocalDateTimeAdapter;
import dev.common.utils.UuidAdapter;
import dev.server.Server;
import dev.server.database.models.Modelo;
import org.apache.ibatis.logging.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public class Client {

    private static String HOST = "localhost";
    private static int PORT = 3000;
    private static final Logger logger = LoggerFactory.getLogger(Client.class);
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .registerTypeAdapter(UUID.class, new UuidAdapter()).create();
    private SSLSocket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String token;

    public static void main(String[] args) throws IOException {
        Client client = new Client();
        try {
            client.start();
        } catch (IOException e) {
            logger.error("Error: " + e.getMessage());
        }

    }

    public static Properties loadProperties() {

        logger.info("cargando properties");

        Properties appProps = new Properties();

        try (FileInputStream file = new FileInputStream(Objects.requireNonNull(Server.class.getClassLoader().getResource("client.properties")).getPath())) {

            appProps.load(file);

        } catch (Exception e) {
            logger.error("Error al cargar la configuracion " + e);
        }

        PORT = Integer.parseInt(appProps.getProperty("host.port"));
//        HOST = appProps.getProperty("host.address");

        return appProps;

    }

    public void initClient() throws IOException {

        logger.info("init client");
        Properties props = loadProperties();

        logger.debug("Cargando fichero de propiedades");
        System.setProperty("javax.net.ssl.trustStore", props.getProperty("keyFile"));
        System.setProperty("javax.net.ssl.trustStorePassword", props.getProperty("keyPassword"));

        SSLSocketFactory clientFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        socket = (SSLSocket) clientFactory.createSocket(HOST, PORT);

        socket.setEnabledCipherSuites(new String[]{"TLS_AES_128_GCM_SHA256"});
        socket.setEnabledProtocols(new String[]{"TLSv1.3"});

        logger.debug("Conectando a" + HOST + ":" + PORT);

    }

    private void openConnection() throws IOException {

        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        logger.info("Conexion establecido con " + HOST + ":" + PORT);

    }

    public void closeConnection() throws IOException {

        in.close();
        out.close();
        socket.close();

    }

    public void start() throws IOException {

        final String logMessage = "Funko: {}";
        initClient();
        openConnection();

        token = sendLoginRequest();

        Response<List<Funko>> allFunkos = sendRequest(null, Request.Type.GETALL);

        logger.info(logMessage, allFunkos.content());

        Response<Funko> funkoById = sendRequest(UUID.fromString("3b6c6f58-79b9-434b-82ab-01a2d6e4434"), Request.Type.GETBYID);

        logger.info(logMessage, funkoById.content());

        Response<Funko> funkoByModel = sendRequest(Modelo.DISNEY, Request.Type.GETBYMODELO);

        logger.info(logMessage, funkoByModel.content());

        Response<Funko> funkoByYear = sendRequest(2022, Request.Type.GETBYYEAR);

        logger.info(logMessage, funkoByYear.content());
        UUID funkoUuid = UUID.randomUUID();
        Response<Funko> insertedFunko = sendRequest(new Funko(funkoUuid, "FunkoInsertado", Modelo.DISNEY, 10.0, LocalDate.now()), Request.Type.POST);

        logger.info(logMessage, insertedFunko.content());
Funko funko = new Funko(funkoUuid, "FunkoActualizado", Modelo.DISNEY, 500.0, LocalDate.now());
        Response<Funko> updatedFunko = sendRequest(funko, Request.Type.UPDATE);

        logger.info(logMessage, updatedFunko.content());

        Response<String> deletedFunko = sendRequest(funko, Request.Type.DELETE);

        logger.info(logMessage, deletedFunko.content());

        closeConnection();

    }

    public String sendLoginRequest() {

        Login request =  new Login("juan", "juan1234");
        Response<String> response = sendRequest(request, Request.Type.LOGIN);
        if (response == null) {
            logger.error("Error al enviar la petición de login");
            return null;
        }
        return response.content();

    }


    private <T, R> Response<R> sendRequest(T content, Request.Type type) {
        out.println(gson.toJson(new Request<>(type, content, token, LocalDateTime.now().toString())));
        try {
            return gson.fromJson(in.readLine(), new TypeToken<Response<R>>() {
            }.getType());
        } catch (IOException e) {
            logger.error("Error: {}", e.getMessage());
        }
        return null;
    }


}
