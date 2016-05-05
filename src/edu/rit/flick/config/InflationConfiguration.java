/**
 *  COPYRIGHT (C) 2015 Alex Aiezza. All Rights Reserved.
 *
 *  See the LICENSE for the specific language governing permissions and
 *  limitations under the License provided with this project.
 */
package edu.rit.flick.config;

import static edu.rit.flick.config.DefaultOptionSet.ARCHIVE_MODE;
import static edu.rit.flick.config.DefaultOptionSet.INFLATION_ARCHIVE_MODE;

/**
 * @author Alex Aiezza
 *
 */
public class InflationConfiguration extends AbstractConfiguration
{
    {
        for ( final InflationOptionSet ios : FileArchiverExtensionRegistry.getInstance()
                .getInflationOptionSets() )
            registerOptionSet( ios );

        registerOptionSet( new InflationOptionSet() );

        setFlag( ARCHIVE_MODE, INFLATION_ARCHIVE_MODE );
    }
}
