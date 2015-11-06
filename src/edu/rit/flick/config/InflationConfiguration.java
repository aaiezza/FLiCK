/**
 *  COPYRIGHT (C) 2015 Alex Aiezza. All Rights Reserved.
 *
 *  See the LICENSE for the specific language governing permissions and
 *  limitations under the License provided with this project.
 */
package edu.rit.flick.config;

import edu.rit.flick.genetics.config.FastaInflationOptionSet;
import edu.rit.flick.genetics.config.FastqInflationOptionSet;

/**
 * @author Alex Aiezza
 *
 */
public class InflationConfiguration extends AbstractConfiguration
{
    {
        registerOptionSet( new InflationOptionSet() );

        registerOptionSet( new FastaInflationOptionSet() );
        registerOptionSet( new FastqInflationOptionSet() );
    }
}
