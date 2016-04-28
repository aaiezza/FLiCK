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
public class FastqInflationOptionSet extends InflationOptionSet
{
    public static final Flag KEEP_ZIPPED_FQ = new DefaultFlag( "keep zipped fq", "keep-zipped-fq",
            "", false );

    {
        options.add( KEEP_ZIPPED_FQ );
    }
}
