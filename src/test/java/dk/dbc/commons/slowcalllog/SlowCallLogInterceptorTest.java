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

import java.time.Duration;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import static dk.dbc.commons.slowcalllog.SlowCallLogInterceptor.*;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class SlowCallLogInterceptorTest {

    @Test
    public void testDurationParsing() throws Exception {
        System.out.println("testDurationParsing");
        assertTimeout(
                Duration.ofMillis(2000),
                () -> {
            assertThat(durationInNs("2000 ns"), is(durationInNs("2us")));
            assertThat(durationInNs("2000 us"), is(durationInNs("2ms")));
            assertThat(durationInNs("2000 ms"), is(durationInNs("2s")));
            String message = assertThrows(IllegalArgumentException.class, () -> durationInNs("-2ns")).getMessage();
            assertThat(message, Matchers.containsString("-2ns"));
        });
    }

    @Test
    public void testBuildExceptionString() throws Exception {
        System.out.println("testBuildExceptionString");
        assertTimeout(
                Duration.ofMillis(2000),
                () -> {
            assertThat(makeExceptionString(new IllegalStateException()),
                       is("java.lang.IllegalStateException"));
            assertThat(makeExceptionString(new RuntimeException(
                    null,
                    new IllegalArgumentException("xxx"))),
                       is("java.lang.RuntimeException > java.lang.IllegalArgumentException: xxx"));
        });
    }
}
