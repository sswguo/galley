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
package org.commonjava.maven.galley.io.checksum;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.Collections;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.SimpleLocation;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.testing.core.ApiFixture;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ChecksummingOutputStreamTest
{

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Rule
    public ApiFixture fixture = new ApiFixture( temp );

    @Before
    public void before()
    {
        fixture.initMissingComponents();
    }

    @Test
    public void verifyUsingMd5()
        throws Exception
    {
        final Transfer txfr =
            fixture.getCache()
                   .getTransfer( new ConcreteResource( new SimpleLocation( "test:uri" ), "my-path.txt" ) );
        final OutputStream os = new ByteArrayOutputStream();

        final byte[] data = "this is a test".getBytes();

        ChecksummingOutputStream stream = null;
        try
        {
            stream =
                new ChecksummingOutputStream(
                                              Collections.<AbstractChecksumGeneratorFactory<?>> singleton( new Md5GeneratorFactory() ),
                                              os, txfr );

            stream.write( data );
        }
        finally
        {
            IOUtils.closeQuietly( stream );
        }

        final MessageDigest md = MessageDigest.getInstance( "MD5" );
        md.update( data );
        final byte[] digest = md.digest();
        final String digestHex = Hex.encodeHexString( digest );

        final Transfer md5Txfr = txfr.getSiblingMeta( ".md5" );
        InputStream in = null;
        String resultHex = null;
        try
        {
            in = md5Txfr.openInputStream();

            resultHex = IOUtils.toString( in );
        }
        finally
        {
            IOUtils.closeQuietly( in );
        }

        assertThat( resultHex, equalTo( digestHex ) );
    }

}
