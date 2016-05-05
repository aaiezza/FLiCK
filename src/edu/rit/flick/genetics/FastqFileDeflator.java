/**
 *  COPYRIGHT (C) 2015 Alex Aiezza. All Rights Reserved.
 *
 *  See the LICENSE for the specific language governing permissions and
 *  limitations under the License provided with this project.
 */
package edu.rit.flick.genetics;

import static java.lang.String.format;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.Arrays;
import java.util.List;

import edu.rit.flick.genetics.util.ByteBufferOutputStream;
import edu.rit.flick.genetics.util.HexPrinter;

/**
 * @author Alex Aiezza
 *
 */
public class FastqFileDeflator extends FastFileDeflator implements FastqFileArchiver
{
    private final static int       COMMENT_LINE               = 3, SCORES_LINE = 0;

    // Output files
    private ByteBufferOutputStream scorefile;
    private BufferedOutputStream   lengthfile;
    private BufferedOutputStream   commentsfile;

    // Tracking fields
    private boolean                commentTheSameAsSequenceId = false;
    private boolean                commentEmpty               = false;
    private boolean                encounteredFirstComment    = false;

    private String                 sequenceId;

    @Override
    protected void afterProcessNucleotides() throws IOException
    {
        HexPrinter.shortToFile( (short) seqLineSize, lengthfile );
    }

    @Override
    protected void createOutputFiles( final File fastqFile, final String tempOutputDirectory )
            throws IOException
    {
        super.createOutputFiles( fastqFile, tempOutputDirectory );

        scorefile = ByteBufferOutputStream.map(
            new File( tempOutputDirectory + SEQUENCE_SCORE_FILE ), MapMode.READ_WRITE,
            (long) ( fastqFile.length() * 2 * EXPECTED_COMPRESSION_RATIO ) );
        lengthfile = new BufferedOutputStream(
                new FileOutputStream( tempOutputDirectory + SEQUENCE_LENGTH_FILE ),
                DEFAULT_BUFFER );
        commentsfile = new BufferedOutputStream(
                new FileOutputStream( tempOutputDirectory + COMMENTS_FILE ), DEFAULT_BUFFER );
    }

    @Override
    protected List<Byte> getSequenceEscapes()
    {
        return Arrays.asList( SEQUENCE_ID_START, COMMENT_START );
    }

    @Override
    protected void initializeDeflator()
    {
        super.initializeDeflator();

        encounteredFirstComment = false;
    }

    @Override
    protected void processLineType()
    {
        try
        {
            switch ( lineType.get() )
            {
            case COMMENT_LINE:
                assert dnaByte == COMMENT_START;

                final StringBuilder comment = new StringBuilder();
                while ( fastIn.available() > 0 && ( dnaByte = (byte) fastIn.read() ) != -1 )
                {
                    comment.append( (char) dnaByte );
                    if ( dnaByte == NEWLINE )
                        break;
                }

                if ( !encounteredFirstComment )
                {
                    commentTheSameAsSequenceId = sequenceId.equals( comment.toString() );
                    commentEmpty = comment.length() <= 1;
                    metafile.write( format( META_COMMENT_SAME_AS_SEQUENCE_ID_FORMAT,
                        commentTheSameAsSequenceId ) );
                    metafile.write( format( META_COMMENT_EMPTY_FORMAT, commentEmpty ) );
                    encounteredFirstComment = true;
                }

                if ( !commentTheSameAsSequenceId && !commentEmpty )
                    commentsfile.write( comment.toString().getBytes() );

                break;
            case SCORES_LINE:
                final StringBuilder scoreLine = new StringBuilder();
                while ( fastIn.available() > 0 && ( dnaByte = (byte) fastIn.read() ) != -1 &&
                        dnaByte != NEWLINE )
                    scoreLine.append( (char) dnaByte );

                assert scoreLine.length() == seqLineSize;

                scorefile.write( scoreLine.toString().getBytes() );

                dnaByte = (byte) fastIn.read();
                break;
            }
        } catch ( final IOException e )
        {
            e.printStackTrace();
        }
    }

    @Override
    protected String processSequenceIdentifier() throws IOException
    {
        sequenceId = super.processSequenceIdentifier();
        return sequenceId;
    }

    @Override
    protected void progressLineType()
    {
        if ( lineType.get() == SEQUENCE_LINE )
            lineType.set( COMMENT_LINE );
        else if ( lineType.get() == COMMENT_LINE )
            lineType.set( SCORES_LINE );
        else super.progressLineType();
    }

    @SuppressWarnings ( "resource" )
    @Override
    protected void removeUnusedBufferSpace( final String tmpOutputDirectory )
            throws IOException, InterruptedException
    {
        super.removeUnusedBufferSpace( tmpOutputDirectory );

        final long actualScoreFileSize = scorefile.position();

        scorefile.close();
        commentsfile.close();
        lengthfile.close();

        scorefile = null;

        // Give the last method a moment to garbage collect
        System.gc();
        Thread.sleep( 1000 );

        final File scoreFile = new File( tmpOutputDirectory + SEQUENCE_SCORE_FILE );

        final FileChannel fc = new FileOutputStream( scoreFile, true ).getChannel();
        fc.force( true );
        fc.truncate( actualScoreFileSize ).close();
    }
}
