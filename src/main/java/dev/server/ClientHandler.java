package dev.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.common.models.Login;
import dev.common.models.Request;
import dev.common.models.Response;
import dev.common.models.User;
import dev.common.utils.LocalDateAdapter;
import dev.common.utils.UuidAdapter;
import dev.server.repositories.UsersRepository;
import dev.server.services.TokenService;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.Socket;
import java.rmi.ServerException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ClientHandler extends Thread {

    private static Logger logger = LoggerFactory.getLogger(ClientHandler.class);
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateAdapter())
            .registerTypeAdapter(UUID.class, new UuidAdapter()).create();
    private final long clientNumber;
    private final Socket socket;
    private TokenService tokenService = TokenService.getInstance();

    private static Map<Request.Type, Method> handlers = new HashMap<>();

    BufferedReader in;
    PrintWriter out;


    public ClientHandler(Socket socket, long clientNumber) {

        this.clientNumber = clientNumber;
        this.socket = socket;

    }

    public void start(){

        registerHandlers(this);

        try {

            openConnection();

            String clientInput;
            Request request;

            while (true) {
                clientInput = in.readLine();
                logger.debug("request " + clientInput);
                request = gson.fromJson(clientInput, Request.class);
                out.println(gson.toJson(handleRequest(request)));
            }

        } catch (IOException | NoSuchMethodException e) {
            logger.error("Error: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private void openConnection() throws IOException, NoSuchMethodException {

        logger.info("Abriendo conexión con el cliente " + clientNumber);

        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

    }

    private void closeConnection() throws IOException {

        in.close();
        out.close();
        socket.close();

    }

    public static void registerHandlers(Object obj) {
        Class<?> clazz = obj.getClass();
        Method[] methods = clazz.getDeclaredMethods();

        for (Method method : methods) {
//             && method.getParameterCount() == 1
            if (method.isAnnotationPresent(RequestHandler.class)) {
                RequestHandler annotation = method.getAnnotation(RequestHandler.class);
                Request.Type requestType = annotation.value();
                handlers.put(requestType, method);
            }
        }
    }

    public Response<?> handleRequest(Request request) throws Exception {

        logger.debug("Procesando peticion " + request.type());

        Method handler = handlers.get(request.type());

        if (handler != null) {
            return (Response<?>) handler.invoke(this, request.content());
        } else {
            logger.error("Request no valida");
            return new Response<>(Response.Status.ERROR, "Request no valida", LocalDateTime.now().toString());
        }
    }

    @RequestHandler(value = Request.Type.LOGIN)
    @Authorized(roles = {"USER", "ADMIN"})
    private Response<String> login(String content) {

        logger.debug("Login "+content);

        Login login = gson.fromJson(content, Login.class);

        Optional<User> user = UsersRepository.getInstance().findByByUsername(login.username());

        if (user.isEmpty() || !BCrypt.checkpw(login.password(), user.get().password())) {
            logger.warn("Usuario no encontrado o falla la contraseña");
            return new Response<>(Response.Status.ERROR, "Usuario no encontrado o falla la contraseña", LocalDateTime.now().toString());
        }

        return new Response<>(Response.Status.TOKEN, TokenService.getInstance().createToken(user.get(), Server.SECRET, Server.EXPIRATION_TIME), LocalDateTime.now().toString());

    }






}


