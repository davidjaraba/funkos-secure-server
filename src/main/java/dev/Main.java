package dev;

import dev.server.Server;
import dev.server.controllers.FunkoController;
import dev.server.repositories.FunkosReactiveRepoImpl;
import dev.server.services.FunkoServiceImpl;
import dev.server.services.cache.FunkosCacheImpl;
import dev.server.services.database.DatabaseManager;

public class Main {
    public static void main(String[] args) {


        System.out.println("Hello world!");

        DatabaseManager databaseManager = DatabaseManager.getInstance();

        FunkosReactiveRepoImpl funkosReactiveRepo = FunkosReactiveRepoImpl.getInstance(databaseManager);

        FunkosCacheImpl funkoCache = new FunkosCacheImpl();

        FunkoServiceImpl funkoService = new FunkoServiceImpl(funkosReactiveRepo, funkoCache);

        FunkoController funkoController = new FunkoController(funkoService);

        funkoController.importCsv();



        Server.run();


    }
}