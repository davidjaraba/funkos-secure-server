package dev.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.common.models.Login;
import dev.common.models.Request;
import dev.common.models.Response;
import dev.common.models.User;
import dev.common.utils.LocalDateAdapter;
import dev.common.utils.UuidAdapter;
import dev.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;

public class Client {

    private static String HOST = "localhost";
    private static int PORT = 3000;
    private static final Logger logger = LoggerFactory.getLogger(Client.class);
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateAdapter())
            .registerTypeAdapter(UUID.class, new UuidAdapter()).create();
    private SSLSocket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String token;

    public static void main(String[] args) {
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

        try (FileInputStream file = new FileInputStream(Objects.requireNonNull(Server.class.getClassLoader().getResource("client.properties")).getPath())){

            appProps.load(file);

        }catch (Exception e) {
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

        initClient();

        openConnection();

        token = sendLoginRequest();


    }

    public String sendLoginRequest() {

        Request request = new Request(Request.Type.LOGIN, gson.toJson(new Login("manolo", "manolo1234")), null, LocalDateTime.now().toString());

        out.println(gson.toJson(request));

        try{
            logger.debug("Esperando respuesta del servidor");

            Response response = gson.fromJson(in.readLine(), new TypeToken<Response>() {
            }.getType());

            logger.debug("Respuesta "+response);

            return response.content().toString();

        }catch (Exception e){
            logger.error("Error "+e);
        }

        return null;

    }



}
