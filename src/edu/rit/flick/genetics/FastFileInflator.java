/**
 *  COPYRIGHT (C) 2015 Alex Aiezza. All Rights Reserved.
 *
 *  See the LICENSE for the specific language governing permissions and
 *  limitations under the License provided with this project.
 */
package edu.rit.flick.genetics;

import static org.apache.commons.io.FileUtils.getFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel.MapMode;
import java.util.Properties;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

import org.apache.commons.io.FileUtils;

import com.google.common.collect.BiMap;
import com.google.common.io.Files;

import edu.rit.flick.FileInflator;
import edu.rit.flick.config.Configuration;
import edu.rit.util.ByteBufferOutputStream;
import it.unimi.dsi.io.ByteBufferInputStream;
import it.unimi.dsi.lang.MutableString;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

/**
 * @author Alex Aiezza
 *
 */
public abstract class FastFileInflator implements FastFileArchiver, FileInflator
{
    // Input files
    protected ByteBufferInputStream     datahcf;
    protected Scanner                   nfile;
    protected Scanner                   headerfile;
    protected Scanner                   iupacfile;
    protected Scanner                   tailfile;
    protected Properties                metafile;

    // Output file
    protected ByteBufferOutputStream    fastOut;

    // Tracking fields
    protected final MutableString       header                  = new MutableString();
    protected long                      headerPosition;

    protected long                      nStart                  = -1, consecNs = -1;

    protected long                      iupacPosition           = -1;
    protected char                      iupacBase               = 0x0;

    protected final MutableString       nucleotides             = new MutableString();
    // @formatter:off
    protected final LongAdder           dnaPosition             = new LongAdder()
    {
        private static final long serialVersionUID = 1L;

        @Override
        public void increment()
        {
            super.increment();

            // Check for headerPosition index
            if ( fastOut.position() == headerPosition )
                writeNextHeader();
            else afterWriteNucleotide();

            processSequence();
        }

    };
    // @formatter:on
    protected final AtomicLong          seqDnaPosition          = new AtomicLong();

    private long                        fastFileSize;
    private boolean                     containsCarriageReturns = false;
    private boolean                     isRNAData               = false;

    protected final BiMap<String, Byte> byteConverter;

    public FastFileInflator()
    {
        byteConverter = new ByteConverterBiMapFactory().getByteConverter( 4 );
    }

    protected void afterWriteNucleotide()
    {
        seqDnaPosition.incrementAndGet();
    }

    protected void beforeSequence() throws IOException
    {}

    protected void close() throws IOException, InterruptedException
    {
        fastOut.close();
        iupacfile.close();
        datahcf.close();
        headerfile.close();
        nfile.close();
        tailfile.close();

        datahcf = null;
        fastOut = null;

        // Give the last method a moment to garbage collect
        // System.gc();
        // Thread.sleep( 1000 );
    }

    @Override
    public boolean containsCarriageReturns()
    {
        return containsCarriageReturns;
    }

    @SuppressWarnings ( "resource" )
    protected void createOutputFiles( final String tempOutputDirectory, final File fastFile )
            throws IOException
    {
        datahcf = ByteBufferInputStream
                .map( new FileInputStream( getFile( tempOutputDirectory, SEQUENCE_DATA_FILE ) )
                        .getChannel() );
        nfile = new Scanner( getFile( tempOutputDirectory, N_FILE ) ).useDelimiter( "\\" + PIPE );
        headerfile = new Scanner( getFile( tempOutputDirectory, SEQUENCE_ID_FILE ) )
                .useDelimiter( "" + NEWLINE );
        iupacfile = new Scanner( getFile( tempOutputDirectory, IUPAC_CODE_FILE ) )
                .useDelimiter( "\\" + PIPE );
        tailfile = new Scanner( getFile( tempOutputDirectory, SEQUENCE_TAIL_FILE ) );
        metafile = getProperties( getFile( tempOutputDirectory, META_FILE ) );

        parseProperties();

        fastOut = ByteBufferOutputStream.map( fastFile, MapMode.READ_WRITE, fastFileSize );
    }

