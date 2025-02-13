package ch.heigvd.amt;

import ch.heigvd.amt.jpa.entity.Customer;
import ch.heigvd.amt.jpa.entity.Inventory;
import ch.heigvd.amt.jpa.entity.Staff;
import ch.heigvd.amt.jpa.entity.Store;
import ch.heigvd.amt.jpa.service.RentalService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@QuarkusTest
class RentalServiceTest {

  @Inject
  RentalService rentalService;

  @Inject
  RentalTestHelper rentalTestHelper;

  @Inject
  EntityManager em;

  @Test
  void testRental() throws InterruptedException {
    var inventoryId = 854;
    var concurrency = 10;
    var passes = 50;

    var customer = em.createQuery("SELECT c from customer c", Customer.class).setMaxResults(1).getSingleResult();
    var staff = em.createQuery("SELECT s from Staff s", Staff.class).setMaxResults(1).getSingleResult();
    var inventory = em.find(Inventory.class, inventoryId);

    // Given that the test try to trigger interlacing of SQL statements with concurrent calls,
    // a certain number of passes must be tested to gain confidence of the result.
    // From empirical testing the test should fail on improper concurrency transaction at the first pass.
    for (int p=1; p<=passes; p++) {
      // Ensure nominal case by pruning any existing active rental for inventory item
      rentalTestHelper.deleteAnyActiveRentalForInventory(inventoryId);
      Assertions.assertEquals(0, rentalTestHelper.getRentalsForInventory(inventoryId).size());

      // Setup concurrent calls to renting service and execute at "once"
      List<Callable<Boolean>> tasks = new ArrayList<>();
      ExecutorService executor = Executors.newFixedThreadPool(concurrency);
      for (int i=0; i<concurrency; i++) {
        tasks.add(() -> {
          Optional<RentalService.RentalDTO> r = Optional.empty();
          try {
            r = rentalService.rentFilm(inventory, customer, staff);
          } catch (Exception e) {
            // Ignore exceptions as they are expected in case of concurrency issues
          }
          return r.isPresent();
        });
      }
      var futures = executor.invokeAll(tasks);
      executor.shutdown();
      var termination = executor.awaitTermination(30, TimeUnit.SECONDS);
      Assertions.assertTrue(termination);

      var successfulRentals = futures.stream().filter( f -> {
        try {
          return f.get();
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }).toList();

      // Validate that only one rental for the same item was inserted in database
      Assertions.assertEquals(successfulRentals.size(), rentalTestHelper.getRentalsForInventory(inventoryId).size(), "Database state must match service successes (Pass:"+p+")");
      Assertions.assertEquals(1, rentalTestHelper.getRentalsForInventory(inventoryId).size(), "DB, Only one rental active per inventory item (Pass:"+p+")");
      Assertions.assertEquals(1, successfulRentals.size(), "Service, Only one rental successful per inventory item (Pass:"+p+")");
    }
  }

  @Test
  void testSearchInventoryBerlin() {
    var store = new Store();
    store.setId(1);
    var films = rentalService.searchFilmInventory("berlin", store);
    Assertions.assertEquals(123, films.size(), "Number of films in inventory with 'berlin' must be 123");
    Assertions.assertEquals(3, films.stream().filter(f -> f.title().equals("SMILE EARRING")).count(),
        "Number of films in inventory with title 'SMILE EARRING' must be 3");
  }

  @Test
  void testSearchInventoryBerlinDrama() {
    var store = new Store();
    store.setId(1);
    var films = rentalService.searchFilmInventory("berlin drama", store);
    Assertions.assertEquals(13, films.size(), "Number of films in inventory with 'berlin drama' must be 13");
  }

  @ParameterizedTest
  @ValueSource(strings = {"3707", "smile 3707", "smile 3707 drama"})
  void testSearchInventorySmileEarring(String query) {
    var store = new Store();
    store.setId(1);
    var films = rentalService.searchFilmInventory(query, store);
    Assertions.assertEquals(1, films.size());
    Assertions.assertEquals(3707 , films.get(0).inventoryId());
    Assertions.assertEquals("SMILE EARRING", films.get(0).title());
  }

  @ParameterizedTest
  @ValueSource(strings = {"hunt", "eleanor", "148", "hunt eleanor", "eleanor hunt", "eleanor 148 hunt"})
  void testSearchCustomer(String query) {
    var store = new Store();
    store.setId(1);
    var customers = rentalService.searchCustomer(query, store);
    Assertions.assertEquals(1, customers.size(), "Number of customers with lastname 'hunt' must be 1");
    Assertions.assertEquals("ELEANOR", customers.get(0).firstName());
    Assertions.assertEquals("HUNT", customers.get(0).lastName());
  }

  @ParameterizedTest
  @ValueSource(ints = {148, 3707, 854})
  void testSearchFilmInventory(Integer inventoryId) {
    var film = rentalService.searchFilmInventory(inventoryId);
    Assertions.assertEquals(inventoryId, film.inventoryId());
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3})
  void testSearchCustomer(Integer customerId) {
    var customer = rentalService.searchCustomer(customerId);
    Assertions.assertEquals(customerId, customer.id());
  }
}
