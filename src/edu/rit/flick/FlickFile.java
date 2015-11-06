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
public interface FlickFile extends FileDeflator, FileInflator
{
    public default File deflate()
    {
        return deflate( getConfiguration(), getFileIn(), getFileOut() );
    }

    public Configuration getConfiguration();

    public File getFileIn();

    public String getFileInPath();

    public File getFileOut();

    public String getFileOutPath();

    public default File inflate()
    {
        return inflate( getConfiguration(), getFileIn(), getFileOut() );
    }
}
