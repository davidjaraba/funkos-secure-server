package dev.common.models;

import dev.server.database.models.Modelo;
import dev.server.locale.EspanaLocale;

import java.time.LocalDate;
import java.util.UUID;

public record Funko(UUID codigo, String nombre, Modelo modelo, double precio, LocalDate fechaLanzamiento) {


    @Override
    public String toString() {

        return String.format("Código: %s\n Nombre: %s\nModelo: %s\nPrecio: %s\nFecha de lanzamiento: %s\n",
                codigo, nombre, modelo, EspanaLocale.toLocalMoney(precio), EspanaLocale.toLocalDate(fechaLanzamiento));

    }

}
