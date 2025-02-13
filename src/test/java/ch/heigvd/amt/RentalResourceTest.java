package ch.heigvd.amt;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

@QuarkusTest
class RentalResourceTest {

  @Inject
  RentalTestHelper rentalTestHelper;

  @Test
  void testRentStaff() {
    rentalTestHelper.deleteAnyActiveRentalForInventory(854);
    given()
        .auth().form("Mike", "12345")
        .when()
        .formParam("inventory", 854)
        .formParam("customer", 1)
        .post("/rental")
        .then()
        .statusCode(HttpStatus.SC_OK)
        .body(containsString("The rental of inventory 854 by customer 1 was successfully registered"));
  }

  @Test
  void testRentStaffWrongPassword() {
    rentalTestHelper.deleteAnyActiveRentalForInventory(854);
    given()
        .auth().form("Mike", "54321")
        .when()
        .formParam("inventory", 854)
        .formParam("customer", 1)
        .post("/rental")
        .then()
        .statusCode(HttpStatus.SC_MOVED_TEMPORARILY)
        .header("Location", Matchers.endsWith("login.html"));
  }

  @Test
  void testRentAnonymous() {
    rentalTestHelper.deleteAnyActiveRentalForInventory(854);
    given()
        .when()
        .formParam("inventory", 854)
        .formParam("customer", 1)
        .post("/rental")
        .then()
        .statusCode(HttpStatus.SC_MOVED_TEMPORARILY)
        .header("Location", Matchers.endsWith("login.html"));
  }
}
