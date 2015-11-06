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
public interface Configuration
{
    public void addFlag( final Flag flag );

    public void addOption( final Option<? extends Object> option );

    public boolean getFlag( final Flag flag );

    public Object getOption( final Option<? extends Object> option );

    public Option<? extends Object> getOptionFromLongFormat( final String option );

    public Option<? extends Object> getOptionFromShortFormat( final String optionStr );

    public void registerOptionSet( final OptionSet<? extends Object> optionSet );

    public void setFlag( final Flag flag, final boolean value );

    public void setOption( final Option<? extends Object> op, final Object object );
}
