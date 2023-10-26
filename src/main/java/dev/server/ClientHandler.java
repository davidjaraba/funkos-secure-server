package dev.server;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.common.models.Login;
import dev.common.models.Request;
import dev.common.models.Response;
import dev.common.models.User;
import dev.common.utils.LocalDateAdapter;
import dev.common.utils.UuidAdapter;
import dev.server.annotations.Authorized;
import dev.server.annotations.RequestBody;
import dev.server.annotations.RequestHandler;
import dev.server.annotations.RequestToken;
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
import java.lang.reflect.Parameter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.*;

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

    public void start() {

        registerHandlers(this);

        try {

            openConnection();

            String clientInput;

            while ((clientInput = in.readLine()) != null) {
                logger.debug("request " + clientInput);
                Response<?> response = handleRequest(clientInput);
                String json = gson.toJson(response);
                out.println(json);
            }

        } catch (IOException | NoSuchMethodException e) {
            logger.error("Error: {}", e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private void openConnection() throws IOException, NoSuchMethodException {

        logger.info("Abriendo conexión con el cliente {}", clientNumber);

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
                if (method.getReturnType() != Response.class) {
                    throw new IllegalArgumentException("El método " + method.getName() + " de la clase " + clazz.getName() + " debe devolver una respuesta");
                }
                RequestHandler annotation = method.getAnnotation(RequestHandler.class);
                Request.Type requestType = annotation.value();
                handlers.put(requestType, method);
            }
        }
    }

    public Response<?> handleRequest(String request) throws Exception {

        Request<?> requestObj = gson.fromJson(request, Request.class);
        logger.debug("Procesando peticion {}", requestObj.type());
        Method handler = handlers.get(requestObj.type());

        if (handler != null) {
            DecodedJWT token = verifyToken(requestObj.token());
            if (handler.isAnnotationPresent(Authorized.class) && token == null) {
                return new Response<>(Response.Status.UNAUTHORIZED, "No autorizado", LocalDateTime.now().toString());
            }
            Parameter[] parameters = handler.getParameters();
            List<Object> args = new ArrayList<>();
            for (Parameter parameter : parameters) {
                if (parameter.isAnnotationPresent(RequestToken.class)) {
                    if (!parameter.getType().isAssignableFrom(DecodedJWT.class)) {
                        throw new IllegalArgumentException("El parámetro " + parameter.getName() + " del método " + handler.getName() + " de la clase " + handler.getDeclaringClass().getName() + " debe ser de tipo DecodedJWT");
                    }
                    args.add(token);
                }

                if (parameter.isAnnotationPresent(RequestBody.class)) {

                    String contentJson = gson.toJson(requestObj.content());
                    Object methodParameter = gson.fromJson(contentJson, parameter.getType());
                    if (!parameter.getType().isAssignableFrom(methodParameter.getClass())) {
                        throw new IllegalArgumentException("El parámetro " + parameter.getName() + " del método " + handler.getName() + " de la clase " + handler.getDeclaringClass().getName() + " debe ser de tipo " + parameter.getType().getName());
                    }
                    args.add(methodParameter);
                }
            }
            return (Response<?>) handler.invoke(this, args.toArray());
        } else {
            logger.error("Request no valida");
            return new Response<>(Response.Status.ERROR, "Request no valida", LocalDateTime.now().toString());
        }
    }

    @RequestHandler(value = Request.Type.LOGIN)
    private Response<String> login(@RequestBody Login login) {

        logger.debug("Login {}", login);


        Optional<User> user = UsersRepository.getInstance().findByByUsername(login.username());

        if (user.isEmpty() || !BCrypt.checkpw(login.password(), user.get().password())) {
            logger.warn("Usuario no encontrado o falla la contraseña");
            return new Response<>(Response.Status.ERROR, "Usuario no encontrado o falla la contraseña", LocalDateTime.now().toString());
        }

        return new Response<>(Response.Status.TOKEN, TokenService.getInstance().createToken(user.get(), Server.SECRET, Server.EXPIRATION_TIME), LocalDateTime.now().toString());

    }


    private DecodedJWT verifyToken(String token) {
        if (token == null) {
            return null;
        }
        return tokenService.verifyToken(token, Server.SECRET);
    }


}


