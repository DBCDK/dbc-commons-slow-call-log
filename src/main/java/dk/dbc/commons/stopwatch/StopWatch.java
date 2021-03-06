/*
 * Copyright (C) 2020 DBC A/S (http://dbc.dk/)
 *
 * This is part of dbc-commons-slow-call-log
 *
 * dbc-commons-slow-call-log is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * dbc-commons-slow-call-log is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package dk.dbc.commons.stopwatch;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import javax.annotation.CheckReturnValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class StopWatch {

    private static final Logger log = LoggerFactory.getLogger(StopWatch.class);

    /**
     * A supplier like {@link Supplier} that throws exception(s)
     *
     * @param <T> the type of results supplied by this supplier
     */
    public interface SupplierWithCheckedExceptions<T> {

        T get() throws Exception;
    }

    /**
     * A Code block that does not throw checked exceptions
     */
    public interface VoidBlock {

        void perform();
    }

    /**
     * A Code block that throws checked exceptions
     */
    public interface VoidBlockWithCheckedExceptions {

        void perform() throws Exception;
    }

    /**
     * Wrapper for carrying return value or exception
     *
     * @param <T> the type of results supplied by this wrapper
     */
    public interface Value<T> {

        /**
         * Get the value
         * <p>
         * if an exception was thrown and not propagandated by
         * {@link #checkFor(java.lang.Class)} it is wrapped in a
         * RuntimeException and thrown
         *
         * @return the result value
         */
        T value();

        /**
         * If the supplier threw an exception of this type (or inherited from
         * this type) rethrow it.
         *
         * @param <E>   Exception type
         * @param clazz class indicator
         * @return self for chaining
         * @throws E exception
         */
        @CheckReturnValue
        <E extends Exception> Value<T> checkFor(Class<E> clazz) throws E;
    }

    private static class TimerEntry {

        private long ns;
        private int count;

        private TimerEntry() {
            this.ns = 0;
            this.count = 0;
        }

        private synchronized void add(long ns) {
            this.ns += ns;
            this.count++;
        }
    }

    private final ConcurrentMap<String, TimerEntry> timers;
    private final ConcurrentMap<String, String> mdc;
    private final long start;

    public StopWatch() {
        this.timers = new ConcurrentHashMap<>();
        this.mdc = new ConcurrentHashMap<>();
        this.start = System.nanoTime();
    }

    /**
     * Register an {@link MDC} entry
     *
     * @param key   name of value
     * @param value content
     */
    public void setMDC(String key, String value) {
        mdc.put(key, value);
    }

    /**
     * Clone the current {@link MDC}
     */
    public void importMDC() {
        mdc.putAll(MDC.getCopyOfContextMap());
    }

    /**
     * Log the registered times
     */
    @SuppressFBWarnings(value = "IS2_INCONSISTENT_SYNC", justification = "This is not called in a threaded context - only upon context cleanup")
    void dispose() {
        MDC.setContextMap(mdc);
        timers.forEach((name, entry) -> {
            MDC.put(name + "_ms", String.valueOf(( (double) entry.ns ) / 1_000_000.0));
            MDC.put(name + "_count", String.valueOf(entry.count));
        });
        MDC.put("total_ms", String.valueOf(( (double) System.nanoTime() - start ) / 1_000_000.0));
        log.info("TIMING");
        MDC.clear();
    }

    /**
     * Create a timing context
     *
     * @param name {@link MDC} name of timing information
     * @return auto-closable context
     */
    public Clock time(String name) {
        TimerEntry entry = timers.computeIfAbsent(name.replaceAll("[^_0-9a-zA-Z]", ""), s -> new TimerEntry());
        long nano = System.nanoTime();
        return () -> entry.add(System.nanoTime() - nano);
    }

    /**
     * Call a supplier and time it
     *
     * @param <T>      type of return value
     * @param name     {@link MDC} name of timing information
     * @param supplier method that produces a value
     * @return the result of {@link Supplier#get()}
     */
    public <T> T timed(String name, Supplier<T> supplier) {
        try (Clock timer = time(name)) {
            return supplier.get();
        }
    }

    /**
     * Call a code segment and time it
     *
     * @param name  {@link MDC} name of timing information
     * @param block code segment to time
     */
    public void timed(String name, VoidBlock block) {
        try (Clock timer = time(name)) {
            block.perform();
        }
    }

    /**
     * Call a supplier and time it
     *
     * @param <T>      type of return value
     * @param name     {@link MDC} name of timing information
     * @param supplier method that produces a value
     * @return the result of {@link Supplier#get()} wrapped in a
     *         {@link Value}
     */
    @CheckReturnValue
    public <T> Value<T> timedWithExceptions(String name, SupplierWithCheckedExceptions<T> supplier) {
        try (Clock timer = time(name)) {
            return new ValueWithValue<>(supplier.get());
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            return new ValueWithCheckedExceptionThrown<>(ex);
        }
    }

    /**
     * Call a code segment and time it
     *
     * Remember to check for exceptions
     *
     * @param name  {@link MDC} name of timing information
     * @param block code segment to time
     * @return the result of {@link Supplier#get()} wrapped in a
     *         {@link Value}
     */
    @CheckReturnValue
    public Value<Void> timedWithExceptions(String name, VoidBlockWithCheckedExceptions block) {
        try (Clock timer = time(name)) {
            block.perform();
            return new ValueWithValue<>(null);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            return new ValueWithCheckedExceptionThrown<>(ex);
        }
    }

    private static class ValueWithValue<T> implements Value<T> {

        private final T obj;

        private ValueWithValue(T obj) {
            this.obj = obj;
        }

        @Override
        public T value() {
            return obj;
        }

        @Override
        public <E extends Exception> Value<T> checkFor(Class<E> clazz) throws E {
            return this;
        }
    }

    private static class ValueWithCheckedExceptionThrown<T> implements Value<T> {

        private final Exception ex;

        private ValueWithCheckedExceptionThrown(Exception ex) {
            this.ex = ex;
        }

        @Override
        public T value() {
            throw new RuntimeException(ex);
        }

        @Override
        public <E extends Exception> Value<T> checkFor(Class<E> clazz) throws E {
            if (clazz.isAssignableFrom(ex.getClass()))
                throw (E) ex;
            return this;
        }
    }
}
