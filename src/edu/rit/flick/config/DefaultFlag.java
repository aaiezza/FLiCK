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
public class DefaultFlag extends AbstractOption<Boolean> implements Flag
{
    /**
     * @param name
     * @param longFlag
     * @param shortFlag
     * @param defaultValue
     */
    public DefaultFlag(
        final String name,
        final String longFlag,
        final String shortFlag,
        final boolean defaultValue )
    {
        super( name, longFlag, shortFlag, defaultValue );
    }

    @Override
    public String toString()
    {
        return String.format( "Flag (%s)", getName() );
    }
}