    @Override
    public BiMap<String, Byte> getByteConverter()
    {
        return byteConverter;
    }

    protected void getNextIupacBase()
    {
        final StringTokenizer iupacs;
        if ( iupacfile.hasNext() )
        {
            iupacs = new StringTokenizer( iupacfile.next(), RANGE );
            iupacPosition = Long.parseLong( iupacs.nextToken(), 16 );
            iupacBase = iupacs.nextToken().charAt( 0 );
        }
        // Check for IUPAC index
        if ( fastOut.position() > 0 && dnaPosition.longValue() == iupacPosition )
            writeNextIupacBase();
    }

    protected void getNextNs()
    {
        final long nEnd;
        final StringTokenizer ns;
        if ( nfile.hasNext() )
        {
            ns = new StringTokenizer( nfile.next(), RANGE );
            nStart = Long.parseLong( ns.nextToken(), 16 );
            nEnd = Long.parseLong( ns.nextToken(), 16 );
            consecNs = nEnd - nStart;
        }
        // Check for nStart index
        if ( fastOut.position() > 0 && dnaPosition.longValue() == nStart )
            writeNextNs();
    }

    protected boolean getNextNucleotides()
    {
        if ( datahcf.available() > 0 )
        {
            nucleotides.replace( byteConverter.inverse().get( (byte) datahcf.read() ) );
            return true;
        }
        return false;
    }

    private void getNextSequenceIdentifier()
    {
        if ( headerfile.hasNext() )
        {
            final String headerInfo = headerfile.nextLine() +
                    ( containsCarriageReturns() ? (char) CARRIAGE_RETURN + "" + (char) NEWLINE
                                                : (char) NEWLINE );
            final String [] headInd = headerInfo.split( "\\" + PIPE, 2 );
            headerPosition = Long.parseLong( headInd[0] );
            header.replace(
                ( containsCarriageReturns() ? (char) CARRIAGE_RETURN + "" + (char) NEWLINE
                                            : (char) NEWLINE ) +
                        "" + (char) getSequenceIdentifierStart() + headInd[1] );
        } else header.replace( "" );
    }

    protected Properties getProperties( final File propertiesFile ) throws IOException
    {
        final Properties props = new Properties();

        final InputStream in = new FileInputStream( propertiesFile );
        props.load( in );

        in.close();

        return props;
    }

    @Override
    public File inflate( final Configuration configuration, final File fileIn, final File fileOut )
    {
        assert fileIn.exists();

        try
        {
            // Inflate to Directory
            final String outputDirectoryPath = fileOut.getPath().replaceAll(
                "." + Files.getFileExtension( fileOut.getPath() ), FLICK_FAST_FILE_TMP_DIR_SUFFIX );

            final File tmpOutputDirectory = new File( outputDirectoryPath );
            if ( tmpOutputDirectory.exists() )
                FileUtils.deleteDirectory( tmpOutputDirectory );

            // Make cleaning hook
            final Thread cleanHook = new Thread( () -> {
                try
                {
                    // Clean up IO
                    close();

                    // Give the last method a moment to garbage collect
                    System.gc();
                    // Thread.sleep( 1000 );

                    // Clean up temporary directory
                    FileUtils.deleteDirectory( tmpOutputDirectory );
                } catch ( final IOException | InterruptedException e )
                {
                    e.printStackTrace();
                }
            } );

            Runtime.getRuntime().addShutdownHook( cleanHook );

            // Inflate Fast file to a temporary directory
            inflateFromFile( fileIn, tmpOutputDirectory );

            // Inflate Directory to a zip file
            inflateFromDirectory( tmpOutputDirectory, fileOut );

            cleanHook.start();
            cleanHook.join();

            Runtime.getRuntime().removeShutdownHook( cleanHook );

        } catch ( IOException | InterruptedException e )
        {
            e.printStackTrace();
        }

        return fileOut;
    }

