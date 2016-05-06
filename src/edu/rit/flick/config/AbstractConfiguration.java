/**
 *  COPYRIGHT (C) 2015 Alex Aiezza. All Rights Reserved.
 *
 *  See the LICENSE for the specific language governing permissions and
 *  limitations under the License provided with this project.
 */
package edu.rit.flick.config;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * @author Alex Aiezza
 *
 */
public abstract class AbstractConfiguration implements Configuration
{
    private final Map<Option<? extends Object>, Object> options;

    private final Map<Flag, Boolean>                    flags;

    {
        options = new HashMap<Option<? extends Object>, Object>();
        flags = new HashMap<Flag, Boolean>();
    }

    @SuppressWarnings ( "unchecked" )
    public AbstractConfiguration()
    {
        registerOptionSet( new DefaultOptionSet() );
    }

    @Override
    public void addFlag( final Flag flag )
    {
        flags.put( flag, flag.getDefaultValue() );
    }

    @Override
    public void addOption( final Option<? extends Object> option )
    {
        options.put( option, option.getDefaultValue() );
    }

    @Override
    public boolean getFlag( final Flag flag )
    {
        return flags.get( flag );
    }

    @Override
    public Object getOption( final Option<? extends Object> option )
    {
        return options.get( option );
    }

    @Override
    public Option<? extends Object> getOptionFromLongFormat( final String optionStr )
    {
        return getOptionFromString( optionStr, e -> e.getLongFlag().matches( optionStr ) );
    }

    @Override
    public Option<? extends Object> getOptionFromShortFormat( final String optionStr )
    {
        return getOptionFromString( optionStr, e -> e.getShortFlag().matches( optionStr ) );
    }

    private Option<? extends Object> getOptionFromString(
            final String optionStr,
            final Predicate<Option<? extends Object>> predicate )
    {
        for ( final Option<? extends Object> op : options.keySet() )
            if ( predicate.test( op ) )
                return op;

        for ( final Option<? extends Object> op : flags.keySet() )
            if ( predicate.test( op ) )
                return op;

        return null;
    }

    @Override
    public void registerOptionSet( final OptionSet<? extends Object> optionSet )
    {
        for ( final Option<? extends Object> option : optionSet.getOptions() )
            if ( option.isFlag() )
                addFlag( (Flag) option );
            else addOption( option );
    }

    @Override
    public void setFlag( final Flag flag, final boolean value )
    {
        flags.put( flag, value );
    }

    @Override
    public void setOption( final Option<? extends Object> option, final Object value )
    {
        options.put( option, value );
    }

    @Override
    public Map<Flag, Boolean> getFlags()
    {
        return flags;
    }

    @Override
    public Map<Option<?>, Object> getOptions()
    {
        return options;
    }

    @Override
    public int hashCode()
    {
        return getOptions().hashCode() + getFlags().hashCode();
    }

    @Override
    public boolean equals( final Object obj )
    {
        if ( obj instanceof Configuration )
        {
            return this.getOptions().equals( ( (Configuration) obj ).getOptions() ) &&
                    this.getFlags().equals( ( (Configuration) obj ).getFlags() );
        }
        return false;
    }

    @Override
    public String toString()
    {
        return String.format( "%s\n%s", getOptions(), getFlags() );
    }
}
