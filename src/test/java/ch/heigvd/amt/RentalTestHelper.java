package ch.heigvd.amt;

import ch.heigvd.amt.jpa.entity.Rental;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class RentalTestHelper {

  @Inject
  EntityManager em;


  public List<Rental> getRentalsForInventory(Integer inventoryId) {
    return em.createQuery("""
          SELECT r from rental r
          WHERE r.inventory.id = :inventoryId
          AND r.returnDate IS NULL
        """, Rental.class)
        .setParameter("inventoryId", inventoryId)
        .getResultList();
  }

  @Transactional
  public void deleteAnyActiveRentalForInventory(Integer inventoryId) {
    em.createQuery("""
        DELETE FROM rental r
        WHERE r.inventory.id = :inventoryId
        AND r.returnDate IS NULL
        """)
        .setParameter("inventoryId", inventoryId)
        .executeUpdate();
  }
}
