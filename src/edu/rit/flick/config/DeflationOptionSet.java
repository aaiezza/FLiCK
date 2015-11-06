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
public class DeflationOptionSet extends AbstractOptionSet<Boolean>
{
    public static final Flag NO_ZIP_FLAG = new DefaultFlag( "no zip", "no-zip", "", false );

    {
        options.add( NO_ZIP_FLAG );
    }
}
