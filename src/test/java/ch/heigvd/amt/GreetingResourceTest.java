package ch.heigvd.amt;

import io.quarkus.test.junit.QuarkusTest;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class GreetingResourceTest {

    @Test
    public void testHelloEndpoint() {
        given()
          .when().get("/hello?name={name}", "student")
          .then()
             .statusCode(200)
             .body(is("Hello student!"));
    }

    @ParameterizedTest
    @CsvSource({"Mike,12345", "Jon,12345"})
    void testHelloStaffValid(String staff, String password) {
        given()
            .auth().form(staff, password)
            .when()
            .get("/hello/me")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body(is("Hello "+staff+"!"));
    }

    @Test
    void testHelloStaffAnonymous() {
        given()
            .redirects().follow(false)
            .when()
            .get("/hello/me")
            .then()
            .statusCode(HttpStatus.SC_MOVED_TEMPORARILY)
            .header("Location", Matchers.endsWith("login.html"));
    }

}
