package dk.dbc.example;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

public class Status {

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
