package dev.server.repositories;

import dev.common.models.User;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class UsersRepositoryTest {
    UsersRepository usersRepository = UsersRepository.getInstance();

    @Test
    void findByUsername() {

        User user = usersRepository.findByUsername("juan").get();

        assertAll(
                ()-> assertEquals(1, user.id()),
                ()-> assertEquals("juan", user.username()),
                ()-> assertEquals(User.Role.ADMIN, user.role())
        );

    }

    @Test
    void findByById() {
        Optional<User> user = usersRepository.findByById(2);
        assertAll(
                () -> assertTrue(user.isPresent()),
                () -> assertEquals(user.get().username(), "manolo")
        );
    }
}