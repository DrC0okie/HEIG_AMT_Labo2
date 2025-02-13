package ch.heigvd.amt.jpa.service;

import ch.heigvd.amt.jpa.entity.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.persistence.criteria.Predicate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Signature of existing methods must not be changed.
 */
@ApplicationScoped
public class RentalService {

    @Inject
    EntityManager em;

    // The following records must not be changed
    public record RentalDTO(Integer inventory, Integer customer) {
    }

    public record FilmInventoryDTO(String title, String description, Integer inventoryId) {
    }

    public record CustomerDTO(Integer id, String firstName, String lastName) {
    }

    /**
     * Search a staff with username, considering it is a unique key
     *
     * @param username searched username
     */
    public Staff searchStaff(String username) {
        return em.createQuery(
                "SELECT s FROM Staff s WHERE s.username = :username",
                Staff.class).setParameter("username", username).getSingleResult();
    }

    /**
     * Rent a film out of store's inventory for a given customer.
     *
     * @param inventory the inventory to rent
     * @param customer  the customer to which the inventory is rented
     * @param staff     the staff that process the customer's request in the store
     * @return an Optional that is present if rental is successful, if Optional is
     *         empty rental failed
     */
    @Transactional
    public Optional<RentalDTO> rentFilm(Inventory inventory, Customer customer, Staff staff) {
        try {
            // Set SERIALIZABLE transaction isolation for strict concurrency control
            em.createNativeQuery("SET TRANSACTION ISOLATION LEVEL SERIALIZABLE").executeUpdate();

            // Step 2: Check if there is any active rental for this inventory item
            Long activeRentals = em.createQuery(
                    "SELECT COUNT(r) FROM rental r WHERE r.inventory.id = :inventoryId AND r.returnDate IS NULL",
                    Long.class)
                    .setParameter("inventoryId", inventory.getId())
                    .getSingleResult();

            // Step 3: If there is an active rental, exit and return Optional.empty()
            if (activeRentals > 0) {
                return Optional.empty();
            }

            // Step 4: Otherwise, create and persist a new rental record
            Rental rental = new Rental();
            rental.setInventory(inventory);
            rental.setCustomer(customer);
            rental.setStaff(staff);
            rental.setRentalDate(Timestamp.from(Instant.now()));
            em.persist(rental);

            // Step 5: Return the DTO indicating successful rental
            return Optional.of(new RentalDTO(inventory.getId(), customer.getId()));

        } catch (Exception e) {
            // Handle exceptions (e.g., log them if needed) and return Optional.empty() on
            // failure
            return Optional.empty();
        }
    }

    /**
     * @param query the searched string
     * @param store the store to search in
     * @return films matching the query
     */
    public List<FilmInventoryDTO> searchFilmInventory(String query, Store store) {
        // Initialize the criteria builder and query, define the query root as Inventory
        // and join it with the film entity.
        var cb = em.getCriteriaBuilder();
        var q = cb.createQuery(FilmInventoryDTO.class);
        var inventory = q.from(Inventory.class);
        var film = inventory.join(Inventory_.film);

        // Select the film title, description, and inventory id
        q.multiselect(film.get(Film_.title), film.get(Film_.description), inventory.get(Inventory_.id));

        // Create a list of predicates to filter the query
        List<Predicate> predicates = new ArrayList<>();

        // Filter the query by store
        predicates.add(cb.equal(inventory.get(Inventory_.store), store));

        // Split the query in parts, check type of each word and then filter by film
        // inventory id, title or description
        String[] queryParts = query.trim().split("\\s+");
        for (String queryPart : queryParts) {
            Integer inventoryId = null;
            try {
                inventoryId = Integer.parseInt(queryPart);
            } catch (NumberFormatException e) {
                /* Do nothing */}

            if (inventoryId != null)
                predicates.add(cb.equal(inventory.get(Inventory_.id), inventoryId));
            else {
                String queryFormatted = queryPart.toLowerCase();
                predicates.add(cb.or(
                        cb.like(cb.lower(film.get(Film_.title)), "%" + queryFormatted + "%"),
                        cb.like(cb.lower(film.get(Film_.description)), "%" + queryFormatted + "%")));
            }
        }

        // Apply the predicates to the query
        q.where(predicates.toArray(new Predicate[0]));

        // Execute the query and map the results to DTOs
        return em.createQuery(q).getResultList();
    }

    public FilmInventoryDTO searchFilmInventory(Integer inventoryId) {
        // Initialize the criteria builder and query, define the query root as Inventory
        // and join it with the film entity.
        var cb = em.getCriteriaBuilder();
        var q = cb.createQuery(FilmInventoryDTO.class);
        var inventory = q.from(Inventory.class);

        // Select the film title, description, and inventory id
        q.multiselect(inventory.get(Inventory_.film).get(Film_.title),
                inventory.get(Inventory_.film).get(Film_.description), inventory.get(Inventory_.id));

        // Filter the query by inventory id
        q.where(cb.equal(inventory.get(Inventory_.id), inventoryId));

        // Execute the query and map the results to DTOs
        return em.createQuery(q).getSingleResult();
    }

    public CustomerDTO searchCustomer(Integer customerId) {
        // Initialize the criteria builder and query, define the query root as Customer.
        var cb = em.getCriteriaBuilder();
        var q = cb.createQuery(CustomerDTO.class);
        var customer = q.from(Customer.class);

        // Select the customer id, first name, and last name
        q.multiselect(customer.get(Customer_.id), customer.get(Customer_.firstName), customer.get(Customer_.lastName));

        // Filter the query by customer id
        q.where(cb.equal(customer.get(Customer_.id), customerId));

        // Execute the query and map the results to DTOs
        return em.createQuery(q).getSingleResult();
    }

    public List<CustomerDTO> searchCustomer(String query, Store store) {
        // Initialize the criteria builder and query, define the query root as Customer.
        var cb = em.getCriteriaBuilder();
        var q = cb.createQuery(CustomerDTO.class);
        var customer = q.from(Customer.class);

        // Select the customer id, first name, and last name
        q.multiselect(customer.get(Customer_.id), customer.get(Customer_.firstName), customer.get(Customer_.lastName));

        // Create a list of predicates to filter the query
        List<Predicate> predicates = new ArrayList<>();

        // Filter the query by store
        predicates.add(cb.equal(customer.get(Customer_.store), store));

        // Split the query in parts, check type of each word and then filter by customer
        // id, first name or last name
        String[] queryParts = query.trim().split("\\s+");
        for (String queryPart : queryParts) {
            Integer customerId = null;
            try {
                customerId = Integer.parseInt(queryPart);
            } catch (NumberFormatException e) {
                /* Do nothing */}

            if (customerId != null)
                predicates.add(cb.equal(customer.get(Customer_.id), customerId));
            else {
                String queryFormatted = queryPart.toLowerCase();
                predicates.add(cb.or(
                        cb.like(cb.lower(customer.get(Customer_.firstName)), queryFormatted),
                        cb.like(cb.lower(customer.get(Customer_.lastName)), queryFormatted)));
            }
        }

        // Apply the predicates to the query
        q.where(predicates.toArray(new Predicate[0]));

        // Execute the query and map the results to DTOs
        return em.createQuery(q).getResultList();
    }

    public List<Store> getStoreFromManager(String username) {
        return em.createQuery(
                "SELECT s FROM store s " +
                        "JOIN FETCH s.managerStaff m " +
                        "WHERE m.username = :username",
                Store.class)
                .setParameter("username", username)
                .getResultList();
    }
}
