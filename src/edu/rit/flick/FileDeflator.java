/**
 *  COPYRIGHT (C) 2015 Alex Aiezza. All Rights Reserved.
 *
 *  See the LICENSE for the specific language governing permissions and
 *  limitations under the License provided with this project.
 */
package edu.rit.flick;

import java.io.File;

import edu.rit.flick.config.Configuration;

/**
 * @author Alex Aiezza
 *
 */
public interface FileDeflator extends FileArchiver
{
    public default File deflate( final Configuration configuration, final File fileIn )
    {
        return deflate( configuration, fileIn,
            new File( fileIn.getPath() + getDefaultDeflatedExtension() ) );
    }

    public File deflate( final Configuration configuration, final File fileIn, final File fileOut );

    public default File deflate(
            final Configuration configuration,
            final String fileIn,
            final String fileOut )
    {
        return deflate( configuration, new File( fileIn ), new File( fileOut ) );
    }
}
