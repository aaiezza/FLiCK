/**
 *  COPYRIGHT (C) 2015 Alex Aiezza. All Rights Reserved.
 *
 *  See the LICENSE for the specific language governing permissions and
 *  limitations under the License provided with this project.
 */
package edu.rit.flick.config;

import java.util.Map;

/**
 * @author Alex Aiezza
 *
 */
public interface Configuration
{
    public void addFlag( final Flag flag );

    public void addOption( final Option<?> option );

    public boolean getFlag( final Flag flag );

    public Map<Flag, Boolean> getFlags();

    public <V> V getOption( final Option<V> option );

    public Map<Option<?>, Object> getOptions();

    public Option<?> getOptionFromLongFormat( final String option );

    public Option<?> getOptionFromShortFormat( final String optionStr );

    public void registerOptionSet( final OptionSet optionSet );

    public void setFlag( final Flag flag, final boolean value );

    public void setOption( final Option<?> op, final Object object );
}
