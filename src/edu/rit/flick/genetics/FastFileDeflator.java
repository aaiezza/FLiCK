/**
 *  COPYRIGHT (C) 2015 Alex Aiezza. All Rights Reserved.
 *
 *  See the LICENSE for the specific language governing permissions and
 *  limitations under the License provided with this project.
 */
package edu.rit.flick.genetics;

import static edu.rit.flick.config.DefaultOptionSet.DELETE_FLAG;
import static edu.rit.flick.config.DefaultOptionSet.VERBOSE_FLAG;
import static java.lang.String.format;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

import org.apache.commons.io.FileUtils;

import com.google.common.collect.BiMap;
import com.google.common.io.Files;

import edu.rit.flick.FileDeflator;
import edu.rit.flick.config.Configuration;
import edu.rit.flick.genetics.util.ByteBufferOutputStream;
import it.unimi.dsi.io.ByteBufferInputStream;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;

/**
 * @author Alex Aiezza
 *
 */
public abstract class FastFileDeflator implements FastFileArchiver, FileDeflator
{
    public final static double          EXPECTED_COMPRESSION_RATIO = 0.25;

    private boolean                     interrupted                = false;

    // Output files
    protected ByteBufferOutputStream    datahcf;
    protected ByteBufferOutputStream    nfile;
    protected BufferedOutputStream      headerfile;
    protected BufferedOutputStream      iupacfile;
    protected BufferedOutputStream      tailfile;
    protected FileWriter                metafile;

    // Input file
    protected ByteBufferInputStream     fastIn;

    // Tracking fields
    private boolean                     writingToNFile             = false;

    protected byte []                   hyperCompressionBytes      = new byte [4];
    protected byte                      dnaByte;

    protected final AtomicInteger       lineType                   = new AtomicInteger(
            SEQUENCE_IDENTIFIER_LINE );
    protected final LongAdder           dnaPosition                = new LongAdder()
    // @formatter:off
    {
        private static final long serialVersionUID = 1L;

        @Override
        public void increment()
        {
            super.increment();
            localSeqLineSize++;
        }
    };
    // @formatter:on
    protected int                       compressionCounter         = 0;

    protected int                       localSeqLineSize           = 0;
    protected int                       seqLineSize                = 0;

    private boolean                     containsCarriageReturns    = false;
    private boolean                     isRNAData                  = false;

    protected final BiMap<String, Byte> byteConverter;

    public FastFileDeflator()
    {
        byteConverter = new ByteConverterBiMapFactory().getByteConverter( 4 );
    }

    protected void afterProcessNucleotides() throws IOException
    {}

    protected void beforeProcessNucleotide()
    {}

    @Override
    public boolean containsCarriageReturns()
    {
        return containsCarriageReturns;
    }

    protected void createOutputFiles( final File fastFile, final String tempOutputDirectory )
            throws IOException
    {
        datahcf = ByteBufferOutputStream.map( new File( tempOutputDirectory + SEQUENCE_DATA_FILE ),
            MapMode.READ_WRITE, (long) ( fastFile.length() * EXPECTED_COMPRESSION_RATIO ) );
        nfile = ByteBufferOutputStream.map( new File( tempOutputDirectory + N_FILE ),
            MapMode.READ_WRITE, (long) ( fastFile.length() * EXPECTED_COMPRESSION_RATIO * 2 ) );
        headerfile = new BufferedOutputStream(
                new FileOutputStream( tempOutputDirectory + SEQUENCE_ID_FILE ), DEFAULT_BUFFER );
        iupacfile = new BufferedOutputStream(
                new FileOutputStream( tempOutputDirectory + IUPAC_CODE_FILE ), DEFAULT_BUFFER );
        tailfile = new BufferedOutputStream(
                new FileOutputStream( tempOutputDirectory + SEQUENCE_TAIL_FILE ), DEFAULT_BUFFER );
        metafile = new FileWriter( new File( tempOutputDirectory + META_FILE ) );

        metafile.write( format( META_FILE_SIZE_FORMAT, fastFile.length() ) );

        final FileInputStream fastFis = new FileInputStream( fastFile );
        fastIn = ByteBufferInputStream.map( fastFis.getChannel() );

        fastFis.close();
    }

