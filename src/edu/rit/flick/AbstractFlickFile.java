/**
 *  COPYRIGHT (C) 2015 Alex Aiezza. All Rights Reserved.
 *
 *  See the LICENSE for the specific language governing permissions and
 *  limitations under the License provided with this project.
 */
package edu.rit.flick;

import static edu.rit.flick.config.DefaultOptionSet.ARCHIVE_MODE;
import static edu.rit.flick.config.DefaultOptionSet.FORCE_FLAG;
import static edu.rit.flick.config.DefaultOptionSet.INPUT_PATH;
import static edu.rit.flick.config.DefaultOptionSet.OUTPUT_PATH;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.NoSuchFileException;
import java.util.Scanner;

import org.apache.commons.io.FileUtils;

import com.google.common.io.Files;

import edu.rit.flick.config.Configuration;
import edu.rit.flick.config.FileArchiverExtensionRegistry;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;

/**
 * @author Alex Aiezza
 *
 */
public abstract class AbstractFlickFile implements FlickFile
{
    public static final String                    FILE_NOT_FOUND_EXCEPTION_MESSAGE               = "file not found";

    public static final String                    FILE_IS_EMPTY_EXCEPTION_FORMAT                 = "Input file %s is empty.";

    private static final String                   FILE_ALREADY_EXISTS_AS_DIRECTORY_EXCEPTION     = "already exists as directory";

    private static final String                   CANT_OVERWRITE_EXISTING_FILE_WITHOT_FORCE_FLAG = "\n  can't overwrite existing file without --force flag";

    protected final File                          fileIn, fileOut;

    protected final Configuration                 configuration;

    protected final ZipFile                       flickFile;

    protected final ZipParameters                 zParams;

    protected final FileArchiverExtensionRegistry REGISTRY                                       = FileArchiverExtensionRegistry
            .getInstance();

    public AbstractFlickFile( final Configuration configuration ) throws Exception
    {
        this.configuration = configuration;

        final String inputPath = configuration.getOption( INPUT_PATH );
        final Object outputPath = configuration.getOption( OUTPUT_PATH );

        // Obtain the would-be input and output files
        fileIn = new File( inputPath );
        fileOut = new File(
                outputPath == null ? configuration
                        .getFlag( ARCHIVE_MODE )
                                                 ? fileIn.getPath() + getDefaultDeflatedExtension()
                                                 : fileIn.getPath()
                                                         .replaceAll( "." + Files.getFileExtension(
                                                             fileIn.getPath() ), "" )
                                   : (String) outputPath );

        defaultDeflationInflationVerification();

        if ( configuration.getFlag( ARCHIVE_MODE ) )
            defaultDeflationVerification();
        else defaultInflationVerification();

        /*
         * TODO eventually, get around to being able to alter the default
         * compression.
         */
        // Default compression is Zip
        flickFile = new ZipFile( configuration.getFlag( ARCHIVE_MODE ) ? fileOut : fileIn );

        zParams = new ZipParameters();

        zParams.setIncludeRootFolder( true );
        zParams.setCompressionMethod( Zip4jConstants.COMP_DEFLATE );
        zParams.setCompressionLevel( Zip4jConstants.DEFLATE_LEVEL_FAST );
    }

    private void defaultDeflationInflationVerification() throws Exception
    {
        // Verify the input file exists
        if ( !fileIn.exists() )
            throw new NoSuchFileException( fileIn.getPath(), null,
                    FILE_NOT_FOUND_EXCEPTION_MESSAGE );

        // Verify the output file does not exist
        if ( fileOut.exists() )
        {
            // Since file does exist:

            // Verify the force is on
            if ( !configuration.getFlag( FORCE_FLAG ) )
                throw new FileAlreadyExistsException( fileIn.getPath(), fileOut.getPath(),
                        CANT_OVERWRITE_EXISTING_FILE_WITHOT_FORCE_FLAG );

            // Verify the output file is not a directory
            if ( fileOut.isDirectory() )
                throw new FileAlreadyExistsException( FILE_ALREADY_EXISTS_AS_DIRECTORY_EXCEPTION );

            FileUtils.deleteQuietly( fileOut );
        }

        // Subclass's verification
        deflationInflationVerification();
    }

    private void defaultDeflationVerification() throws Exception
    {
        // Verify the input file is not empty
        if ( !fileIn.isDirectory() )
            try ( final Scanner sc = new Scanner( fileIn ) )
            {
                if ( !sc.hasNext() )
                    throw new IOException(
                            String.format( FILE_IS_EMPTY_EXCEPTION_FORMAT, fileIn.getPath() ) );
            }

        // Subclass's deflation verification
        deflationVerification();
    }

    private void defaultInflationVerification() throws Exception
    {

        // Subclass's inflation verification
        inflationVerification();
    }

    @Override
    public Configuration getConfiguration()
    {
        return configuration;
    }

    @Override
    public File getFileIn()
    {
        return fileIn;
    }

    @Override
    public String getFileInPath()
    {
        return fileIn.getPath();
    }

    @Override
    public File getFileOut()
    {
        return fileOut;
    }

    @Override
    public String getFileOutPath()
    {
        return fileOut.getPath();
    }
}
