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

import jakarta.ws.rs.core.FeatureContext;
import org.glassfish.jersey.internal.spi.ForcedAutoDiscoverable;

/**
 * Autodiscoverable Jersey feature
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
public class StopWatchAutoDiscover implements ForcedAutoDiscoverable {

    @Override
    public void configure(FeatureContext context) {
        if (!context.getConfiguration().isRegistered(StopWatchFeature.class)) {
            context.register(StopWatchFeature.class);
        }
    }
}
