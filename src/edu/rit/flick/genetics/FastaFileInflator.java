/**
 *  COPYRIGHT (C) 2015 Alex Aiezza. All Rights Reserved.
 *
 *  See the LICENSE for the specific language governing permissions and
 *  limitations under the License provided with this project.
 */
package edu.rit.flick.genetics;

import static org.apache.commons.io.FileUtils.getFile;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.util.StringTokenizer;

import edu.rit.flick.genetics.util.HexPrinter;

/**
 * @author Alex Aiezza
 *
 */
public class FastaFileInflator extends FastFileInflator implements FastaFileArchiver
{
    // Input files
    protected Scanner tandemFile;

    // Tracking fields
    protected boolean useCustomSequenceLineSize;
    protected int     fastaSequenceLineSize = DEFAULT_FASTA_SEQUENCE_LINE_SIZE;

    protected long    tandemStart           = -1, tandemEnd = -1;
    protected boolean inTandemRepeat        = false;

    @Override
    protected void afterWriteNucleotide()
    {
        super.afterWriteNucleotide();
        try
        {
            if ( seqDnaPosition.get() % fastaSequenceLineSize == 0 &&
                    fastOut.position() != headerPosition )
                writeNewline();
        } catch ( final IOException e )
        {
            e.printStackTrace();
        }
    }

    @Override
    protected void close() throws IOException, InterruptedException
    {
        super.close();
        if ( tandemFile != null )
            tandemFile.close();
    }

    @SuppressWarnings ( "resource" )
    @Override
    protected void createOutputFiles( final String tempOutputDirectory, final File fastFile )
            throws IOException
    {
        useCustomSequenceLineSize = false;
        super.createOutputFiles( tempOutputDirectory, fastFile );

        tandemFile = new Scanner( getFile( tempOutputDirectory, TANDEM_REPEAT_FILE ) )
                .useDelimiter( "\\" + PIPE );
    }

    @Override
    public int getFastaSequenceLineSize()
    {
        return fastaSequenceLineSize;
    }

    private void getNextTandemRepeatChunk()
    {
        inTandemRepeat = false;
        if ( tandemFile.hasNext() )
        {
            final StringTokenizer tandems = new StringTokenizer( tandemFile.next(), RANGE );
            tandemStart = Long.parseLong( tandems.nextToken(), HexPrinter.RADIX );
            tandemEnd = Long.parseLong( tandems.nextToken(), HexPrinter.RADIX );
        }
    }

    @Override
    protected void initializeInflator()
    {
        super.initializeInflator();

        tandemStart = -1;
        tandemEnd = -1;
        inTandemRepeat = false;

        // Get first tandem repeat chunk
        getNextTandemRepeatChunk();
    }

    @Override
    protected void parseProperties()
    {
        super.parseProperties();
        fastaSequenceLineSize = useCustomSequenceLineSize ? fastaSequenceLineSize : Integer
                .parseInt( (String) metafile.get( META_FASTA_SEQUENCE_LINE_LENGTH ) );
    }

    @Override
    protected void processSequence()
    {
        if ( dnaPosition.longValue() == tandemStart )
            inTandemRepeat = true;

        // Wrap up a tandem repeat section
        if ( dnaPosition.longValue() == tandemEnd )
        {
            inTandemRepeat = false;
            // Get next tandemRepeatSection
            getNextTandemRepeatChunk();
        }
        super.processSequence();
    }

    @Override
    public void setFastaSequenceLineSize( final int fastaSequenceLineSize )
    {
        useCustomSequenceLineSize = true;
        this.fastaSequenceLineSize = fastaSequenceLineSize;
    }

    @Override
    protected void writeNucleotide( final byte base )
    {
        byte nucleotide = (byte) ( inTandemRepeat ? Character.toLowerCase( base ) : base );
        nucleotide = isRNAData() && nucleotide == t ? u : nucleotide;
        super.writeNucleotide( nucleotide );
    }
}
