/**
 *  COPYRIGHT (C) 2015 Alex Aiezza. All Rights Reserved.
 *
 *  See the LICENSE for the specific language governing permissions and
 *  limitations under the License provided with this project.
 */
package edu.rit.flick.config;

/**
 * @author Alex Aiezza
 *
 */
public class InflationOptionSet extends AbstractOptionSet
{
    public static final Flag KEEP_ZIPPED_FLAG = new DefaultFlag( "keep zipped", "keep-zipped", "",
            false );

    {
        options.add( KEEP_ZIPPED_FLAG );
    }
}
