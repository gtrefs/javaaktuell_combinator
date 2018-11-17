package de.gtrefs.combinator;

import org.junit.Test;

import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import static de.gtrefs.combinator.ValidatorShould.UserValidation.eMailContainsAtSign;
import static de.gtrefs.combinator.ValidatorShould.UserValidation.nameIsNotEmpty;
import static de.gtrefs.combinator.ValidatorShould.ValidationResult.invalid;
import static de.gtrefs.combinator.ValidatorShould.ValidationResult.valid;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ValidatorShould {

    @Test
    public void yield_valid_for_user_with_email_and_non_empty_name(){
        final User gregor = new User("Gregor Trefs", 32, "mail@mailinator.com");

        UserValidation validation = nameIsNotEmpty.and(eMailContainsAtSign);

        assertThat(validation.apply(gregor).isValid(), is(true));
    }

    @Test
    public void yield_invalid_for_user_without_email(){
        final User gregor = new User("Gregor Trefs", 32, "");

        final UserValidation validation = nameIsNotEmpty.and(eMailContainsAtSign);
        final ValidationResult validationResult = validation.apply(gregor);

        assertThat(validationResult.isValid(), is(false));
        assertThat(validationResult.getReason().get(), is("E-Mail is not valid."));
    }

    @Test
    public void sort_users_by_age_and_name(){
        final User gregor = new User("Gregor Trefs", 32, "gregor@mailinator.com");
        final User petra = new User("Petra Kopfler", 30, "petra@mailinator.com");
        final User robert = new User("Robert Schmidt", 32, "robert@mailinator.com");

        Comparator<User> byAge = Comparator.comparing(user -> user.age);
        Comparator<User> byName = Comparator.comparing(user -> user.name);

        Comparator<User> byAgeThenName = byAge.thenComparing(byName);

        assertThat(byAgeThenName.compare(gregor, petra), is(1));
        assertThat(byAgeThenName.compare(gregor, robert), is(-11));
    }

    @Test
    public void use_combinators_from_different_namespaces(){
        var gregor = new User("Gregor Trefs", 32, "gregor@mailinator.com");
        var validations = (NameValidations & AgeValidations) UserValidation::create;

        var validator = validations.olderThan(20).and(validations.nameIsUpperCase());
        var validationResult = validator.apply(gregor);

        assertThat(validationResult.getReason().get(), containsString("upper case."));
    }

    public interface NameValidations extends UserValidation {
        default UserValidation nameIsUpperCase(){
            return (var user) -> {
                String message = String.format("User %s must be named in all upper case.", user.toString());
                return user.name.chars().allMatch(c -> c >= 65 && c <= 90)? valid(): invalid(message);
            };
        }
    }

    public interface AgeValidations extends UserValidation {
        default UserValidation olderThan(int age){
            return (var user) -> {
                String message = String.format("User %s must be older than %d.", user.toString(), age);
                return user.age > age? valid() : invalid(message);
            };
        }
    }
    public interface UserValidation extends Function<User, ValidationResult> {
        static ValidationResult create(User user){
            throw new UnsupportedOperationException();
        }

        UserValidation nameIsNotEmpty = user -> user.name.trim().isEmpty()?invalid("User name is empty") : valid();
        UserValidation eMailContainsAtSign = user -> user.email.contains("@")?valid() : invalid("E-Mail is not valid.");

        default UserValidation and(UserValidation other){
            return user -> {
                final ValidationResult result = this.apply(user);
                return result.isValid() ? other.apply(user) : result;
            };
        }
    }

    static class User {
        final String name;
        final int age;
        final String email;

        User(String name, int age, String email) {
            this.name = name;
            this.age = age;
            this.email = email;
        }

        @Override
        public String toString() {
            return "User{" +
                    "name='" + name + '\'' +
                    ", age=" + age +
                    ", email='" + email + '\'' +
                    '}';
        }
    }

    interface ValidationResult{
        static ValidationResult valid(){
            return ValidationSupport.valid();
        }

        static ValidationResult invalid(String reason){
            return new Invalid(reason);
        }

        boolean isValid();

        Optional<String> getReason();
    }

    private final static class Invalid implements ValidationResult {

        private final String reason;

        Invalid(String reason){
            this.reason = reason;
        }

        public boolean isValid(){
            return false;
        }

        public Optional<String> getReason(){
            return Optional.of(reason);
        }

        @Override
        public String toString() {
            return "Invalid{" +
                   "reason='" + reason + '\'' +
                   '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) { return true; }
            if (!(o instanceof Invalid)) { return false; }
            Invalid invalid = (Invalid) o;
            return Objects.equals(reason, invalid.reason);
        }

        @Override
        public int hashCode() {
            return Objects.hash(reason);
        }
    }

    private static final class ValidationSupport {
        private static final ValidationResult valid = new ValidationResult(){
            public boolean isValid(){ return true; }
            public Optional<String> getReason(){ return Optional.empty(); }
        };

        static ValidationResult valid(){
            return valid;
        }
    }

    private static  <T> T todo(){
        throw new UnsupportedOperationException();
    }
}