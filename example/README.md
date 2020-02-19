# Example Payara Application

An example that uses `dbc-commons-slow-call-log`


## Building

Remember to build library first (and install it) by running: `(cd .. && mvn install)`

The you can build the JakartaEE application by running: `mvn package` - this also downloads a `payara-micro.jar`

## Running
You can deploy it by running:
```
env CALL_THRESHOLD=20ms \
    java -Dlogback.configurationFile=$PWD/logback.xml -DlogbackDisableServletContainerInitializer=true \
         -Dhazelcast.phone.home.enabled=false \
         -jar target/payara-micro.jar \
         --nocluster \
         --contextroot / --deploy target/dbc-commons-slow-call-log-example-1.0-SNAPSHOT.war
```

## Testing

Using `curl` to test it

 * `curl -D /dev/stdout 'http://localhost:8080/api/ping?s=25'` - tells it to sleep for 25 ms
 * `curl -D /dev/stdout 'http://localhost:8080/api/ping?s=25&client'` - sleep and produce an exception (code 400)
 * `curl -D /dev/stdout 'http://localhost:8080/api/ping?s=25&server'` - sleep and produce an exception (code 500)

You will hit [this class](src/main/java/dk/dbc/example/Ping.java), and given it's annotation you should expect the output to be a lot like this (this is pretty printed using `jq`):

```
{
  "timestamp": "...",
  "version": "1",
  "message": "dk.dbc.example.Ping.ping([25]) = [Status{ok=true, message=pong!}] (25873µs)",
  "logger": "dk.dbc.commons.slowcalllog.SlowCallLog",
  "thread": "http-thread-pool::http-listener(2)",
  "level": "ERROR",
  "level_value": 40000,
  "HOSTNAME": "...",
  "mdc": {
    "call_duration_ms": "25.873413",
    "method": "ping",
    "class": "dk.dbc.example.Ping"
  }
}
```