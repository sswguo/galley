/**
 * Copyright (C) 2013 Red Hat, Inc. (nos-devel@redhat.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.maven.galley.io.checksum;

import com.codahale.metrics.Timer;
import org.commonjava.maven.galley.model.Transfer;

import java.io.IOException;
import java.util.function.Function;

public abstract class AbstractChecksumGeneratorFactory<T extends AbstractChecksumGenerator>
{

    protected AbstractChecksumGeneratorFactory()
    {
    }

    @SuppressWarnings( "DeprecatedIsStillUsed" )
    @Deprecated
    public final T createGenerator( final Transfer transfer )
        throws IOException
    {
        return createGenerator( transfer, true, null );
    }

    public final T createGenerator( final Transfer transfer, boolean writeChecksumFiles,
                                    final Function<String, Timer.Context> timerProvider )
            throws IOException
    {
        return newGenerator( transfer, writeChecksumFiles, timerProvider == null ? (s)->null : timerProvider );
    }

    protected abstract T newGenerator( Transfer transfer, boolean writeChecksumFiles,
                                       final Function<String, Timer.Context> timerProvider )
        throws IOException;

}
