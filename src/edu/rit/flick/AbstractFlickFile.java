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
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.NoSuchFileException;

import org.apache.commons.io.FileUtils;

import com.google.common.io.Files;

import edu.rit.flick.config.Configuration;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;

/**
 * @author Alex Aiezza
 *
 */
public abstract class AbstractFlickFile implements FlickFile
{
    public static final String    FILE_NOT_FOUND_EXCEPTION_MESSAGE               = "file not found";

    private static final String   FILE_ALREADY_EXISTS_AS_DIRECTORY_EXCEPTION     = "already exists as directory";

    private static final String   CANT_OVERWRITE_EXISTING_FILE_WITHOT_FORCE_FLAG = "\n  can't overwrite existing file without --force flag";

    protected final File          fileIn, fileOut;

    protected final Configuration configuration;

    protected final ZipFile       flickFile;

    protected final ZipParameters zParams;

    public AbstractFlickFile( final Configuration configuration )
        throws ZipException,
        FileAlreadyExistsException,
        NoSuchFileException
    {
        this.configuration = configuration;

        init();

        final String inputPath = (String) configuration.getOption( INPUT_PATH );
        final Object outputPath = configuration.getOption( OUTPUT_PATH );

        fileIn = new File( inputPath );
        fileOut = new File(
                outputPath == null ? configuration
                        .getFlag( ARCHIVE_MODE )
                                                 ? fileIn.getPath() + getDefaultDeflatedExtension()
                                                 : fileIn.getPath()
                                                         .replaceAll( "." + Files.getFileExtension(
                                                             fileIn.getPath() ), "" )
                                   : (String) outputPath );

        if ( !fileIn.exists() )
            throw new NoSuchFileException( fileIn.getPath(), null,
                    FILE_NOT_FOUND_EXCEPTION_MESSAGE );

        if ( fileOut.isDirectory() )
            throw new FileAlreadyExistsException( FILE_ALREADY_EXISTS_AS_DIRECTORY_EXCEPTION );

        if ( fileOut.exists() )
        {
            if ( !configuration.getFlag( FORCE_FLAG ) )
                throw new FileAlreadyExistsException( fileIn.getPath(), fileOut.getPath(),
                        CANT_OVERWRITE_EXISTING_FILE_WITHOT_FORCE_FLAG );
            FileUtils.deleteQuietly( fileOut );
        }

        flickFile = new ZipFile( configuration.getFlag( ARCHIVE_MODE ) ? fileOut : fileIn );

        zParams = new ZipParameters();

        zParams.setIncludeRootFolder( true );
        zParams.setCompressionMethod( Zip4jConstants.COMP_DEFLATE );
        zParams.setCompressionLevel( Zip4jConstants.DEFLATE_LEVEL_FAST );
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

    protected void init()
    {}
}
