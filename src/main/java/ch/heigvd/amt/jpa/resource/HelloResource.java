package ch.heigvd.amt.jpa.resource;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

import java.security.Principal;

// The existing annotations on this class must not be changed
@Path("hello")
public class HelloResource {

    @Inject
    Template hello;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public TemplateInstance get(@QueryParam("name") String name) {
        return hello.data("name", name);
    }

    @GET
    @Path("me")
    @Produces(MediaType.TEXT_PLAIN)
    @RolesAllowed({ "user", "staff" }) // Restricts access to users with the "user" or "staff" role
    public TemplateInstance getMe(@Context SecurityContext sec) {
        // Retrieve the username of the currently authenticated user
        Principal user = sec.getUserPrincipal();
        String name = user != null ? user.getName() : "anonymous";
        // Pass the username to the hello template
        return hello.data("name", name);
    }
}
