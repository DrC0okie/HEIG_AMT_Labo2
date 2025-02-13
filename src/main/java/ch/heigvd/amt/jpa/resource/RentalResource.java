package ch.heigvd.amt.jpa.resource;

import ch.heigvd.amt.jpa.entity.Customer;
import ch.heigvd.amt.jpa.entity.Inventory;
import ch.heigvd.amt.jpa.entity.Inventory_;
import ch.heigvd.amt.jpa.entity.Store;
import ch.heigvd.amt.jpa.service.RentalService;
import io.quarkus.qute.CheckedTemplate;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.common.annotation.Blocking;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;
import org.jboss.resteasy.reactive.RestForm;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// The existing annotations on this class must not be changed (i.e. new ones are allowed)
@Path("rental")
public class RentalResource {

    @Inject
    RentalService rs;

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance rental(String username);

        public static native TemplateInstance rental$success(RentalService.RentalDTO rental);

        public static native TemplateInstance rental$failure(String message);

        public static native TemplateInstance searchFilmsResults(
                List<RentalService.FilmInventoryDTO> films);

        public static native TemplateInstance searchFilmsSelect(
                RentalService.FilmInventoryDTO film);

        public static native TemplateInstance searchCustomersResults(
                List<RentalService.CustomerDTO> customers);

        public static native TemplateInstance searchCustomersSelect(
                RentalService.CustomerDTO customer);
    }

    @GET
    @RolesAllowed({ "staff" })
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance rental(@Context SecurityContext securityContext) {
        return Templates.rental(securityContext.getUserPrincipal().getName());
    }

    @POST
    @Produces(MediaType.TEXT_HTML)
    @Blocking
    @RolesAllowed({ "staff" }) // Restricts access to users with the "staff" role
    public TemplateInstance registerRental(@Context SecurityContext securityContext,
            @FormParam("inventory") Integer inventory, @FormParam("customer") Integer customer) {
        if (inventory == null || customer == null) {
            return Templates.rental$failure("The submission is not valid, missing inventory or customer");
        }

        // Creating fake objects just to pass the id to rs.rentFilm()
        var inventoryObj = new Inventory();
        inventoryObj.setId(inventory);
        var customerObj = new Customer();
        customerObj.setId(customer);
        var staff = rs.searchStaff(securityContext.getUserPrincipal().getName());
        if (staff == null) // In the rare case of staff deletion from another account
            return Templates.rental$failure("Logged in staff wasn't found in database...");

        // Any exception will return Optional.empty()
        Optional<RentalService.RentalDTO> rentalResult = rs.rentFilm(inventoryObj, customerObj, staff);

        if (rentalResult.isPresent()) {
            return Templates.rental$success(rentalResult.get());

        } else {
            return Templates.rental$failure("The selected item is not available.");
        }
    }

    @GET
    @Path("/film/{inventory}")
    @RolesAllowed({ "staff" })
    @Produces(MediaType.TEXT_HTML)
    @Blocking
    public TemplateInstance selectFilmsGet(Integer inventory) {
        return Templates.searchFilmsSelect(rs.searchFilmInventory(inventory));
    }

    @POST
    @Path("/film/search")
    @RolesAllowed({ "staff" })
    @Produces(MediaType.TEXT_HTML)
    @Blocking
    public TemplateInstance searchFilmsPost(@Context SecurityContext securityContext,
            @FormParam("search") String query) {
        List<Store> stores;
        List<RentalService.FilmInventoryDTO> films = new ArrayList<>();
        stores = rs.getStoreFromManager(securityContext.getUserPrincipal().getName());
        for (Store store : stores) {
            films.addAll(rs.searchFilmInventory(query, store));
        }
        return Templates.searchFilmsResults(films);
    }

    @POST
    @Path("/customer/search")
    @RolesAllowed({ "staff" })
    @Produces(MediaType.TEXT_HTML)
    @Blocking
    public TemplateInstance searchCustomersPost(@Context SecurityContext securityContext,
            @FormParam("search") String query) {
        List<Store> stores;
        List<RentalService.CustomerDTO> customers = new ArrayList<>();
        stores = rs.getStoreFromManager(securityContext.getUserPrincipal().getName());
        for (Store store : stores) {
            customers.addAll(rs.searchCustomer(query, store));
        }
        return Templates.searchCustomersResults(customers);
    }

    @GET
    @Path("/customer/{customer}")
    @RolesAllowed({ "staff" })
    @Produces(MediaType.TEXT_HTML)
    @Blocking
    public TemplateInstance selectCustomerGet(Integer customer) {
        return Templates.searchCustomersSelect(rs.searchCustomer(customer));
    }
}
