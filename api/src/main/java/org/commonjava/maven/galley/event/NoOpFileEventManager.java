/**
 * Copyright (C) 2013 Red Hat, Inc. (jdcasey@commonjava.org)
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
package org.commonjava.maven.galley.event;

import javax.enterprise.inject.Alternative;
import javax.inject.Named;

import org.commonjava.maven.galley.spi.event.FileEventManager;

@Named( "no-op-galley-events" )
@Alternative
public class NoOpFileEventManager
    implements FileEventManager
{

    @Override
    public void fire( final FileNotFoundEvent evt )
    {
    }

    @Override
    public void fire( final FileStorageEvent evt )
    {
    }

    @Override
    public void fire( final FileAccessEvent evt )
    {
    }

    @Override
    public void fire( final FileDeletionEvent evt )
    {
    }

    @Override
    public void fire( final FileErrorEvent evt )
    {
    }

}
