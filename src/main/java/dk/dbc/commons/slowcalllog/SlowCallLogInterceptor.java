/*
 * Copyright (C) 2020 DBC A/S (http://dbc.dk/)
 *
 * This is part of slow-call-log
 *
 * slow-call-log is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * slow-call-log is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.commons.slowcalllog;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.event.Level;

/**
 * Across bean boundary interceptor annotation
 * <p>
 * This allows for logging parameters/return values for show invocations
 * <p>
 * This is added via the {@link SlowCallLogExtension} to methods that are
 * annotated with {@link SlowCallLog}. This should not be used directly.
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
@SlowCallLog
@Interceptor
@Priority(Interceptor.Priority.LIBRARY_BEFORE + 1)
@SuppressWarnings("PMD.UnusedPrivateMethod")
public class SlowCallLogInterceptor {

    private static final Logger log = LoggerFactory.getLogger(SlowCallLogInterceptor.class);

    private static final HashMap<Method, Invoker> WRAPPERS = new HashMap<>();
    private static final Invoker DEFAULT_WRAPPER = InvocationContext::proceed;
    private static final String MDC_DURATION = "call_duration_ms";

    @FunctionalInterface
    private interface Invoker {

        Object call(InvocationContext ic) throws Exception;
    }

    @FunctionalInterface
    private interface LogPrinter {

        void call(long time, Object[] params, Object result);
    }

    @AroundInvoke
    private Object methodInvocation(InvocationContext context) throws Exception {
        return WRAPPERS.getOrDefault(context.getMethod(), DEFAULT_WRAPPER)
                .call(context);
    }

    static String wrapMethod(Method method) {
        try {
            SlowCallLog slowCallLog = method.getAnnotation(SlowCallLog.class);
            if (slowCallLog == null)
                return null;
            int[] params = paramList(slowCallLog, method);
            long maxInvocationDurationInNs = logDuration(slowCallLog);
            BiConsumer<String, Object[]> logger = loggerForLevel(slowCallLog);
            NanoUnit logUnit = NanoUnit.of(slowCallLog.unit());
            LogPrinter exceptionLogger = loggerFor(method, params, true, logUnit, logger);
            LogPrinter slowLogger = slowCallLog.result() ? exceptionLogger : loggerFor(method, params, false, logUnit, logger);
            Invoker invoker = makeInvoker(maxInvocationDurationInNs, slowLogger, exceptionLogger);
            WRAPPERS.put(method, invoker);
            log.info("SlowCallLog for: {} with a max duration of {}ns", method.toGenericString(), maxInvocationDurationInNs);
        } catch (RuntimeException ex) {
            return ex.getMessage() + " for " + method.toGenericString();
        }
        return null;
    }

    /**
     * Ensure parameter indexes are valid
     *
     * @param slowCallLog annotation
     * @param method      then method that provides parameters
     * @return list of parameters to log
     */
    private static int[] paramList(SlowCallLog slowCallLog, Method method) {
        int[] params = slowCallLog.parameters();
        if (params.length == 1 && params[0] == -1)
            return IntStream.range(0, method.getParameterCount()).toArray();
        for (int param : params) {
            if (param < 0)
                throw new IllegalArgumentException("Cannot have positional parameters less than 0");
            if (param >= method.getParameterCount())
                throw new IllegalArgumentException("Cannot have positional parameters greater than or equal to " + method.getParameterCount());
        }
        return params;
    }

    private static Invoker makeInvoker(long thresholdInNs, LogPrinter logger, LogPrinter exception) {
        return ic -> {
            long before = System.nanoTime();
            try {
                Object ret = ic.proceed();
                long duration = System.nanoTime() - before;
                if (duration >= thresholdInNs)
                    logger.call(duration, ic.getParameters(), ret);
                return ret;
            } catch (Exception ex) {
                long duration = System.nanoTime() - before;
                if (duration >= thresholdInNs)
                    exception.call(duration, ic.getParameters(), makeExceptionString(ex));
                throw ex;
            }
        };
    }

    /**
     * If en exception doesn't have a message find the one that does, and give a
     * meaning full message
     * <p>
     * Includes names of exception classes that didn't have messages
     *
     * @param tr Exception to get message for
     * @return first real message of the causes
     */
    static String makeExceptionString(Throwable tr) {
        StringBuilder cause = new StringBuilder();
        for (;;) {
            cause.append(tr.getClass().getName());
            String message = tr.getMessage();
            if (message != null) {
                cause.append(": ")
                        .append(message);
                break;
            }
            tr = tr.getCause();
            if (tr == null)
                break;
            cause.append(" > ");
        }
        return cause.toString();
    }

    private static LogPrinter loggerFor(Method method, int[] parameters, boolean withResult, NanoUnit timingUnit, BiConsumer<String, Object[]> logger) {
        String pattern = new StringBuilder()
                .append(method.getDeclaringClass().getCanonicalName()) //class
                .append(".")
                .append(method.getName()) // method
                .append("(")
                .append(IntStream.of(parameters) // call args
                        .mapToObj(i -> "[{}]")
                        .collect(Collectors.joining(", ")))
                .append(") ")
                .append(withResult ? " = [{}] " : "") // optional result
                .append("({}") //duration
                .append(timingUnit.unitText())
                .append(')')
                .toString();
        long timeScaler = timingUnit.nanoSeconds();
        return (time, params, result) -> {
            ParamToStringMapper parammeters = new ParamToStringMapper(params);
            Stream.Builder<Object> builder = Stream.builder();
            IntStream.of(parameters)
                    .mapToObj(parammeters::asStringValue)
                    .forEach(builder::add);
            if (withResult)
                builder.add(result);
            builder.add(( time + timeScaler / 2 ) / timeScaler);
            String oldValue = MDC.get(MDC_DURATION);
            MDC.put(MDC_DURATION, String.valueOf(( (double) time ) / 1_000_000.0)); // ms
            logger.accept(pattern, builder.build().toArray());
            if (oldValue != null)
                MDC.put(MDC_DURATION, oldValue);
            else
                MDC.remove(MDC_DURATION);
        };
    }

    private static BiConsumer<String, Object[]> loggerForLevel(SlowCallLog slowCallLog) {
        Level logLevel = slowCallLog.level();
        switch (logLevel) {
            case TRACE:
                return SlowCallLog.log::trace;
            case DEBUG:
                return SlowCallLog.log::debug;
            case INFO:
                return SlowCallLog.log::info;
            case WARN:
                return SlowCallLog.log::warn;
            case ERROR:
                return SlowCallLog.log::error;
            default:
                throw new IllegalArgumentException("Invalid log level: " + logLevel);
        }
    }

    /**
     * Figure out how much time a slow call needs to take
     *
     * @param slowCallLog annotation
     * @return number of nanoseconds
     * @throws IllegalArgumentException if the environment variable does not
     *                                  resolve to a duration
     */
    private static long logDuration(SlowCallLog slowCallLog) throws IllegalArgumentException {
        String variableName = slowCallLog.env();
        String env = System.getenv(variableName);
        if (env == null)
            throw new IllegalArgumentException("Unknown variable: $" + variableName + " for logging threshold");
        return (long) ( slowCallLog.scale() * (double) durationInNs(env) );
    }

    /**
     * Parse a duration string
     *
     * @param duration string with content of the type: {number} {unit}
     * @return number of nano seconds
     */
    static long durationInNs(String duration) {
        try {
            String[] parts = duration.split("(?=\\D)", 2);
            if (parts.length != 2)
                throw new IllegalArgumentException("Don't know duration: " + duration);
            long amount = Long.parseUnsignedLong(parts[0].trim());
            return amount * NanoUnit.of(parts[1].trim()).nanoSeconds();
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Don't know duration: " + duration);

        }
    }

    private static class ParamToStringMapper {

        private final Object[] params;

        ParamToStringMapper(Object[] params) {
            this.params = params;
        }

        String asStringValue(int index) {
            Object param = params[index];
            if (param != null && param.getClass().isArray())
                return Arrays.toString((Object[]) param);
            return String.valueOf(param);
        }
    }
}
