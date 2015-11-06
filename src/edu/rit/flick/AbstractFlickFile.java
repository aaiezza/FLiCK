/**
 *  COPYRIGHT (C) 2015 Alex Aiezza. All Rights Reserved.
 *
 *  See the LICENSE for the specific language governing permissions and
 *  limitations under the License provided with this project.
 */
package edu.rit.flick;

import static edu.rit.flick.config.DefaultOptionSet.ARCHIVE_MODE;
import static edu.rit.flick.config.DefaultOptionSet.INPUT_PATH;
import static edu.rit.flick.config.DefaultOptionSet.OUTPUT_PATH;

import java.io.File;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;

import org.apache.commons.io.FileUtils;

import edu.rit.flick.config.Configuration;

/**
 * @author Alex Aiezza
 *
 */
public abstract class AbstractFlickFile implements FlickFile
{
    private final File            fileIn, fileOut;

    protected final Configuration configuration;

    protected final ZipFile       flickFile;

    protected final ZipParameters zParams;

    public AbstractFlickFile( final Configuration configuration ) throws ZipException
    {
        this.configuration = configuration;

        final String inputPath = (String) configuration.getOption( INPUT_PATH );
        final Object outputPath = configuration.getOption( OUTPUT_PATH );

        fileIn = new File( inputPath );
        fileOut = new File( outputPath == null ? fileIn.getPath() + getDefaultDeflatedExtension()
                                               : (String) outputPath );

        if ( fileOut.exists() )
            FileUtils.deleteQuietly( fileOut );

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
}
