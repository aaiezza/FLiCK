/**
 *  COPYRIGHT (C) 2015 Alex Aiezza. All Rights Reserved.
 *
 *  See the LICENSE for the specific language governing permissions and
 *  limitations under the License provided with this project.
 */
package edu.rit.flick.genetics;

import static java.lang.String.format;

import java.io.File;

import com.google.common.collect.BiMap;

import edu.rit.flick.FileArchiver;

/**
 * @author Alex Aiezza
 *
 */
public interface FastFileArchiver extends FileArchiver
{
    final static byte          A                               = 'A', C = 'C', G = 'G', T = 'T',
            U = 'U', a = 'a', c = 'c', g = 'g', t = 't', u = 'u', NEWLINE = '\n',
            CARRIAGE_RETURN = '\r', N = 'N';

    final static String        PIPE                            = "|", RANGE = "-";

    final static int           DEFAULT_BUFFER                  = 1024;

    final static String        GENZIP_FAST_FILE_TMP_DIR_SUFFIX = "~flick\\" + File.separator;

    /*
     * Byte iteration file location
     */
    public final static int    SEQUENCE_IDENTIFIER_LINE        = 1, SEQUENCE_LINE = 2;

    /*
     * Storage Files
     */
    final static String        SEQUENCE_ID_FILE                = "Header.txt";
    final static String        SEQUENCE_DATA_FILE              = "Data.hcf";
    final static String        N_FILE                          = "NFile.txt";
    final static String        IUPAC_CODE_FILE                 = "Iupac.txt";
    final static String        SEQUENCE_TAIL_FILE              = "Tail.txt";
    final static String        META_FILE                       = "Meta.txt";

    public final static String META_FILE_SIZE                  = "uncompressedSize";
    public final static String META_TAIL_NUCLEOTIDES           = "tailNucleotides";
    public final static String META_CARRIAGE_RETURN            = "carriageReturn";
    public final static String META_RNA_DATA                   = "rnaData";

    final static String        META_FILE_SIZE_FORMAT           = format( "%s=%%d%%n",
        META_FILE_SIZE ),
        META_CARRIAGE_RETURN_FORMAT = format( "%s=%%b%%n", META_CARRIAGE_RETURN ),
        META_RNA_DATA_FORMAT = format( "%s=%%b%%n", META_RNA_DATA ),
        META_TAIL_NUCLEOTIDES_FORMAT = format( "%s=%%s%%n", META_TAIL_NUCLEOTIDES );

    boolean containsCarriageReturns();

    public BiMap<String, Byte> getByteConverter();

    public byte getSequenceIdentifierStart();

    boolean isRNAData();
}
