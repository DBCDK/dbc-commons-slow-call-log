# Slow-Call-Log and StopWatch

## Slow-Call-Log

### An annotation based logging of slow method calls in JavaEE/JakartaEE using Interceptors

This is inspired by PostgreSQL's `slow-query` settings that log input for queries over a given threshold.

Ideally you would have debug log level for the calls that are slow, but not those that are fast. This cannot be achieved since you only know after the method call if you wanted one or the other. Theoretically you can make a logger that accumulates all the lines, that are of debug level, and posts them when the call is determined to be slow, however this approach will take up a log of resources both memory and cpu cycles, that potentially will render all the calls slow.

Instead we opt to log when a call has been completed, and determined slow.

__WARNING__: We log the arguments as they are when the call is completed. If arguments are altered by the method, they will not be seen as the input edition of them. This might hide the culprit for the call duration.

It is all based around an annotation `@SlowCallLog`, and `CDI` in `EE`. During bean discovery in the container, all `@SlowCallLog` annotations are picked up. An interceptor is configured with a wrapper for the given methods, that calls the method, and if the calls duration is above a threshold, the call in logged with its parameters and optionally `return value` or `exception` if the call fails.

### Example

A fully running payara-micro demonstration can be found in the [example](example) directory.

It is based around this structure:

```java
@Stateless // or any other bean annotation
public MyBackendClass {

    @SlowCallLog(env="BACKEND_TIMING", scale=.6, result=false, level="ERROR")
    List<Row> getDatabaseEntriesFor(String key, Timestamp whenish) {
        ...
    }

    @SlowCallLog(env="BACKEND_TIMING", scale=.4, params={1})
    Resp processEntries(List<Row> rows, String trackingId) {
        ...
    }

}
```

Called from:

```java
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

The calls are across bean-boundaries, so interceptors are in play. The environment variable `BACKEND_TIMING` is set to some duration (ex. `20ms`), it is expected that `getDatabaseEntriesFor()` takes under normal circumstances 60% of the wall clock time, and `processEntries()` about 40%.
This will log all the arguments and exceptions but not return values for the calls to `getDatabaseEntriesFor()`, that take more than 12ms, as an error.
And for `processEntries()` calls that take more that 8ms it will log trackingId and return value.

The duration from the environment variable has 2 special vales (case insensitive):

 * `off` - Don't log, no matter how long it takes
 * `always` - Always log

### Log output (MDC)

This is built upon the `slf4j` log framework.

The logger used is called `dk.dbc.commons.slowcalllog.SlowCallLog`. If the log level, declared in the annotation, is __not__ enabled initialization will fail.

Several values are set in the Mapped Diagnostic Contexts (MDC)

 * `call_duration_ms` records duration in milliseconds.
 * `class` the fully qualified class name
 * `method` the method name

These are useful for filtering the calls you're interested in.

NB. Do notice that if you set the `trackingId` or other values in the MDC in your call, it will __not__ be included in the log line for the call.

## StopWatch

### A @Context element for Jersey to track time spent

It is based around the syntax of:

```java
    @GET/POST/PUT
    ...
    public Object method(@Context StopWatch stopwatch, ...) {

```
or
```java
    @Context
    StopWatch stopWatch;
```

And the `StopWatch` is supplied by `Jersey`, and when the request is completed, ie. all data is sent to the client, the logger: `dk.dbc.commons.stopwatch.StopWatch` logs a line with the message `TIMINGS`, and MDC values for number of timer invocations and accumulated spent time.
An extra timer wil always be present: `total_ms` which has no corrosponding `total_count`. Which might be very different from a timing of the entire request method body, since that doesn't include time spent sending data to the client.


### Timing a scope

If you want to time multiple statements combined in one timer use the `AutoClosable` `Clock` instance:

```java
    try(Clock c = stopWatch.time("my_scope")) {
        ...
    }
```

This will count the time spent from the `.time()` call to the `Clock.close()` with the name `my_scope`

### Timing a simple (java.util.function) Supplier

A simple supplier can be timed like this:

```java
    Object x = stopWatch.timed("my_method", () -> doSomething());
```

### Timing a "supplier" that throws checked exceptions

If a method throws exceptions, the `java.lang.function.Supplier` interface cannot be used. Instead you can do:

```java
    Object x = stopWatch.timedWithExceptions("my_method", () -> doSomething())
        .checkfor(IOException.class)
        .value()
```

This method allows for `doSomething()` to throw checked exceptions. The `.checkfor(IOException.class)` re-throws the exception from the method, if it threw one of io-exception type. `.value()` takes the value, or warns and throws a `RuntimeException` wrapping the exception thrown by the method. Any `RuntimeException` thrown by the method is just throw immediately.

It also works for `void` methods. Then a `StopWatch.Value<Void>` is returned. Remember to chech for exceptions in that case. It is annotated with `@CheckReturn` so your IDE should help you there.

### Additional MDC values

Since the `Jersey` `@Context` is run before any `Interceptors`, tools like `dk.dbc:dbc-commons-mdc:1.0-SNAPSHOT` cannot be used to instantiate `MDC` trackingId and the like. Therefore the methods `.importMDC()` and `.setMDC("", "")` are included:

*  `.importMDC()` simply includes the current `MDC` map when logging the `TIMINGS` line
*  `.setMDC("", "")` allows for setting specific `MDC` values for logging

Usually it'll be enough to do a `stopWatch.importMDC()` whenever the trackingId has been set up. Remember the timings will overwrite values that are copied or set.

### Output

The logged output (ready for ELK) from the `curl http://localhost:8080/api/timings` call to [Timings.java](example/src/main/java/dk/dbc/example/Timings.java)

```json
{
  "timestamp": "...",
  "version": "1",
  "message": "TIMING",
  "logger": "dk.dbc.commons.timings.TimingContext",
  "thread": "...",
  "level": "INFO",
  "level_value": 20000,
  "HOSTNAME": "...",
  "mdc": {
    "make_tracking_id_count": "1",
    "body_ms": "1.817336",
    "body_count": "1",
    "parse_url_count": "1",
    "parse_url_ms": "0.04561",
    "make_tracking_id_ms": "0.052685",
    "trackingId": "..."
  }
}
```

Unfortunately the current `MDC` implementation from `slf4j` only supports `String` type, so logstash needs to learn how to map the values to float/int:

```
filter {
  json {
    source => "message"
  }
  ruby {
    code => "
```
```ruby
      mdc = event.get('[mdc]')
      if mdc != nil
        mdc.each do |k, v|
          if k.end_with?('_ms')
            mdc[k] = v.to_f
          elsif k.end_with?('_count')
            mdc[k] = v.to_i
          end
        end
        event.set('[mdc]', mdc)
      end
```
```
    "
  }
}
```