    @Override
    public File deflate( final Configuration configuration, final File fileIn, final File fileOut )
    {
        assert fileIn.exists();

        try
        {
            // Deflate to Directory
            final String outputDirectoryPath = fileOut.getPath().replaceAll(
                "." + Files.getFileExtension( fileOut.getPath() ), FLICK_FAST_FILE_TMP_DIR_SUFFIX );

            final File tmpOutputDirectory = new File( outputDirectoryPath );
            if ( tmpOutputDirectory.exists() )
                FileUtils.deleteDirectory( tmpOutputDirectory );
            tmpOutputDirectory.mkdirs();

            final AtomicReference<Thread> cleanHookAtomic = new AtomicReference<Thread>();

            // Deflate Fast file to a temporary directory
            final Thread deflateToDirectoryThread = new Thread( () -> {
                try
                {
                    // Deflate Fast file to a temporary directory
                    deflateToDirectory( fileIn, tmpOutputDirectory );

                    // Remove unused buffer space
                    removeUnusedBufferSpace( outputDirectoryPath );

                    // Compress Directory to a zip file
                    deflateToFile( tmpOutputDirectory, fileOut );

                    Runtime.getRuntime().removeShutdownHook( cleanHookAtomic.get() );
                } catch ( final Exception e )
                {
                    if ( !interrupted )
                        System.err.println( e.getMessage() );
                }
            }, "Default_Deflation_Thread" );

            // Make cleaning hook
            final Thread cleanHook = new Thread( () -> {
                interrupted = true;
                configuration.setFlag( VERBOSE_FLAG, false );
                configuration.setFlag( DELETE_FLAG, false );
                try
                {
                    if ( deflateToDirectoryThread.isAlive() )
                        deflateToDirectoryThread.interrupt();

                    // Remove unused buffer space
                    removeUnusedBufferSpace( outputDirectoryPath );

                    // Delete files that were not able to be processed
                    FileUtils.deleteQuietly( tmpOutputDirectory );
                    System.out.println();
                } catch ( final IOException | InterruptedException e )
                {
                    e.printStackTrace();
                }
            }, "Deflation_Cleaning_Thread" );

            cleanHookAtomic.set( cleanHook );

            Runtime.getRuntime().addShutdownHook( cleanHook );

            deflateToDirectoryThread.start();
            deflateToDirectoryThread.join();

        } catch ( final IOException | InterruptedException e )
        {
            e.printStackTrace();
        }

        return fileOut;
    }

    public void deflateToDirectory( final File fileIn, final File tmpOutputDirectory )
            throws IOException
    {
        createOutputFiles( fileIn, tmpOutputDirectory.getPath() + File.separator );

        initializeDeflator();

        if ( fastIn.available() > 0 )
            dnaByte = (byte) fastIn.read();

        while ( fastIn.available() > 0 )
        {
            switch ( lineType.get() )
            {
            case SEQUENCE_IDENTIFIER_LINE:
                processSequenceIdentifier();
                break;
            case SEQUENCE_LINE:
                processNucleotides();
                afterProcessNucleotides();
                break;
            default:
                processLineType();
            }
            progressLineType();
        }
        processProperties();

        processTail();
    }

    protected void deflateToFile( final File tmpOutputDirectory, final File fileOut )
            throws IOException, ZipException
    {
        if ( fileOut.exists() )
            fileOut.delete();

        final ZipFile flickFile = new ZipFile( fileOut );

        final ZipParameters zParams = new ZipParameters();

        zParams.setIncludeRootFolder( false );
        zParams.setCompressionMethod( Zip4jConstants.COMP_DEFLATE );
        zParams.setCompressionLevel( Zip4jConstants.DEFLATE_LEVEL_NORMAL );

        flickFile.createZipFileFromFolder( tmpOutputDirectory, zParams, false, 0 );

        // Delete files that were just zipped
        FileUtils.deleteQuietly( tmpOutputDirectory );
    }

    @Override
    public BiMap<String, Byte> getByteConverter()
    {
        return byteConverter;
    }

    protected abstract List<Byte> getSequenceEscapes();

    protected void initializeDeflator()
    {
        writingToNFile = false;

        hyperCompressionBytes = new byte [4];
        dnaByte = 0;

        lineType.set( SEQUENCE_IDENTIFIER_LINE );
        dnaPosition.reset();
        compressionCounter = 0;

        localSeqLineSize = 0;
        seqLineSize = 0;

        containsCarriageReturns = false;
        isRNAData = false;
    }

    @Override
    public boolean isRNAData()
    {
        return isRNAData;
    }

    protected void processLineType()
    {}

