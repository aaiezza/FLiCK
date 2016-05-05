/**
 *  COPYRIGHT (C) 2015 Alex Aiezza. All Rights Reserved.
 *
 *  See the LICENSE for the specific language governing permissions and
 *  limitations under the License provided with this project.
 */
package edu.rit.flick.config;

import static edu.rit.flick.config.DefaultOptionSet.ARCHIVE_MODE;
import static edu.rit.flick.config.DefaultOptionSet.DEFLATION_ARCHIVE_MODE;

/**
 * @author Alex Aiezza
 *
 */
public class DeflationConfiguration extends AbstractConfiguration
{
    {
        for ( final DeflationOptionSet dos : FileArchiverExtensionRegistry.getInstance()
                .getDeflationOptionSets() )
            registerOptionSet( dos );

        registerOptionSet( new DeflationOptionSet() );

        setFlag( ARCHIVE_MODE, DEFLATION_ARCHIVE_MODE );
    }

}
