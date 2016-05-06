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
public interface FileInflator extends FileArchiver
{
    public default File inflate( final Configuration configuration, final File fileIn )
    {
        return inflate( configuration, fileIn,
            new File( fileIn.getPath().replaceAll( getDefaultDeflatedExtension(), "" ) ) );
    }

    public File inflate( final Configuration configuration, final File fileIn, final File fileOut );

    public default File inflate(
            final Configuration configuration,
            final String fileIn,
            final String fileOut )
    {
        return inflate( configuration, new File( fileIn ), new File( fileOut ) );
    }

    public default void inflationVerification() throws Exception
    {}
}