    protected void processNucleotides() throws IOException
    {
        while ( fastIn.available() > 0 && ( dnaByte = (byte) fastIn.read() ) != -1 )
        {
            beforeProcessNucleotide();

            switch ( dnaByte )
            {
            // Check for N
            case N:
                if ( !writingToNFile )
                {
                    writingToNFile = true;
                    final String nPositionStr = Long.toHexString( dnaPosition.longValue() )
                            .toUpperCase() + RANGE;
                    nfile.write( nPositionStr.getBytes() );
                }
                dnaPosition.increment();
                continue;
            // Check for uppercase nucleotides
            case A:
            case C:
            case G:
            case T:
            case U:

                // Check for U
                if ( dnaByte == U || dnaByte == u )
                {
                    isRNAData = true;
                    dnaByte = T;
                }

                if ( writingToNFile )
                {
                    final String nPositionStr = Long.toHexString( dnaPosition.longValue() )
                            .toUpperCase() + PIPE;
                    nfile.write( nPositionStr.getBytes() );
                    writingToNFile = false;
                }

                processConventionalNucleotide();

                dnaPosition.increment();
                continue;
            case CARRIAGE_RETURN:
                containsCarriageReturns = true;
                continue;
            case NEWLINE:
                if ( seqLineSize < localSeqLineSize )
                    seqLineSize = localSeqLineSize;
                localSeqLineSize = 0;
                continue;
            default:
                if ( getSequenceEscapes().contains( dnaByte ) )
                    return;

                if ( writingToNFile )
                {
                    final String nPositionStr = Long.toHexString( dnaPosition.longValue() )
                            .toUpperCase() + PIPE;
                    nfile.write( nPositionStr.getBytes() );
                    writingToNFile = false;
                }

                // File for IUPAC codes and erroneous characters
                final String iupacBase = format( "%s-%s|",
                    Long.toHexString( dnaPosition.longValue() ), (char) dnaByte + "" );
                iupacfile.write( iupacBase.getBytes() );
                dnaPosition.increment();
            }
        }
    }

    protected void processConventionalNucleotide() throws IOException
    {
        hyperCompressionBytes[compressionCounter] = dnaByte;
        if ( compressionCounter == 3 )
        {
            final String tetramer = new String( hyperCompressionBytes );

            if ( !getByteConverter().containsKey( tetramer ) )
                throw new TetramerNotFoundException( tetramer );
            else datahcf.put( getByteConverter().get( tetramer ).byteValue() );

            compressionCounter = 0;
        } else compressionCounter++;
    }

    protected void processProperties() throws IOException
    {
        metafile.write( format( META_CARRIAGE_RETURN_FORMAT, containsCarriageReturns() ) );
        metafile.write( format( META_RNA_DATA_FORMAT, isRNAData() ) );
    }

    protected String processSequenceIdentifier() throws IOException
    {
        assert dnaByte == getSequenceIdentifierStart();

        // Write Start Location in File
        long hI = 0;
        if ( fastIn.position() > 1 )
            hI = fastIn.position() - ( containsCarriageReturns ? 3 : 2 );

        final StringBuffer seqId = new StringBuffer();
        while ( dnaByte != NEWLINE )
        {
            dnaByte = (byte) fastIn.read();
            if ( dnaByte == CARRIAGE_RETURN )
                containsCarriageReturns = true;
            seqId.append( (char) dnaByte );
        }

        headerfile.write( ( hI + PIPE + seqId.toString() ).getBytes() );

        return seqId.toString();
    }

    protected void processTail() throws IOException
    {
        int tailCounter = 0;
        while ( compressionCounter-- != 0 )
        {
            final char nucleotide = (char) hyperCompressionBytes[tailCounter];
            tailfile.write( nucleotide );
            if ( nucleotide != N && writingToNFile )
            {
                final String nPositionStr = Long.toHexString( dnaPosition.longValue() )
                        .toUpperCase() + PIPE;
                nfile.write( nPositionStr.getBytes() );
                writingToNFile = false;
            }
            dnaPosition.increment();
            tailCounter++;
        }

        if ( writingToNFile )
        {
            final String nPositionStr = Long.toHexString( dnaPosition.longValue() ).toUpperCase() +
                    PIPE;
            nfile.write( nPositionStr.getBytes() );
            writingToNFile = false;
        }
    }

    protected void progressLineType()
    {
        if ( lineType.get() == SEQUENCE_IDENTIFIER_LINE )
            lineType.set( SEQUENCE_LINE );
        else lineType.set( SEQUENCE_IDENTIFIER_LINE );
    }

    @SuppressWarnings ( "resource" )
    protected void removeUnusedBufferSpace( final String tmpOutputDirectory )
            throws IOException, InterruptedException
    {
        final long actualDataHcfFileSize = datahcf.position();
        final long actualNFileSize = nfile.position();

        fastIn.close();
        datahcf.close();
        headerfile.close();
        nfile.close();
        iupacfile.close();
        tailfile.close();
        metafile.close();

        fastIn = null;
        datahcf = null;
        nfile = null;

        // Give the last method a moment to garbage collect
        // System.gc();
        // Thread.sleep( 1000 );

        final File dataFile = new File( tmpOutputDirectory + SEQUENCE_DATA_FILE );
        final File nFile = new File( tmpOutputDirectory + N_FILE );

        // Remove unused buffer space
        FileChannel fc = new FileOutputStream( dataFile, true ).getChannel();
        fc.force( true );
        fc.truncate( actualDataHcfFileSize ).close();

        fc = new FileOutputStream( nFile, true ).getChannel();
        fc.force( true );
        fc.truncate( actualNFileSize ).close();
    }
}
