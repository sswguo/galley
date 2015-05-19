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
package org.commonjava.maven.galley.transport.htcli.internal;

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.commonjava.maven.galley.TransferException;
import org.commonjava.maven.galley.spi.transport.PublishJob;
import org.commonjava.maven.galley.transport.htcli.Http;
import org.commonjava.maven.galley.transport.htcli.internal.util.TransferResponseUtils;
import org.commonjava.maven.galley.transport.htcli.model.HttpLocation;
import org.commonjava.maven.galley.util.ContentTypeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HttpPublish
    implements PublishJob
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final String url;

    private final HttpLocation location;

    private final Http http;

    private TransferException error;

    private final InputStream stream;

    private final long length;

    private final String contentType;

    public HttpPublish( final String url, final HttpLocation location, final InputStream stream, final long length, final Http http )
    {
        this( url, location, stream, length, null, http );
    }

    public HttpPublish( final String url, final HttpLocation location, final InputStream stream, final long length, final String contentType,
                        final Http http )
    {
        this.url = url;
        this.location = location;
        this.stream = stream;
        this.length = length;
        this.contentType = contentType == null ? ContentTypeUtils.detectContent( url ) : contentType;
        this.http = http;
    }

    @Override
    public Boolean call()
    {
        //            logger.info( "Trying: {}", url );
        final HttpPut request = new HttpPut( url );
        request.setEntity( new InputStreamEntity( stream, length, ContentType.create( contentType ) ) );

        http.bindCredentialsTo( location, request );

        try
        {
            executePut( request, url );
            return true;
        }
        catch ( final TransferException e )
        {
            this.error = e;
            return false;
        }
        finally
        {
            cleanup( request );
        }
    }

    @Override
    public TransferException getError()
    {
        return error;
    }

    private HttpResponse executePut( final HttpPut request, final String url )
        throws TransferException
    {
        try
        {
            final HttpResponse response = http.getClient()
                                              .execute( request );

            final StatusLine line = response.getStatusLine();
            final int sc = line.getStatusCode();

            if ( sc != HttpStatus.SC_OK && sc != HttpStatus.SC_CREATED )
            {
                logger.warn( "{} : {}", line, url );
                return TransferResponseUtils.handleUnsuccessfulResponse( request, response, url, false );
            }
            else
            {
                return response;
            }
        }
        catch ( final ClientProtocolException e )
        {
            throw new TransferException( "Repository remote request failed for: {}. Reason: {}", e, url, e.getMessage() );
        }
        catch ( final IOException e )
        {
            throw new TransferException( "Repository remote request failed for: {}. Reason: {}", e, url, e.getMessage() );
        }
    }

    private void cleanup( final HttpPut request )
    {
        http.clearBoundCredentials( location );
        request.abort();
        http.closeConnection();
    }

}
