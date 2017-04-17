package org.commonjava.maven.galley.io.nocache;

import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.transport.htcli.model.HttpExchangeMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by ruhan on 4/25/17.
 */
public class NoCacheInputStream
                extends FilterInputStream
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final Transfer transfer;

    public NoCacheInputStream( final InputStream stream, final Transfer transfer )
    {
        super( stream );
        this.transfer = transfer;
    }

    @Override
    public void close() throws IOException
    {
        try
        {
            logger.trace( "START CLOSE: {}", transfer );
            super.close();

            logger.trace( "Delete: {} and its siblings in: {}.", transfer.getPath(), transfer.getLocation() );
            transfer.delete( false );

            Transfer meta = transfer.getSibling( HttpExchangeMetadata.FILE_EXTENSION );
            if ( meta != null && meta.exists() )
            {
                meta.delete( false );
            }
        }
        finally
        {
            logger.trace( "END CLOSE: {}", transfer );
        }
    }
}
