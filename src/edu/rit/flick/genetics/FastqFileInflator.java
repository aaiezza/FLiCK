/**
 *  COPYRIGHT (C) 2015 Alex Aiezza. All Rights Reserved.
 *
 *  See the LICENSE for the specific language governing permissions and
 *  limitations under the License provided with this project.
 */
package edu.rit.flick.genetics;

import static org.apache.commons.io.FileUtils.getFile;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import it.unimi.dsi.io.ByteBufferInputStream;
import it.unimi.dsi.lang.MutableString;

/**
 * @author Alex Aiezza
 *
 */
public class FastqFileInflator extends FastFileInflator implements FastqFileArchiver
{
    // Input Files
    protected BufferedInputStream   lengthfile;
    protected Scanner               commentsfile;
    protected ByteBufferInputStream scorefile;

    // Tracking fields
    private short                   length;
    private final MutableString     previousHeader = new MutableString();

    protected boolean               commentTheSameAsSequenceId;
    protected boolean               commentEmpty;

    @Override
    protected void beforeSequence() throws IOException
    {
        previousHeader.replace(
            header.substring( header.indexOf( (char) getSequenceIdentifierStart() ) + 1 ) );
        getNextLength();
    }

    @Override
    protected void close() throws IOException, InterruptedException
    {
        if ( lengthfile != null )
            lengthfile.close();
        if ( commentsfile != null )
            commentsfile.close();
        if ( scorefile != null )
            scorefile.close();

        scorefile = null;

        super.close();
    }


    @Override
    protected void createOutputFiles( final String tempOutputDirectory, final File fastFile )
            throws IOException
    {
        super.createOutputFiles( tempOutputDirectory, fastFile );

        lengthfile = new BufferedInputStream(
                new FileInputStream( getFile( tempOutputDirectory, SEQUENCE_LENGTH_FILE ) ),
                DEFAULT_BUFFER )
        {
            @Override
            public synchronized int read() throws IOException
            {
                return super.read() << 8 | super.read() & 0x00ff;
            }
        };
        commentsfile = new Scanner( getFile( tempOutputDirectory, COMMENTS_FILE ) );

        final FileInputStream scoreFis = new FileInputStream(
                getFile( tempOutputDirectory, SEQUENCE_SCORE_FILE ) );
        scorefile = ByteBufferInputStream.map( scoreFis.getChannel() );

        scoreFis.close();
    }

    @Override
    public List<String> getExtensions()
    {
        return Collections.singletonList( DEFAULT_DEFLATED_FASTQ_EXTENSION );
    }

    public void getNextLength() throws IOException
    {
        if ( lengthfile.available() > 0 )
            length = (short) lengthfile.read();
    }

    @Override
    protected void initializeInflator()
    {
        super.initializeInflator();

        previousHeader.replace( "" );
    }

    @Override
    protected void parseProperties()
    {
        super.parseProperties();
        commentTheSameAsSequenceId = Boolean
                .parseBoolean( (String) metafile.get( META_COMMENT_SAME_AS_SEQUENCE_ID ) );
        commentEmpty = Boolean.parseBoolean( (String) metafile.get( META_COMMENT_EMPTY ) );
    }

    @Override
    protected void processSequence()
    {
        if ( fastOut.hasRemaining() && seqDnaPosition.get() == length )
            try
            {
                writeComment();
                writeQualityScores();
                writeNextHeader();
            } catch ( final IOException e )
            {
                e.printStackTrace();
            }
        super.processSequence();
    }

    protected void writeComment() throws IOException
    {
        fastOut.put( NEWLINE );
        // Write comment
        if ( commentsfile.hasNextLine() )
        {
            fastOut.put( COMMENT_START );
            fastOut.put( commentsfile.nextLine().getBytes() );
            fastOut.put( NEWLINE );
        } else if ( commentEmpty )
        {
            fastOut.put( COMMENT_START );
            fastOut.put( NEWLINE );
        } else if ( commentTheSameAsSequenceId )
        {
            fastOut.put( COMMENT_START );
            fastOut.put( previousHeader.toString().getBytes() );
        }
    }

    protected void writeQualityScores() throws IOException
    {
        // Write quality scores
        if ( scorefile.available() > 0 )
        {
            final byte [] scores = new byte [length];
            scorefile.read( scores );
            fastOut.put( scores );
        }
    }
}
