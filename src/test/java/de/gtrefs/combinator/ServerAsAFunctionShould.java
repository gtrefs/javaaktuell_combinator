package de.gtrefs.combinator;

import com.fredhonorio.json_decoder.Decoder;
import com.fredhonorio.json_decoder.Decoders;
import de.gtrefs.combinator.ValidatorShould.User;
import de.gtrefs.combinator.ValidatorShould.UserValidation;
import io.vavr.Tuple;
import org.junit.Before;
import org.junit.Test;

import java.util.function.Function;

import static com.fredhonorio.json_decoder.Decoders.field;
import static de.gtrefs.combinator.ValidatorShould.UserValidation.eMailContainsAtSign;
import static de.gtrefs.combinator.ValidatorShould.UserValidation.nameIsNotEmpty;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class ServerAsAFunctionShould {

    private Function<Function<User, HttpResponse>, Server> serverWith;

    @Before
    public void setUp(){
        var validator = nameIsNotEmpty.and(eMailContainsAtSign);
        var decoder = Decoder.map3(
                field("name", Decoders.String),
                field("age", Decoders.Integer),
                field("email", Decoders.String),
                User::new);
        this.serverWith = Server.builder().apply(decoder).apply(validator);
    }

    @Test
    public void decode_request_body_to_user_and_validate_valid_user(){
        var server = serverWith.apply(user -> HttpResponse.ok("Received user."));

        var result = server.post(validUserEncodedAsJson());

        assertThat(result.status, is(HttpStatus.OK));
        assertThat(result.responseBody, is("Received user."));
    }

    private String validUserEncodedAsJson(){
        return "{\"name\": \"Gregor\", " +
                "\"age\": 32, " +
                "\"email\":\"Gregor.Trefs@gmail.com\"}";
    }

    @Test
    public void decode_request_body_to_user_and_validate_invalid_user(){
        var server = serverWith.apply(user -> HttpResponse.ok("Received user: " + user));

        var result = server.post(userWithoutAtSignInEMail());

        assertThat(result.status, is(HttpStatus.BAD_REQUEST));
        assertThat(result.responseBody, is("E-Mail is not valid."));
    }


    private String userWithoutAtSignInEMail(){
        return "{\"name\": \"Gregor\", " +
                "\"age\": 32, " +
                "\"email\":\"Gregorgmail.com\"}";
    }

    @Test
    public void return_bad_request_for_invalid_json(){
        var server = serverWith.apply(user -> HttpResponse.ok("Received user: " + user));

        var result = server.post(invalidJson());

        assertThat(result.status, is(HttpStatus.BAD_REQUEST));
        assertThat(result.responseBody, containsString("Unexpected character"));
    }

    private String invalidJson() {
        return "{{{{{{}{}{}[[]]";
    }

    @Test
    public void return_bad_request_for_user_without_email(){
        var server = serverWith.apply(user -> HttpResponse.ok("Received user: " + user));

        var result = server.post(userWithoutMail());

        assertThat(result.status, is(HttpStatus.BAD_REQUEST));
        assertThat(result.responseBody, is("field 'email': missing"));
    }

    private String userWithoutMail() {
        return "{\"name\": \"Gregor\", " +
                "\"age\": 32}";
    }

    static class Server {
        private final Decoder<User> decoder;
        private final UserValidation validator;
        private final Function<User, HttpResponse> handler;

        Server(Decoder<User> decoder, UserValidation validator, Function<User, HttpResponse> handler){
            this.decoder = decoder;
            this.validator = validator;
            this.handler = handler;
        }

        static Function<Decoder<User>, Function<UserValidation, Function<Function<User, HttpResponse>, Server>>> builder() {
            return decoder -> validator -> handler -> new Server(decoder, validator, handler);
        }

        HttpResponse post(String requestBody){
            return Decoders.decodeString(requestBody, decoder)
                    .map(user -> Tuple.of(user, validator.apply(user)))
                    .map(result -> result._2.isValid()? handler.apply(result._1) : HttpResponse.badRequest(result._2.getReason().get()))
                    .getOrElseGet(HttpResponse::badRequest);
        }
    }

    static class HttpResponse {
        private final HttpStatus status;
        private final String responseBody;

        HttpResponse(HttpStatus status, String responseBody) {
            this.status = status;
            this.responseBody = responseBody;
        }

        static HttpResponse badRequest(String description){
            return new HttpResponse(HttpStatus.BAD_REQUEST, description);
        }


        static HttpResponse ok(String response){
            return new HttpResponse(HttpStatus.OK, response);
        }
    }

    enum HttpStatus {
        OK(200), BAD_REQUEST(400);

        private final int status;

        HttpStatus(int status) {
            this.status = status;
        }
    }
}
