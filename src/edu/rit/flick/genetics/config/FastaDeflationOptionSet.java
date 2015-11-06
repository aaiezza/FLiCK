/**
 *  COPYRIGHT (C) 2015 Alex Aiezza. All Rights Reserved.
 *
 *  See the LICENSE for the specific language governing permissions and
 *  limitations under the License provided with this project.
 */
package edu.rit.flick.genetics.config;

import edu.rit.flick.config.DefaultFlag;
import edu.rit.flick.config.DeflationOptionSet;
import edu.rit.flick.config.Flag;

/**
 * @author Alex Aiezza
 *
 */
public class FastaDeflationOptionSet extends DeflationOptionSet
{
    public static final Flag NO_ZIP_FA = new DefaultFlag( "no zip fa", "no-zip-fa", "", false );

    {
        options.add( NO_ZIP_FA );
    }

}
