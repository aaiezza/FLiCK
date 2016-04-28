/**
 *  COPYRIGHT (C) 2015 Alex Aiezza. All Rights Reserved.
 *
 *  See the LICENSE for the specific language governing permissions and
 *  limitations under the License provided with this project.
 */
package edu.rit.flick.genetics.config;

import edu.rit.flick.config.DefaultFlag;
import edu.rit.flick.config.Flag;
import edu.rit.flick.config.InflationOptionSet;

/**
 * @author Alex Aiezza
 *
 */
public class FastaInflationOptionSet extends InflationOptionSet
{
    public static final Flag KEEP_ZIPPED_FA = new DefaultFlag( "keep zipped fa", "keep-zipped-fa",
            "", false );

    {
        options.add( KEEP_ZIPPED_FA );
    }
}
