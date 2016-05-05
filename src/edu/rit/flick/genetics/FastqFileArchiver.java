/**
 *  COPYRIGHT (C) 2015 Alex Aiezza. All Rights Reserved.
 *
 *  See the LICENSE for the specific language governing permissions and
 *  limitations under the License provided with this project.
 */
package edu.rit.flick.genetics;

import static java.lang.String.format;

import edu.rit.flick.RegisterFileDeflatorInflator;
import edu.rit.flick.genetics.config.FastqDeflationOptionSet;
import edu.rit.flick.genetics.config.FastqInflationOptionSet;

/**
 * @author Alex Aiezza
 *
 */
@RegisterFileDeflatorInflator (
    deflatedExtension = FastqFileArchiver.DEFAULT_DEFLATED_FASTQ_EXTENSION,
    inflatedExtensions =
{ "fq", "fastq" },
    fileDeflator = FastqFileDeflator.class,
    fileInflator = FastqFileInflator.class,
    fileDeflatorOptionSet = FastqDeflationOptionSet.class,
    fileInflatorOptionSet = FastqInflationOptionSet.class )
public interface FastqFileArchiver extends FastFileArchiver
{
    public static final String SEQUENCE_SCORE_FILE                     = "Scores.txt";
    public static final String SEQUENCE_LENGTH_FILE                    = "Lengths.hcf";
    public static final String COMMENTS_FILE                           = "Comments.txt";

    public static final byte   SEQUENCE_ID_START                       = '@';
    public static final byte   COMMENT_START                           = '+';

    public static final String DEFAULT_DEFLATED_FASTQ_EXTENSION        = ".flickfq";

    public static final String META_COMMENT_SAME_AS_SEQUENCE_ID        = "commentSameAsSequenceId";
    public static final String META_COMMENT_EMPTY                      = "commentEmpty";

    public static final String META_COMMENT_SAME_AS_SEQUENCE_ID_FORMAT = format( "%s=%%b%%n",
        META_COMMENT_SAME_AS_SEQUENCE_ID );
    public static final String META_COMMENT_EMPTY_FORMAT               = format( "%s=%%b%%n",
        META_COMMENT_EMPTY );

    @Override
    public default String getDefaultDeflatedExtension()
    {
        return DEFAULT_DEFLATED_FASTQ_EXTENSION;
    }

    @Override
    public default byte getSequenceIdentifierStart()
    {
        return SEQUENCE_ID_START;
    }
}
