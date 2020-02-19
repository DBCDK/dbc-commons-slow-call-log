package dk.dbc.example;

import dk.dbc.commons.slowcalllog.SlowCallLog;
import javax.ejb.Stateless;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
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
            unit = "us"
    )
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

    public static class Status {

        public boolean ok;
        public String message;

        public Status() {
        }

        private Status(boolean ok, String message) {
            this.ok = ok;
            this.message = message;
        }

        static Status ok(String message) {
            return new Status(true, message);
        }

        static WebApplicationException fail(String message, Response.Status status) {
            Response resp = Response.status(status).entity(new Status(false, message)).build();
            return new WebApplicationException(message, resp);
        }

        @Override
        public String toString() {
            return "Status{" + "ok=" + ok + ", message=" + message + '}';
        }
    }
}
