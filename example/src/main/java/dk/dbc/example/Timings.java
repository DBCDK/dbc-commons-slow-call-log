package dk.dbc.example;

import dk.dbc.commons.stopwatch.Clock;
import dk.dbc.commons.stopwatch.StopWatch;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.UUID;
import javax.ejb.Stateless;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

@Stateless
@Path("timings")
public class Timings {

    @Context
    StopWatch sw;

    @GET
    public Status timings(@Context StopWatch stopWatch) {
        // Scope based timing
        try (Clock time = stopWatch.time("body")) {

            // Supplier based timing
            UUID uuid = stopWatch.timed("make_tracking_id", UUID::randomUUID);
            sw.setMDC("trackingId", uuid.toString());

            // Supplier with exception(s) based timing
            URL url = stopWatch.timedWithExceptions("parse_url",
                                                    () -> new URL("http://localhost/foo/bar.html"))
                    .checkFor(MalformedURLException.class)
                    .value();
            System.out.println("url = " + url);

            return Status.ok(Instant.now().toString());
        } catch (MalformedURLException ex) {
            throw Status.fail(ex.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
        }
    }
}
