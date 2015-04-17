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
package org.commonjava.maven.galley.transport.htcli;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.HttpProtocolParams;
import org.commonjava.maven.galley.spi.auth.PasswordManager;
import org.commonjava.maven.galley.transport.htcli.internal.LocationSSLSocketFactory;
import org.commonjava.maven.galley.transport.htcli.internal.TLLocationCredentialsProvider;
import org.commonjava.maven.galley.transport.htcli.model.HttpLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpImpl
    implements Http
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private LocationSSLSocketFactory socketFactory;

    private TLLocationCredentialsProvider credProvider;

    private DefaultHttpClient client;

    private final PasswordManager passwords;

    private ClientConnectionManager connectionManager;

    public HttpImpl( final PasswordManager passwords )
    {
        this( passwords, 20 );
    }

    public HttpImpl( final PasswordManager passwords, final int maxConnections )
    {
        this.passwords = passwords;
        setup();
    }

    public HttpImpl( final PasswordManager passwordManager, final ClientConnectionManager connectionManager )
    {
        passwords = passwordManager;
        this.connectionManager = connectionManager;
        setup();
    }

    protected void setup()
    {
        ClientConnectionManager connMgr = connectionManager;
        if ( connMgr == null )
        {
            final PoolingClientConnectionManager ccm = new PoolingClientConnectionManager();

            // TODO: Make this configurable
            ccm.setMaxTotal( 20 );
            connMgr = ccm;
        }

        credProvider = new TLLocationCredentialsProvider( passwords );

        try
        {
            socketFactory = new LocationSSLSocketFactory( passwords, credProvider );

            final SchemeRegistry registry = connMgr.getSchemeRegistry();
            registry.register( new Scheme( "https", 443, socketFactory ) );
        }
        catch ( final KeyManagementException e )
        {
            logger.error( String.format( "Failed to setup SSLSocketFactory. SSL mutual authentication will not be available!\nError: %s",
                                         e.getMessage() ), e );
        }
        catch ( final UnrecoverableKeyException e )
        {
            logger.error( String.format( "Failed to setup SSLSocketFactory. SSL mutual authentication will not be available!\nError: %s",
                                         e.getMessage() ), e );
        }
        catch ( final NoSuchAlgorithmException e )
        {
            logger.error( String.format( "Failed to setup SSLSocketFactory. SSL mutual authentication will not be available!\nError: %s",
                                         e.getMessage() ), e );
        }
        catch ( final KeyStoreException e )
        {
            logger.error( String.format( "Failed to setup SSLSocketFactory. SSL mutual authentication will not be available!\nError: %s",
                                         e.getMessage() ), e );
        }

        final DefaultHttpClient hc = new DefaultHttpClient( connMgr );
        hc.setCredentialsProvider( credProvider );

        HttpProtocolParams.setVersion( hc.getParams(), HttpVersion.HTTP_1_1 );

        client = hc;
    }

    @Override
    public HttpClient getClient()
    {
        return client;
    }

    @Override
    public void bindCredentialsTo( final HttpLocation location, final HttpRequest request )
    {
        credProvider.bind( location );

        if ( location.getProxyHost() != null )
        {
            //            logger.info( "Using proxy: {}:{} for repository: {}", repository.getProxyHost(),
            //                         repository.getProxyPort() < 1 ? 80 : repository.getProxyPort(), repository.getName() );

            final int proxyPort = location.getProxyPort();
            HttpHost proxy;
            if ( proxyPort < 1 )
            {
                proxy = new HttpHost( location.getProxyHost(), -1, "http" );
            }
            else
            {
                proxy = new HttpHost( location.getProxyHost(), location.getProxyPort(), "http" );
            }

            request.getParams()
                   .setParameter( ConnRoutePNames.DEFAULT_PROXY, proxy );
        }

        request.getParams()
               .setParameter( Http.HTTP_PARAM_LOCATION, location );
    }

    @Override
    public void closeConnection()
    {
        client.getConnectionManager()
              .closeExpiredConnections();

        client.getConnectionManager()
              .closeIdleConnections( 2, TimeUnit.SECONDS );
    }

    @Override
    public void clearBoundCredentials( final HttpLocation location )
    {
        credProvider.clear();
    }

    @Override
    public void clearAllBoundCredentials()
    {
        credProvider.clear();
    }

}
