package org.jokes.service;

import javax.ws.rs.*;

public interface IcndbJokesCli {

    // http://api.icndb.com/jokes/random?firstName=John&lastName=Doe

    @GET
    @Path("jokes/{select}")
    String getJoke(@PathParam("select") String select, @QueryParam("firstName") String firstName, @QueryParam("lastName") String lastName);

}
