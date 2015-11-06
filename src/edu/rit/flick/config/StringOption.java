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
public class StringOption extends AbstractOption<String>
{
    /**
     * @param name
     * @param longFlag
     * @param shortFlag
     * @param defaultValue
     */
    public StringOption(
        final String name,
        final String longFlag,
        final String shortFlag,
        final String defaultValue )
    {
        super( name, longFlag, shortFlag, defaultValue );
    }

    @Override
    public String parseValue( final String value )
    {
        return value;
    }

    @Override
    public String toString()
    {
        return String.format( "StringOption (%s)", getName() );
    }
}
