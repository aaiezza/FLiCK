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
public abstract class AbstractOption <V> implements Option<V>
{
    private final String name;

    private final String longFlag;

    private final String shortFlag;

    private final V      defaultValue;

    public AbstractOption(
        final String name,
        final String longFlag,
        final String shortFlag,
        final V defaultValue )
    {
        this.name = name;
        this.longFlag = longFlag;
        this.shortFlag = shortFlag;
        this.defaultValue = defaultValue;
    }

    @Override
    public V getDefaultValue()
    {
        return defaultValue;
    }

    @Override
    public String getLongFlag()
    {
        return longFlag;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public String getShortFlag()
    {
        return shortFlag;
    }
}
