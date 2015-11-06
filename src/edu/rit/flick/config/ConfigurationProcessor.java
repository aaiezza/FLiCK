/**
 *  COPYRIGHT (C) 2015 Alex Aiezza. All Rights Reserved.
 *
 *  See the LICENSE for the specific language governing permissions and
 *  limitations under the License provided with this project.
 */
package edu.rit.flick.config;

import static edu.rit.flick.config.Option.LONG_FLAG_FORMAT;
import static edu.rit.flick.config.Option.SHORT_FLAG_FORMAT;

import java.util.Arrays;
import java.util.Iterator;

/**
 * @author Alex Aiezza
 *
 */
public class ConfigurationProcessor
{
    /**
     *
     * @param configuration
     * @param args
     * @return the arguments deemed to not be options
     */
    public static void processConfiguration(
            final Configuration configuration,
            final String... args )
    {
        final Iterator<String> arguments = Arrays.asList( args ).iterator();
        while ( arguments.hasNext() )
        {
            String option = arguments.next();

            if ( option.startsWith( LONG_FLAG_FORMAT ) )
            {
                option = option.substring( 2 );

                final Option<?> op = configuration.getOptionFromLongFormat( option );

                if ( op.isFlag() )
                    configuration.setFlag( (Flag) op, ! ( (Flag) op ).getDefaultValue() );
                else configuration.setOption( op, op.parseValue( arguments.next() ) );

            } else if ( option.startsWith( SHORT_FLAG_FORMAT ) )
            {
                option = option.substring( 1 );

                for ( final String o : option.split( "" ) )
                {
                    final Option<?> op = configuration.getOptionFromShortFormat( o );

                    if ( op.isFlag() )
                        configuration.setFlag( (Flag) op, ! ( (Flag) op ).getDefaultValue() );
                    else configuration.setOption( op, op.parseValue( arguments.next() ) );
                }
            } else
            {
                // Assume this is the input path
                configuration.setOption( DefaultOptionSet.INPUT_PATH, option );
                if ( arguments.hasNext() )
                    configuration.setOption( DefaultOptionSet.OUTPUT_PATH, arguments.next() );
            }
        }
    }
}
