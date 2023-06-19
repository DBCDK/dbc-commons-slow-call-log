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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import jakarta.enterprise.util.Nonbinding;
import jakarta.interceptor.InterceptorBinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

/**
 * Across bean boundary interceptor annotation
 * <p>
 * This allows for logging parameters/return values for slow invocations
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
@Inherited
@Documented
@InterceptorBinding
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface SlowCallLog {

    Logger log = LoggerFactory.getLogger(SlowCallLog.class);

    /**
     * Environment variable that sets logging threshold
     * <p>
     * Name of environment variable containing a number and a unit.
     * Ex. "3ms"
     * <p>
     * defaults to {@code SLOW_CALL_THRESHOLD}
     *
     * @return VARIABLE NAME
     */
    @Nonbinding
    String env() default "SLOW_CALL_THRESHOLD";

    /**
     * Scale of value from {@link #env()}
     * <p>
     * For splitting time
     * <p>
     * This is useful if you set the value in {@link #env()} to 2ms which is all
     * the time a request is allowed to take. Then you annotate the GET/POST
     * method with 'scale = 1.0', if it has 3 sub calls you can annotate them
     * with scale proportional to the time they should take under normal
     * circumstances. Then you'll get logging for each of the methods that takes
     * a disproportionate amount of time.
     *
     * @return 1.0
     */
    @Nonbinding
    double scale() default 1.0;

    /**
     * The level (org.slf4j.event.Level) for the logging.
     *
     * @return TRACE/DEBUG/INFO/WARN/WARNING/ERROR
     */
    @Nonbinding
    Level level() default Level.WARN;

    /**
     * A list of parameter numbers (0 to parameter count - 1) to log
     * <p>
     * default is {-1} which is everything. use {} to not log any call
     * parameters
     *
     * @return {-1}
     */
    @Nonbinding
    int[] parameters() default {-1};

    /**
     * Set to true, if return value should be logged. This settings is ignored
     * if the method has void as return type
     *
     * @return true
     */
    @Nonbinding
    boolean result() default true;

    /**
     * Which unit to log time consumption in
     * <p>
     * understood units are:
     * <ul>
     * <li>(nano|micro|milli)(second)?(s)?
     * <li>(n|u|µ|m)(s)?
     * <li>second(s)?
     * <li>s
     * </ul>
     * <p>
     * note: MDC value is always milliseconds
     *
     * @return time unit
     */
    @Nonbinding
    String unit() default "ms";
}
