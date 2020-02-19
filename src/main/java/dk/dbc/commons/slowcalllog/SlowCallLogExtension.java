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

import java.util.ArrayList;
import java.util.List;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.WithAnnotations;

/**
 * This processes all {@link SlowCallLog} annotated methods, and enables an
 * interceptor for them.
 * <p>
 * This is triggered by: META-INF/services/javax.enterprise.inject.spi.Extension
 *
 * @author Morten Bøgeskov (mb@dbc.dk)
 */
@SuppressWarnings("PMD.UnusedPrivateMethod")
public class SlowCallLogExtension implements Extension {

    private final List<String> SETUP_ERRORS = new ArrayList<>();

    /**
     * Process all methods annotated with {@link SlowCallLog}
     *
     * @param <T>                  Type definition
     * @param processAnnotatedType the method metadata for the class with the
     *                             annotation
     */
    private <T> void processAnnotatedType(@Observes @WithAnnotations(SlowCallLog.class) ProcessAnnotatedType<T> processAnnotatedType) {
        AnnotatedType<T> annotatedType = processAnnotatedType.getAnnotatedType();

        if (annotatedType.getJavaClass().equals(SlowCallLogInterceptor.class))
            return;
        if (annotatedType.isAnnotationPresent(SlowCallLog.class))
            SETUP_ERRORS.add("@SlowCallLog is not a valid class annotation on: " + annotatedType.getJavaClass().getName());

        annotatedType.getMethods().stream()
                .filter(callable -> callable.isAnnotationPresent(SlowCallLog.class))
                .map(AnnotatedMethod::getJavaMember)
                .map(SlowCallLogInterceptor::wrapMethod)
                .filter(e -> e != null)
                .forEach(SETUP_ERRORS::add);
    }

    private void validationError(@Observes AfterBeanDiscovery afterBeanDiscovery) {
        SETUP_ERRORS.forEach(message -> afterBeanDiscovery.addDefinitionError(new IllegalStateException(message)));
        SETUP_ERRORS.clear();
    }
}
