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

import org.glassfish.hk2.api.Factory;

/**
 * Factory that provides {@link StopWatch} objects, and disposes of them
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class StopWatchFactory implements Factory<StopWatch> {

    @Override
    public StopWatch provide() {
        return new StopWatch();
    }

    @Override
    public void dispose(StopWatch t) {
        t.dispose();
    }
}
