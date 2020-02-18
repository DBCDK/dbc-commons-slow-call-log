# Slow-Call-Log

## An annotation based logging of slow method calls in JavaEE/JakartaEE using Interceptors

This is inspired by PostgreSQL's `slow-query` settings that log input for queries over a given threshold.

Ideally you would have debug log level for the calls that are slow, but not those that are fast. This cannot be achieved since you only know after the method call if you wanted one or the other. Theoretically you can make a logger that accumulates all the lines, that are of debug level, and posts them when the call is determined to be slow, however this approach will take up a log of resources both memory and cpu cycles, that potentially will render all the calls slow.

Instead we opt to log when a call has been completed, and determined slow.

__WARNING__: We log the arguments as they are when the call is completed. If arguments are altered by the method, they will not be seen as the input edition of them. This might hide the culprit for the call duration.

It is all based around an annotation `@SlowCallLog`, and `CDI` in `EE`. During bean discovery in the container, all `@SlowCallLog` annotations are picked up. An interceptor is configured with a wrapper for the given methods, that calls the method, and if the calls duration is above a threshold, the call in logged with it's parameters and optionally `return value` or `exception` if the call fails.

## Example

A fully running payara-micro demonstration can be found in the [example](example) directory.

It is based around this structure:

```
@Stateless // or any other bean annotation
public MyBackendClass {

    @SlowCallLog(env="BACKEND_TIMING", factor=.6, result=false, level="ERROR")
    List<Row> getDatabaseEntriesFor(String key, Timestamp whenish) {
        ...
    }

    @SlowCallLog(env="BACKEND_TIMING", factor=.4, params={1})
    Resp processEntries(List<Row> rows, String trackingId) {
        ...
    }

}
```

Called from:

```
    @Inject
    MyBackendClass backend;
...

    void doBackendStuff(){
...
        List<Row> rows = backend.getDatabaseEntriesFor(key, whenish);
...
        backend.processEntries(rows, trackingId);
...
    }

```

The calls are across bean-boundries, so interceptors are in play. The environment variable `BACKEND_TIMING` is set to some duration (ex. `20ms`), it is expected that `getDatabaseEntriesFor()` takes under normal circumstances 60% of the wall clock time, and `processEntries()` about 40%.
This will log all the arguments and exceptions but not return values for the calls to `getDatabaseEntriesFor()`, that take more than 12ms, as an error.
And for `processEntries()` calls that take more that 8ms it will log trackingId and return value.

This is bould upon the `slf4j` log framework. And the logger used is called `SlowCallLog`. An mdc value called: `call_duration_ms` records duration in milliseconds.