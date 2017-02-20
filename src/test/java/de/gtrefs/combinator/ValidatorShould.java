package de.gtrefs.combinator;

import org.junit.Test;

import java.util.function.Function;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ValidatorShould {

    @Test
    public void yield_valid_for_user_with_email_and_non_empty_name(){
        final User gregor = new User("Gregor Trefs", 31, "mail@mailinator.com");

        final UserValidation validation = todo();

        assertThat(validation.apply(gregor), is(true));
    }

    interface UserValidation extends Function<User, Boolean> {
    }

    class User {
        final String name;
        final int age;
        final String email;

        User(String name, int age, String email) {
            this.name = name;
            this.age = age;
            this.email = email;
        }
    }

    private <T> T todo(){
        throw new UnsupportedOperationException();
    }
}