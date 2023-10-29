package dev.server;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.common.models.*;
import dev.common.utils.LocalDateAdapter;
import dev.common.utils.LocalDateTimeAdapter;
import dev.common.utils.UuidAdapter;
import dev.server.annotations.Authorized;
import dev.server.annotations.RequestBody;
import dev.server.annotations.RequestHandler;
import dev.server.annotations.RequestToken;
import dev.server.database.models.Modelo;
import dev.server.repositories.FunkosReactiveRepoImpl;
import dev.server.repositories.UsersRepository;
import dev.server.services.FunkoServiceImpl;
import dev.server.services.TokenService;
import dev.server.services.cache.FunkosCacheImpl;
import dev.server.services.database.DatabaseManager;
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
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public class ClientHandler extends Thread {

    private static Logger logger = LoggerFactory.getLogger(ClientHandler.class);
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .registerTypeAdapter(UUID.class, new UuidAdapter()).create();
    private final long clientNumber;
    private final Socket socket;
    private TokenService tokenService = TokenService.getInstance();
    private final FunkoServiceImpl funkoService;

    private static Map<Request.Type, Method> handlers = new HashMap<>();

    BufferedReader in;
    PrintWriter out;


    public ClientHandler(Socket socket, long clientNumber) {

        this.clientNumber = clientNumber;
        this.socket = socket;
        funkoService = new FunkoServiceImpl(FunkosReactiveRepoImpl.getInstance(DatabaseManager.getInstance()), new FunkosCacheImpl());

    }

    public void start() {

        registerHandlers(this);
        String clientInput;

        try {

            openConnection();


            while ((clientInput = in.readLine()) != null) {
                logger.debug("request " + clientInput);
                Response<?> response = handleRequest(clientInput);
                String json = gson.toJson(response);
                out.println(json);
            }

        } catch (IOException | NoSuchMethodException e) {
            logger.error("Error", e);
        } catch (Exception e) {
            logger.error("Error al procesar la petición", e);
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
                    String s = "El método " + method.getName() + " de " + clazz.getName() + " debe devolver una respuesta";
                    throw new IllegalArgumentException(s);
                }
                RequestHandler annotation = method.getAnnotation(RequestHandler.class);
                Request.Type requestType = annotation.value();
                handlers.put(requestType, method);
            }
        }
    }

    public Response<Object> handleRequest(String request) throws Exception {

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
            return (Response<Object>) handler.invoke(this, args.toArray());
        } else {
            logger.error("Request no valida");
            return new Response<>(Response.Status.ERROR, "Request no valida", LocalDateTime.now().toString());
        }
    }

    @RequestHandler(value = Request.Type.LOGIN)
    private Response<String> login(@RequestBody Login login) {

        logger.debug("Login {}", login);


        Optional<User> user = UsersRepository.getInstance().findByUsername(login.username());

        if (user.isEmpty() || !BCrypt.checkpw(login.password(), user.get().password())) {
            logger.warn("Usuario no encontrado o falla la contraseña");
            return new Response<>(Response.Status.ERROR, "Usuario no encontrado o falla la contraseña", LocalDateTime.now().toString());
        }

        return new Response<>(Response.Status.TOKEN, TokenService.getInstance().createToken(user.get(), Server.SECRET, Server.EXPIRATION_TIME), LocalDateTime.now().toString());

    }

    @RequestHandler(value = Request.Type.GETALL)
    @Authorized
    public Response<List<Funko>> getAllFunkos() throws SQLException, IOException {

        List<Funko> funkos = funkoService.findAll().collectList().block();

        return new Response<>(Response.Status.OK, funkos, LocalDateTime.now().toString());

    }

    @RequestHandler(value = Request.Type.GETBYID)
    @Authorized
    public Response<Funko> getFunkoById(@RequestBody UUID id) throws SQLException, IOException {

        Funko funko = funkoService.findById(id).block();

        return new Response<>(Response.Status.OK, funko, LocalDateTime.now().toString());

    }

    @RequestHandler(value = Request.Type.GETBYYEAR)
    @Authorized
    public Response<List<Funko>> getFunkoByYear(@RequestBody Integer year) throws SQLException, IOException {

        List<Funko> funkos = funkoService.releasedIn(year).collectList().block();

        return new Response<>(Response.Status.OK, funkos, LocalDateTime.now().toString());

    }

    @RequestHandler(value = Request.Type.GETBYMODELO)
    @Authorized
    public Response<List<Funko>> getByModelo(@RequestBody Modelo modelo) throws IOException, SQLException {
        Map<Modelo, List<Funko>> map = funkoService.groupedByModel().block();
        List<Funko> funkos = map.get(modelo);
        return new Response<>(Response.Status.OK, funkos, LocalDateTime.now().toString());
    }

    @RequestHandler(value = Request.Type.POST)
    @Authorized
    public Response<Funko> addFunko(@RequestBody Funko funko) throws IOException, SQLException {
        Funko newFunko = funkoService.save(funko).block();
        return new Response<>(Response.Status.OK, newFunko, LocalDateTime.now().toString());
    }

    @RequestHandler(value = Request.Type.DELETE)
    @Authorized
    public Response<String> deleteFunko(@RequestToken DecodedJWT token, @RequestBody Funko funko) throws IOException, SQLException {
        final Response<String> unathorizedResponse = new Response<>(Response.Status.UNAUTHORIZED, "No autorizado", LocalDateTime.now().toString());
        String username = token.getClaim("username").asString();
        Optional<User> userOptional = UsersRepository.getInstance().findByUsername(username);
        if (userOptional.isEmpty()) {
            return unathorizedResponse;
        }
        User user = userOptional.get();
        if (user.role() != User.Role.ADMIN) {
            return unathorizedResponse;
        }

        Boolean deleted = funkoService.delete(funko).block();
        if (deleted) {
            return new Response<>(Response.Status.OK, "Funko borrado correctamente", LocalDateTime.now().toString());
        } else {
            return new Response<>(Response.Status.ERROR, "Ha ocurrido un error al borrar el funko", LocalDateTime.now().toString());
        }
    }

    @RequestHandler(value = Request.Type.UPDATE)
    @Authorized
    public Response<Funko> updateFunko(@RequestBody Funko funko) throws SQLException, IOException {

        Funko resFunko = funkoService.update(funko).block();

        return new Response<>(Response.Status.OK, resFunko, LocalDateTime.now().toString());

    }

    private DecodedJWT verifyToken(String token) {
        if (token == null) {
            return null;
        }
        return tokenService.verifyToken(token, Server.SECRET);
    }


}


