/**
 *  COPYRIGHT (C) 2015 Alex Aiezza. All Rights Reserved.
 *
 *  See the LICENSE for the specific language governing permissions and
 *  limitations under the License provided with this project.
 */
package edu.rit.flick.genetics;

import static java.lang.String.format;

/**
 * @author Alex Aiezza
 *
 */
public interface FastaFileArchiver extends FastFileArchiver
{
    public final static int    DEFAULT_FASTA_SEQUENCE_LINE_SIZE       = 80;

    public final static String TANDEM_REPEAT_FILE                     = "Tandems.txt";

    public final static String META_FASTA_SEQUENCE_LINE_LENGTH        = "seqLineLength";

    public final static String META_FASTA_SEQUENCE_LINE_LENGTH_FORMAT = format( "%s=%%d%%n",
        META_FASTA_SEQUENCE_LINE_LENGTH );

    public static final byte   SEQUENCE_ID_START                      = '>';

    public static final String FASTA_EXTENSION                        = ".fna";

    public static final String DEFAULT_DEFLATED_FASTA_EXTENSION       = ".flickfa";

    @Override
    public default String getDefaultDeflatedExtension()
    {
        return DEFAULT_DEFLATED_FASTA_EXTENSION;
    }

    public int getFastaSequenceLineSize();

    @Override
    public default byte getSequenceIdentifierStart()
    {
        return SEQUENCE_ID_START;
    }

    public void setFastaSequenceLineSize( final int fastaSequenceLineSize );
}
