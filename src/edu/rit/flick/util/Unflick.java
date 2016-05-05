/**
 *  COPYRIGHT (C) 2015 Alex Aiezza. All Rights Reserved.
 *
 *  See the LICENSE for the specific language governing permissions and
 *  limitations under the License provided with this project.
 */
package edu.rit.flick.util;

import static edu.rit.flick.config.DefaultOptionSet.HELP_FLAG;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import edu.rit.flick.DefaultFlickFile;
import edu.rit.flick.FlickFile;
import edu.rit.flick.config.Configuration;
import edu.rit.flick.config.ConfigurationProcessor;
import edu.rit.flick.config.InflationConfiguration;

/**
 * @author Alex Aiezza
 *
 */
public class Unflick
{
    public static final String  UNFLICK_USAGE_FILE = "UnflickUsage.txt";

    /*
     * USAGE statement
     */
    private static final String USAGE_FORMAT;

    static
    {
        final StringBuilder usage = new StringBuilder();
        final BufferedReader br = new BufferedReader( new InputStreamReader(
                Flick.class.getClassLoader().getResourceAsStream( UNFLICK_USAGE_FILE ) ) );
        br.lines().forEach( line -> usage.append( line ).append( "\n" ) );
        USAGE_FORMAT = usage.toString();
    }

    public static void main( final String... args )
    {
        final Configuration configuration = new InflationConfiguration();
        ConfigurationProcessor.processConfiguration( configuration, args );

        if ( args.length <= 0 || configuration.getFlag( HELP_FLAG ) )
        {
            USAGE();
            return;
        }

        try
        {
            final FlickFile flickFile = new DefaultFlickFile( configuration );

            flickFile.inflate();

        } catch ( final Exception e )
        {
            System.err.println( e.getMessage().trim() );
        }
    }

    static final void USAGE()
    {
        System.out.printf( USAGE_FORMAT );
    }
}
