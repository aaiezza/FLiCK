/**
 *  COPYRIGHT (C) 2015 Alex Aiezza. All Rights Reserved.
 *
 *  See the LICENSE for the specific language governing permissions and
 *  limitations under the License provided with this project.
 */
package edu.rit.flick;

import static edu.rit.flick.config.DefaultOptionSet.ARCHIVE_MODE;
import static edu.rit.flick.config.DefaultOptionSet.DEFLATION_ARCHIVE_MODE;
import static edu.rit.flick.config.DefaultOptionSet.DELETE_FLAG;
import static edu.rit.flick.config.DefaultOptionSet.INFLATION_ARCHIVE_MODE;
import static edu.rit.flick.config.DefaultOptionSet.OUTPUT_PATH;
import static edu.rit.flick.config.DefaultOptionSet.VERBOSE_FLAG;
import static edu.rit.flick.config.DeflationOptionSet.NO_ZIP_FLAG;
import static edu.rit.flick.config.InflationOptionSet.KEEP_ZIPPED_FLAG;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;

import com.google.common.io.Files;

import edu.rit.flick.config.Configuration;
import net.lingala.zip4j.core.HeaderReader;
import net.lingala.zip4j.model.CentralDirectory;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipModel;
import net.lingala.zip4j.util.InternalZipConstants;

/**
 * @author Alex Aiezza
 *
 */
public class DefaultFlickFile extends AbstractFlickFile
{
    public static final String  DEFAULT_DEFLATED_EXTENSION               = ".flick";

    /*
     * INFO, WARNING & ERROR messages
     */

    private static final String VERBOSE_COMPRESSION_INFO_FORMAT          = "Compressed '%s' in %.2f sec (deflated %.2f%%)%n";

    private static final String VERBOSE_DECOMPRESSION_INFO_FORMAT        = "Decompressed '%s' in %.2f sec (inflated %.2f%%)%n";

    private static final String FILE_COULD_NOT_BE_DELETED_WARNING_FORMAT = "File '%s' could not be deleted.%n";

    /**
     * @param configuration
     * @throws Exception
     */
    public DefaultFlickFile( final Configuration configuration ) throws Exception
    {
        super( configuration );
    }

    private File archiveFile( final File file, final boolean onlyFile )
    {
        double time = 0;
        File archivedFile = null;

        if ( !file.isDirectory() )
        {
            final String fileExt = "." + Files.getFileExtension( file.getPath() );

            if ( configuration.getFlag( ARCHIVE_MODE ) == DEFLATION_ARCHIVE_MODE &&
                    !configuration.getFlag( NO_ZIP_FLAG ) )
            {
                final FileDeflator fileDeflator = REGISTRY.getFileDeflator( fileExt );
                if ( fileDeflator != null )
                {
                    final long t1 = System.currentTimeMillis();
                    if ( onlyFile && !configuration.optionIsNull( OUTPUT_PATH ) )
                        archivedFile = fileDeflator.deflate( configuration, file,
                            new File( (String) configuration.getOption( OUTPUT_PATH ) ) );
                    else archivedFile = fileDeflator.deflate( configuration, file );
                    time = ( System.currentTimeMillis() - t1 ) / 1000d;
                }
            } else if ( configuration.getFlag( ARCHIVE_MODE ) == INFLATION_ARCHIVE_MODE &&
                    !configuration.getFlag( KEEP_ZIPPED_FLAG ) )
            {
                final FileInflator fileInflator = REGISTRY.getFileInflator( fileExt );
                if ( fileInflator != null )
                {
                    final long t1 = System.currentTimeMillis();
                    if ( onlyFile && !configuration.optionIsNull( OUTPUT_PATH ) )
                        archivedFile = fileInflator.inflate( configuration, file,
                            new File( (String) configuration.getOption( OUTPUT_PATH ) ) );
                    else archivedFile = fileInflator.inflate( configuration, file );
                    time = ( System.currentTimeMillis() - t1 ) / 1000d;
                }
            }

            if ( archivedFile != null )
                if ( configuration.getFlag( VERBOSE_FLAG ) )
                {
                    if ( configuration.getFlag( ARCHIVE_MODE ) == DEFLATION_ARCHIVE_MODE )
                    {
                        // Get the percent deflation on the archived file
                        final double percDeflated = 100 *
                                ( (double) file.length() - (double) archivedFile.length() ) /
                                file.length();

                        System.out.printf( VERBOSE_COMPRESSION_INFO_FORMAT, file.getName(), time,
                            percDeflated );
                    }
                    if ( configuration.getFlag( ARCHIVE_MODE ) == INFLATION_ARCHIVE_MODE )
                    {
                        // Get the percent deflation on the compressed file
                        final double percDeflated = 100 *
                                (double) FileUtils.sizeOf( archivedFile ) /
                                FileUtils.sizeOf( file );

                        System.out.printf( VERBOSE_DECOMPRESSION_INFO_FORMAT, file.getName(), time,
                            percDeflated );
                    }
                }
        }

        return archivedFile;
    }

