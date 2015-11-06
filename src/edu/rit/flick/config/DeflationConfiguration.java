/**
 *  COPYRIGHT (C) 2015 Alex Aiezza. All Rights Reserved.
 *
 *  See the LICENSE for the specific language governing permissions and
 *  limitations under the License provided with this project.
 */
package edu.rit.flick.config;

import edu.rit.flick.genetics.config.FastaDeflationOptionSet;
import edu.rit.flick.genetics.config.FastqDeflationOptionSet;

/**
 * @author Alex Aiezza
 *
 */
public class DeflationConfiguration extends AbstractConfiguration
{
    {
        registerOptionSet( new DeflationOptionSet() );

        registerOptionSet( new FastaDeflationOptionSet() );
        registerOptionSet( new FastqDeflationOptionSet() );
    }

}
