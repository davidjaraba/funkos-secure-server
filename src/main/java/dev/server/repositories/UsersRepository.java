package dev.server.repositories;


import dev.common.models.User;
import org.mindrot.jbcrypt.BCrypt;

import java.util.List;
import java.util.Optional;

public class UsersRepository {
    private static UsersRepository INSTANCE = null;
    private final List<User> users = List.of(
            new User(
                    1,
                    "juan",
                    BCrypt.hashpw("juan1234", BCrypt.gensalt(12)),
                    User.Role.ADMIN
            ),
            new User(
                    2,
                    "manolo",
                    BCrypt.hashpw("manolo1234", BCrypt.gensalt(12)),
                    User.Role.USER
            )
    );

    private UsersRepository() {
    }

    public synchronized static UsersRepository getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new UsersRepository();
        }
        return INSTANCE;
    }

    public Optional<User> findByUsername(String username) {
        return users.stream()
                .filter(user -> user.username().equals(username))
                .findFirst();
    }

    public Optional<User> findByById(int id) {
        return users.stream()
                .filter(user -> user.id() == id)
                .findFirst();
    }
}