    @Override
    public File deflate( final Configuration configuration, final File fileIn, final File fileOut )
    {
        try
        {
            final List<File> compressedFiles = new ArrayList<File>();

            boolean compressSingleFile = false;

            final long inputPathSize = FileUtils.sizeOf( fileIn );
            final long t0 = System.currentTimeMillis();
            if ( fileIn.isDirectory() )
            {
                // Traverse directory and look for files to compress
                for ( final File file : Files.fileTreeTraverser().breadthFirstTraversal( fileIn )
                        .filter( file -> !file.isDirectory() ) )
                {
                    final File compressedFile = archiveFile( file, false );

                    if ( compressedFile != null )
                        compressedFiles.add( compressedFile );
                }

                flickFile.addFolder( fileIn, zParams );
                for ( final File file : compressedFiles )
                {
                    String path = file.getPath();
                    path = path.substring( path.lastIndexOf( fileIn.getName() ),
                        path.lastIndexOf( "." ) );
                    flickFile.removeFile( path );
                }
            } else
            {
                final File compressedFile = archiveFile( fileIn, true );
                if ( compressedFile == null )
                    flickFile.addFile( fileIn, zParams );
                else compressSingleFile = true;
            }
            

            if ( configuration.getFlag( DELETE_FLAG ) )
            {
                if ( !FileUtils.deleteQuietly( fileIn ) )
                    System.err.printf( FILE_COULD_NOT_BE_DELETED_WARNING_FORMAT, fileIn.getPath() );
            } else compressedFiles.forEach( file -> {
                if ( !FileUtils.deleteQuietly( file ) )
                    System.err.printf( FILE_COULD_NOT_BE_DELETED_WARNING_FORMAT, file.getPath() );
            } );

            final double overallTime = ( System.currentTimeMillis() - t0 ) / 1000d;

            if ( !compressSingleFile && configuration.getFlag( VERBOSE_FLAG ) )
            {
                // Get the percent deflation on the compressed file
                final double percDeflated = 100 * ( (double) inputPathSize -
                        (double) FileUtils.sizeOf( flickFile.getFile() ) ) / inputPathSize;

                System.out.printf( VERBOSE_COMPRESSION_INFO_FORMAT, fileIn.getName(), overallTime,
                    percDeflated );
            }
        } catch ( final Exception e )
        {
            e.printStackTrace();
        }

        return fileOut;
    }

    @Override
    public String getDefaultDeflatedExtension()
    {
        final String fileExt = "." + Files.getFileExtension( fileIn.getPath() );

        final String deflatedExtension = REGISTRY.getDeflatedExtension( fileExt );

        if ( deflatedExtension != null )
            if ( configuration.getFlag( ARCHIVE_MODE ) == DEFLATION_ARCHIVE_MODE )
                return deflatedExtension;
            else if ( configuration.getFlag( ARCHIVE_MODE ) == INFLATION_ARCHIVE_MODE )
                return Files
                        .getFileExtension( fileIn.getPath().replaceAll( deflatedExtension, "" ) );

        return DEFAULT_DEFLATED_EXTENSION;
    }

    @Override
    public File inflate( final Configuration configuration, final File fileIn, final File fileOut )
    {
        try
        {
            if ( !getDefaultDeflatedExtension()
                    .endsWith( Files.getFileExtension( fileIn.getName() ) ) )
            {
                final File decompressedFile = archiveFile( fileIn, true );

                if ( decompressedFile != null && configuration.getFlag( DELETE_FLAG ) )
                    if ( !FileUtils.deleteQuietly( fileIn ) )
                        System.err.printf( FILE_COULD_NOT_BE_DELETED_WARNING_FORMAT,
                            fileIn.getPath() );

                return decompressedFile;
            }

            final LongAdder unzippedContentsSize = new LongAdder();

            final long inputFileSize = FileUtils.sizeOf( fileIn );
            final long t0 = System.currentTimeMillis();

            flickFile.extractAll( fileOut.getPath() );

            final RandomAccessFile raf = new RandomAccessFile( flickFile.getFile(),
                    InternalZipConstants.READ_MODE );

            final HeaderReader hr = new HeaderReader( raf );
            final ZipModel zm = hr.readAllHeaders();
            final CentralDirectory centralDirectory = zm.getCentralDirectory();
            @SuppressWarnings ( "unchecked" )
            final List<FileHeader> fhs = Collections.checkedList( centralDirectory.getFileHeaders(),
                FileHeader.class );

            final List<File> files = fhs.stream().map( fh -> {
                final File file = FileUtils.getFile( fileOut.getPath(), File.separator,
                    fh.getFileName() );
                unzippedContentsSize.add( file.length() );
                return file;
            } ).collect( Collectors.toList() );

            if ( !configuration.getFlag( KEEP_ZIPPED_FLAG ) )
                // Traverse directory and look for files to decompress
                for ( final File file : files )
                {
                File decompressedFile = null;
                if ( !file.isDirectory() )
                decompressedFile = archiveFile( file, false );

                if ( decompressedFile != null )
                {
                unzippedContentsSize.add( -FileUtils.sizeOf( file ) );
                unzippedContentsSize.add( FileUtils.sizeOf( decompressedFile ) );
                file.delete();
                }
                }

            raf.close();

            if ( configuration.getFlag( DELETE_FLAG ) )
                if ( !FileUtils.deleteQuietly( fileIn ) )
                    System.err.printf( FILE_COULD_NOT_BE_DELETED_WARNING_FORMAT, fileIn.getPath() );

            final double overallTime = ( System.currentTimeMillis() - t0 ) / 1000d;

            if ( configuration.getFlag( VERBOSE_FLAG ) )
            {
                // Get the percent deflation on the compressed file
                final double percDeflated = 100 * unzippedContentsSize.doubleValue() /
                        inputFileSize;

                System.out.printf( VERBOSE_DECOMPRESSION_INFO_FORMAT, fileIn.getName(), overallTime,
                    percDeflated );
            }
        } catch ( final Exception e )
        {
            e.printStackTrace();
        }

        return fileOut;
    }
}
