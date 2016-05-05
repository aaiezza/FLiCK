/**
 *  COPYRIGHT (C) 2015 Alex Aiezza. All Rights Reserved.
 *
 *  See the LICENSE for the specific language governing permissions and
 *  limitations under the License provided with this project.
 */
package edu.rit.flick.genetics;

import static java.lang.String.format;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import edu.rit.flick.genetics.util.ByteBufferOutputStream;

/**
 * @author Alex Aiezza
 *
 */
public class FastaFileDeflator extends FastFileDeflator implements FastaFileArchiver
{
    // Output files
    protected ByteBufferOutputStream tandemfile;

    // Tracking fields
    protected boolean                writingTandemRepeat;
    protected boolean                stillInTandemRepeat;

    protected boolean                useCustomFastaSequenceLineSize;
    protected int                    fastaSequenceLineSize = DEFAULT_FASTA_SEQUENCE_LINE_SIZE;

    @Override
    protected void beforeProcessNucleotide()
    {
        try
        {
            switch ( dnaByte )
            {
            // Check for N
            case N:
                if ( writingTandemRepeat )
                {
                    final String tandemPositionStr = Long.toHexString( dnaPosition.longValue() )
                            .toUpperCase() + PIPE;
                    tandemfile.write( tandemPositionStr.getBytes() );
                    writingTandemRepeat = false;
                }
                break;
            // Check for lowercase nucleotides
            case a:
            case c:
            case g:
            case t:
            case u:
                if ( !writingTandemRepeat )
                {
                    writingTandemRepeat = true;
                    final String tandemPositionStr = Long.toHexString( dnaPosition.longValue() )
                            .toUpperCase() + RANGE;

                    tandemfile.write( tandemPositionStr.getBytes() );

                }
                stillInTandemRepeat = true;

                dnaByte = (byte) Character.toUpperCase( (char) dnaByte );
                // Check for uppercase nucleotides
            case A:
            case C:
            case G:
            case T:
            case U:
                if ( writingTandemRepeat && !stillInTandemRepeat )
                {
                    final String tandemPositionStr = Long.toHexString( dnaPosition.longValue() )
                            .toUpperCase() + PIPE;
                    tandemfile.write( tandemPositionStr.getBytes() );
                    writingTandemRepeat = false;
                }

                stillInTandemRepeat = false;
                break;
            default:
                stillInTandemRepeat = Character.isLowerCase( dnaByte );

                if ( writingTandemRepeat && !stillInTandemRepeat )
                {
                    final String tandemPositionStr = Long.toHexString( dnaPosition.longValue() )
                            .toUpperCase() + PIPE;
                    tandemfile.write( tandemPositionStr.getBytes() );
                    writingTandemRepeat = false;
                }
            }
        } catch ( final IOException e )
        {
            e.printStackTrace();
        }
    }

    @Override
    protected void createOutputFiles( final File fastaFile, final String tempOutputDirectory )
            throws IOException
    {
        super.createOutputFiles( fastaFile, tempOutputDirectory );

        tandemfile = ByteBufferOutputStream.map(
            new File( tempOutputDirectory + TANDEM_REPEAT_FILE ), MapMode.READ_WRITE,
            (long) ( fastaFile.length() / ( EXPECTED_COMPRESSION_RATIO * 2 ) ) );
    }


    @Override
    public List<String> getExtensions()
    {
        return Collections.singletonList( FASTA_EXTENSION );
    }

    @Override
    public int getFastaSequenceLineSize()
    {
        return fastaSequenceLineSize;
    }

    @Override
    protected List<Byte> getSequenceEscapes()
    {
        return Arrays.asList( SEQUENCE_ID_START );
    }

    @Override
    protected void initializeDeflator()
    {
        super.initializeDeflator();

        writingTandemRepeat = false;
        stillInTandemRepeat = false;

        useCustomFastaSequenceLineSize = false;
        fastaSequenceLineSize = DEFAULT_FASTA_SEQUENCE_LINE_SIZE;
    }

    @Override
    protected void processProperties() throws IOException
    {
        super.processProperties();

        metafile.write( format( META_FASTA_SEQUENCE_LINE_LENGTH_FORMAT,
            useCustomFastaSequenceLineSize ? fastaSequenceLineSize : seqLineSize ) );
    }

    @Override
    protected void processTail() throws IOException
    {
        int tailCounter = 0;
        while ( compressionCounter-- != 0 )
        {
            final char nucleotide = (char) hyperCompressionBytes[tailCounter];
            stillInTandemRepeat = !Character.isUpperCase( nucleotide );
            if ( !writingTandemRepeat && stillInTandemRepeat )
            {
                writingTandemRepeat = true;
                final String tandemPositionStr = Long.toHexString( dnaPosition.longValue() )
                        .toUpperCase() + RANGE;
                tandemfile.write( tandemPositionStr.getBytes() );
            }

            if ( writingTandemRepeat && ( !stillInTandemRepeat || compressionCounter <= 0 ) )
            {
                final String tandemPositionStr = Long.toHexString( dnaPosition.longValue() )
                        .toUpperCase() + PIPE;
                tandemfile.write( tandemPositionStr.getBytes() );
                writingTandemRepeat = false;
            }

            tailfile.write( nucleotide );
            tailCounter++;
        }

        if ( writingTandemRepeat )
        {
            final String tandemPositionStr = Long.toHexString( dnaPosition.longValue() )
                    .toUpperCase() + PIPE;
            tandemfile.write( tandemPositionStr.getBytes() );
            writingTandemRepeat = false;
        }
    }

    @SuppressWarnings ( "resource" )
    @Override
    public void removeUnusedBufferSpace( final String tmpOutputDirectory )
            throws IOException, InterruptedException
    {
        super.removeUnusedBufferSpace( tmpOutputDirectory );

        final long actualTandemFileSize = tandemfile.position();

        tandemfile.close();

        tandemfile = null;

        final File tandemFile = new File( tmpOutputDirectory + TANDEM_REPEAT_FILE );

        final FileChannel fc = new FileOutputStream( tandemFile, true ).getChannel();
        fc.force( true );
        fc.truncate( actualTandemFileSize ).close();
    }

    @Override
    public void setFastaSequenceLineSize( final int fastaSequenceLineSize )
    {
        useCustomFastaSequenceLineSize = true;
        this.fastaSequenceLineSize = fastaSequenceLineSize;
    }
}