    public File inflateFromDirectory( final File tmpOutputDirectory, final File fileOut )
            throws IOException
    {
        createOutputFiles( tmpOutputDirectory.getPath() + File.separator, fileOut );

        initializeInflator();

        while ( getNextNucleotides() )
            // Write sequence
            nucleotides.chars().mapToObj( base -> (byte) base ).forEach( base -> {

                // Check for headerPosition index
                if ( fastOut.position() == headerPosition )
                    writeNextHeader();

                processSequence();

                writeNucleotide( base );
            } );

        // Write tail
        writeTail();

        return fileOut;
    }

    protected File inflateFromFile( final File fileIn, final File tmpOutputDirectory )
    {
        tmpOutputDirectory.mkdirs();
        ZipFile zipFile;
        try
        {
            zipFile = new ZipFile( fileIn );

            zipFile.extractAll( tmpOutputDirectory.getPath() );
        } catch ( final ZipException e )
        {
            e.printStackTrace();
        }

        return tmpOutputDirectory;
    }

    protected void initializeInflator()
    {
        header.replace( "" );

        nStart = -1;
        consecNs = -1;

        iupacPosition = -1;
        iupacBase = 0x0;

        nucleotides.replace( "" );
        dnaPosition.reset();
        seqDnaPosition.set( 0 );

        // Get first sequence identifier
        if ( headerfile.hasNext() )
        {
            final String headerInfo = headerfile.nextLine() +
                    ( containsCarriageReturns() ? (char) CARRIAGE_RETURN + "" + (char) NEWLINE
                                                : (char) NEWLINE );
            final String [] headInd = headerInfo.split( "\\" + PIPE, 2 );
            headerPosition = Long.parseLong( headInd[0] );
            header.replace( (char) getSequenceIdentifierStart() + headInd[1] );
        }

        // Get first N
        getNextNs();

        // Get first IUPAC Base
        getNextIupacBase();
    }

    @Override
    public boolean isRNAData()
    {
        return isRNAData;
    }

    protected void parseProperties()
    {
        fastFileSize = Long.parseLong( (String) metafile.get( META_FILE_SIZE ) );
        containsCarriageReturns = Boolean
                .parseBoolean( (String) metafile.get( META_CARRIAGE_RETURN ) );
        isRNAData = Boolean.parseBoolean( (String) metafile.get( META_RNA_DATA ) );
    }

    protected void processSequence()
    {
        // Check for nStart index
        if ( dnaPosition.longValue() == nStart )
            writeNextNs();

        // Check for IUPAC index
        if ( dnaPosition.longValue() == iupacPosition )
            writeNextIupacBase();
    }

    protected void writeNewline() throws IOException
    {
        if ( containsCarriageReturns() )
            fastOut.write( CARRIAGE_RETURN );
        fastOut.write( NEWLINE );
    }

    protected final void writeNextHeader()
    {
        try
        {
            // Write header
            beforeSequence();
            fastOut.put( header.toString().getBytes() );
            getNextSequenceIdentifier();
            seqDnaPosition.set( 0 );
        } catch ( final IOException e )
        {
            e.printStackTrace();
        }
    }

    protected void writeNextIupacBase()
    {
        writeNucleotide( (byte) iupacBase );
        getNextIupacBase();
        // Check for nStart index
        if ( dnaPosition.longValue() == nStart )
            writeNextNs();
    }

    protected void writeNextNs()
    {
        for ( int n = 0; n < consecNs; writeNucleotide( N ), n++ )
            ;
        getNextNs();
        // Check for IUPAC index
        if ( dnaPosition.longValue() == iupacPosition )
            writeNextIupacBase();
    }

    protected void writeNucleotide( final byte base )
    {
        try
        {
            final byte nucleotide = isRNAData() && base == T ? U : base;
            fastOut.write( nucleotide );
        } catch ( final IOException e )
        {
            e.printStackTrace();
        }
        dnaPosition.increment();
    }

    protected void writeTail() throws IOException
    {
        if ( tailfile.hasNext() )
        {
            final String tail = tailfile.next();
            tail.chars().mapToObj( base -> (byte) base ).forEach( base -> {
                writeNucleotide( base );
            } );
        } else processSequence();
        if ( fastOut.available() > 0 )
            writeNewline();
    }
}
