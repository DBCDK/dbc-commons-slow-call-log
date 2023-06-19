package dk.dbc.example;

import dk.dbc.commons.slowcalllog.SlowCallLog;
import jakarta.ejb.Stateless;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;
import org.slf4j.event.Level;

@Stateless
@Path("ping")
public class Ping {

    public static final Logger log = LoggerFactory.getLogger(Ping.class);

    @GET
    @SlowCallLog(
            env = "CALL_THRESHOLD",
            parameters = {1}, // Not UriInfo
            result = true,
            level = Level.ERROR,
            unit = "us")
    public Status ping(@Context UriInfo UriInfo,
                       @QueryParam("s") int s) {
        log.info("Ping? s={}", s);
        try {
            if (s > 0)
                Thread.sleep(s);
        } catch (InterruptedException ex) {
            throw new InternalServerErrorException(ex);
        }

        boolean serverFail = UriInfo.getQueryParameters().containsKey("server");
        if (serverFail)
            throw Status.fail("no-pong?", Response.Status.INTERNAL_SERVER_ERROR);

        boolean clientFail = UriInfo.getQueryParameters().containsKey("client");
        if (clientFail)
            throw Status.fail("no-pong?", Response.Status.BAD_REQUEST);

        return Status.ok("pong!");
    }
}
