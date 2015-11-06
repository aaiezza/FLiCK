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
public interface Option <V>
{
    public static final String LONG_FLAG_FORMAT  = "--";

    public static final String SHORT_FLAG_FORMAT = "-";

    public V getDefaultValue();

    public String getLongFlag();

    public String getName();

    public String getShortFlag();

    public default boolean isFlag()
    {
        return false;
    }

    public V parseValue( final String value );
}
