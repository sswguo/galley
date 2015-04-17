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
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;

import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.params.HttpParams;
import org.commonjava.maven.galley.auth.PasswordEntry;
import org.commonjava.maven.galley.spi.auth.PasswordManager;
import org.commonjava.maven.galley.transport.htcli.Http;
import org.commonjava.maven.galley.transport.htcli.model.HttpLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocationSSLSocketFactory
    extends SSLSocketFactory
{

    // private final Map<Location, SSLSocketFactory> repoFactories = new WeakHashMap<Location, SSLSocketFactory>();

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final TLLocationCredentialsProvider credProvider;

    private final PasswordManager passwordManager;

    public LocationSSLSocketFactory( final PasswordManager passwordManager, final TLLocationCredentialsProvider credProvider )
        throws KeyManagementException, UnrecoverableKeyException, NoSuchAlgorithmException, KeyStoreException
    {
        super( (TrustStrategy) null, BROWSER_COMPATIBLE_HOSTNAME_VERIFIER );
        this.passwordManager = passwordManager;
        this.credProvider = credProvider;
    }

    @Override
    public Socket createSocket( final HttpParams params )
        throws IOException
    {
        //        logger.info( "Creating socket...looking for repository definition in parameters..." );
        final HttpLocation repo = (HttpLocation) params.getParameter( Http.HTTP_PARAM_LOCATION );

        if ( repo != null )
        {
            //            logger.info( "Creating socket...using repository: {}", repo );
            final SSLSocketFactory fac = getSSLFactory( repo );
            if ( fac != null )
            {
                //                logger.info( "Creating socket using repo-specific factory" );
                return fac.createSocket( params );
            }
            else
            {
                //                logger.info( "No repo-specific factory; Creating socket using default factory (this)" );
                return super.createSocket( params );
            }
        }
        else
        {
            //            logger.info( "No repo; Creating socket using default factory (this)" );
            return super.createSocket( params );
        }
    }

    private synchronized SSLSocketFactory getSSLFactory( final HttpLocation loc )
        throws IOException
    {
        //        logger.info( "Finding SSLSocketFactory for repo: {}", repo.getKey() );

        SSLSocketFactory factory = null; // repoFactories.get( repo );
        if ( factory == null )
        {
            KeyStore ks = null;
            KeyStore ts = null;

            final String kcPem = loc.getKeyCertPem();
            final String kcPass = passwordManager.getPassword( new PasswordEntry( loc, PasswordEntry.KEY_PASSWORD ) );
            if ( kcPem != null )
            {
                if ( kcPass == null || kcPass.length() < 1 )
                {
                    logger.error( "Invalid configuration. Location: {} cannot have an empty key password!", loc.getUri() );
                    throw new IOException( "Location: " + loc.getUri() + " is misconfigured!" );
                }

                try
                {
                    ks = SSLUtils.readKeyAndCert( kcPem, kcPass );

                    //                    final StringBuilder sb = new StringBuilder();
                    //                    sb.append( "Keystore contains the following certificates:" );
                    //
                    //                    for ( final Enumeration<String> aliases = ks.aliases(); aliases.hasMoreElements(); )
                    //                    {
                    //                        final String alias = aliases.nextElement();
                    //                        final X509Certificate cert = (X509Certificate) ks.getCertificate( alias );
                    //
                    //                        if ( cert != null )
                    //                        {
                    //                            sb.append( "\n" )
                    //                              .append( cert.getSubjectDN() );
                    //                        }
                    //                    }
                    //                    sb.append( "\n" );
                    //                    logger.info( sb.toString() );
                }
                catch ( final CertificateException e )
                {
                    logger.error( String.format( "Invalid configuration. Location: %s has an invalid client certificate! Error: %s", loc.getUri(), e.getMessage() ), e );
                    throw new IOException( "Failed to initialize SSL connection for repository: " + loc.getUri() );
                }
                catch ( final KeyStoreException e )
                {
                    logger.error( String.format( "Invalid configuration. Cannot initialize keystore for repository: %s. Error: %s", loc.getUri(), e.getMessage() ), e );
                    throw new IOException( "Failed to initialize SSL connection for repository: " + loc.getUri() );
                }
                catch ( final NoSuchAlgorithmException e )
                {
                    logger.error( String.format( "Invalid configuration. Cannot initialize keystore for repository: %s. Error: %s", loc.getUri(), e.getMessage() ), e );
                    throw new IOException( "Failed to initialize SSL connection for repository: " + loc.getUri() );
                }
                catch ( final InvalidKeySpecException e )
                {
                    logger.error( String.format( "Invalid configuration. Invalid client key for repository: %s. Error: %s", loc.getUri(), e.getMessage() ), e );
                    throw new IOException( "Failed to initialize SSL connection for repository: " + loc.getUri() );
                }
            }

            final String sPem = loc.getServerCertPem();
            //            logger.info( "Server certificate PEM:\n{}", sPem );
            if ( sPem != null )
            {
                try
                {
                    ts = SSLUtils.readCerts( sPem, loc.getHost() );

                    //                    final StringBuilder sb = new StringBuilder();
                    //                    sb.append( "Trust store contains the following certificates:" );
                    //
                    //                    for ( final Enumeration<String> aliases = ts.aliases(); aliases.hasMoreElements(); )
                    //                    {
                    //                        final String alias = aliases.nextElement();
                    //                        final X509Certificate cert = (X509Certificate) ts.getCertificate( alias );
                    //                        if ( cert != null )
                    //                        {
                    //                            sb.append( "\n" )
                    //                              .append( cert.getSubjectDN() );
                    //                        }
                    //                    }
                    //                    sb.append( "\n" );
                    //                    logger.info( sb.toString() );
                }
                catch ( final CertificateException e )
                {
                    logger.error( String.format( "Invalid configuration. Location: %s has an invalid server certificate! Error: %s", loc.getUri(), e.getMessage() ), e );
                    throw new IOException( "Failed to initialize SSL connection for repository: " + loc.getUri() );
                }
                catch ( final KeyStoreException e )
                {
                    logger.error( String.format( "Invalid configuration. Cannot initialize keystore for repository: %s. Error: %s", loc.getUri(), e.getMessage() ), e );
                    throw new IOException( "Failed to initialize SSL connection for repository: " + loc.getUri() );
                }
                catch ( final NoSuchAlgorithmException e )
                {
                    logger.error( String.format( "Invalid configuration. Cannot initialize keystore for repository: %s. Error: %s", loc.getUri(), e.getMessage() ), e );
                    throw new IOException( "Failed to initialize SSL connection for repository: " + loc.getUri() );
                }
            }

            if ( ks != null || ts != null )
            {
                try
                {
                    factory =
                        new SSLSocketFactory( SSLSocketFactory.TLS, ks, kcPass, ts, null, null, SSLSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER );

                    // repoFactories.put( repo, factory );
                }
                catch ( final KeyManagementException e )
                {
                    logger.error( "Invalid configuration. Cannot initialize SSL socket factory for repository: {}. Error: {}", e, loc.getUri(),
                                  e.getMessage() );
                    throw new IOException( "Failed to initialize SSL connection for repository: " + loc.getUri() );
                }
                catch ( final UnrecoverableKeyException e )
                {
                    logger.error( "Invalid configuration. Cannot initialize SSL socket factory for repository: {}. Error: {}", e, loc.getUri(),
                                  e.getMessage() );
                    throw new IOException( "Failed to initialize SSL connection for repository: " + loc.getUri() );
                }
                catch ( final NoSuchAlgorithmException e )
                {
                    logger.error( "Invalid configuration. Cannot initialize SSL socket factory for repository: {}. Error: {}", e, loc.getUri(),
                                  e.getMessage() );
                    throw new IOException( "Failed to initialize SSL connection for repository: " + loc.getUri() );
                }
                catch ( final KeyStoreException e )
                {
                    logger.error( "Invalid configuration. Cannot initialize SSL socket factory for repository: {}. Error: {}", e, loc.getUri(),
                                  e.getMessage() );
                    throw new IOException( "Failed to initialize SSL connection for repository: " + loc.getUri() );
                }
            }
        }

        return factory;
    }

    @SuppressWarnings( "deprecation" )
    @Override
    public Socket createLayeredSocket( final Socket socket, final String host, final int port, final boolean autoClose )
        throws IOException, UnknownHostException
    {
        //        logger.info( "Creating LAYERED socket to: {}:{}...looking for repository definition in parameters...", host,
        //                     port );

        // FIXME: This is prone to confusion if multiple repos using the same host/port have different configs!!!
        final HttpLocation repo = credProvider.getLocation( host, port < 0 ? 443 : port );

        if ( repo != null )
        {
            //            logger.info( "Creating socket...using repository: {}", repo );
            final SSLSocketFactory fac = getSSLFactory( repo );
            if ( fac != null )
            {
                //                logger.info( "Creating socket using repo-specific factory" );
                return fac.createLayeredSocket( socket, host, port, autoClose );
            }
            else
            {
                //                logger.info( "No repo-specific factory; Creating socket using default factory (this)" );
                return super.createLayeredSocket( socket, host, port, autoClose );
            }
        }
        else
        {
            //            logger.info( "No repo; Creating socket using default factory (this)" );
            return super.createLayeredSocket( socket, host, port, autoClose );
        }
    }

}
