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
package org.commonjava.maven.galley.spi.event;

import org.commonjava.maven.galley.event.FileAccessEvent;
import org.commonjava.maven.galley.event.FileDeletionEvent;
import org.commonjava.maven.galley.event.FileErrorEvent;
import org.commonjava.maven.galley.event.FileNotFoundEvent;
import org.commonjava.maven.galley.event.FileStorageEvent;


public interface FileEventManager
{

    void fire( final FileNotFoundEvent evt );

    void fire( final FileStorageEvent evt );

    void fire( final FileAccessEvent evt );

    void fire( final FileDeletionEvent evt );

    void fire( final FileErrorEvent evt );
}
