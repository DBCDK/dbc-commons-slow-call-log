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

import java.util.Locale;

/**
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
enum NanoUnit {
    NS(1L, "ns"),
    US(1_000L, "µs"),
    MS(1_000_000L, "ms"),
    S(1_000_000_000L, "s");
    private final long nanos;
    private final String unit;

    NanoUnit(long nanos, String unit) {
        this.nanos = nanos;
        this.unit = unit;
    }

    String unitText() {
        return unit;
    }

    long nanoSeconds() {
        return nanos;
    }

    static NanoUnit of(String name) {
        if (name == null)
            throw new IllegalArgumentException("Don't know unset unit");
        switch (name.toLowerCase(Locale.ROOT)) {
            case "n":
            case "nano":
            case "nanos":
            case "nanosecond":
            case "nanoseconds":
            case "ns":
                return NS;
            case "micro":
            case "micros":
            case "microsecond":
            case "microseconds":
            case "u":
            case "us":
            case "µ":
            case "µs":
                return US;
            case "m":
            case "ms":
            case "milli":
            case "millis":
            case "millisecond":
            case "milliseconds":
                return MS;
            case "s":
            case "second":
            case "seconds":
                return S;
            default:
                throw new IllegalArgumentException("Don't know unit: " + name);
        }
    }
}
