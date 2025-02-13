package ch.heigvd.amt.jpa.entity;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * This test class contains two tests that demonstrate how to test the Entity mapping for one entity
 * in read and write modes.
 */
@QuarkusTest
class ActorTest {

    @Inject
    EntityManager em;

    /**
     * A test example for reading from the actor table.
     */
    @Test
    void testReadActors() {
        var actors = em.createQuery("SELECT a FROM actor a", Actor.class).getResultList();
        assertEquals(200, actors.size(), "Number of actors in Sakila database must be 200");
    }

    /**
     * A test example for writing to the actor table.
     */
    @Test
    // The @Transactional is required of any write operation
    @TestTransaction
    void testWriteActor() {
        Actor actor = new Actor();
        actor.setFirstName("John");
        actor.setLastName("Doe");
        em.persist(actor);
        assertNotNull(actor.getId(), "Actor ID should not be null");
    }

}
