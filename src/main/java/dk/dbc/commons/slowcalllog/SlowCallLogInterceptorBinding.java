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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import jakarta.interceptor.InterceptorBinding;

/**
 * Across bean boundary interceptor annotation
 * <p>
 * This is only added through {@link SlowCallLogInterceptorBindingExtension}
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
@InterceptorBinding
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@interface SlowCallLogInterceptorBinding {
}
