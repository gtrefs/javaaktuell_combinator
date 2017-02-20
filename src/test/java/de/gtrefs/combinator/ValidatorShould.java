package de.gtrefs.combinator;

import org.junit.Test;

import java.util.function.Function;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static de.gtrefs.combinator.ValidatorShould.UserValidation.*;

public class ValidatorShould {

    @Test
    public void yield_valid_for_user_with_email_and_non_empty_name(){
        final User gregor = new User("Gregor Trefs", 31, "mail@mailinator.com");

        UserValidation validation = nameIsNotEmpty.and(eMailContainsAtSign);

        assertThat(validation.apply(gregor), is(true));
    }

    public interface UserValidation extends Function<User, Boolean> {
        UserValidation nameIsNotEmpty = user -> !user.name.trim().isEmpty();
        UserValidation eMailContainsAtSign = user -> user.email.contains("@");

        default UserValidation and(UserValidation other){
            return user -> this.apply(user) && other.apply(user);
        }
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