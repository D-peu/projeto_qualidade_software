package br.com.projeto_qualidade_software.projeto.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {

    private final PasswordHasher passwordHasher = new PasswordHasher();

    @Test
    void hashesPasswordsWithDifferentSaltsAndVerifiesThem() {
        String firstHash = passwordHasher.encode("Professor@123");
        String secondHash = passwordHasher.encode("Professor@123");

        assertNotEquals(firstHash, secondHash);
        assertTrue(passwordHasher.matches("Professor@123", firstHash));
        assertFalse(passwordHasher.matches("wrong-password", firstHash));
        assertTrue(passwordHasher.precisaAtualizar("senha-ficticia"));
        assertFalse(passwordHasher.precisaAtualizar(firstHash));
    }

    @Test
    void acceptsLegacyDevelopmentPasswordOnlyForUpgrade() {
        assertTrue(passwordHasher.matches("senha-ficticia", "senha-ficticia"));
        assertFalse(passwordHasher.matches("other-password", "senha-ficticia"));
    }
}
