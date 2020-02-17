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

@Stateless
@Path("ping")
public class Ping {

    public static final Logger log = LoggerFactory.getLogger(Ping.class);

    @GET
    @SlowCallLog(env = "ABC", unit = "us")
    public Response ping(@QueryParam("s") int s) {
        log.info("Ping?");
        try {
            if (s > 0)
                Thread.sleep(s);
        } catch (InterruptedException ex) {
            throw new InternalServerErrorException(ex);
        }
        return Response.ok("pong?\n").build();
    }
}
