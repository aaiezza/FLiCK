/**
 *  COPYRIGHT (C) 2015 Alex Aiezza. All Rights Reserved.
 *
 *  See the LICENSE for the specific language governing permissions and
 *  limitations under the License provided with this project.
 */
package edu.rit.flick.util;

import static edu.rit.flick.config.DefaultOptionSet.ARCHIVE_MODE;
import static edu.rit.flick.config.DefaultOptionSet.DEFLATION_ARCHIVE_MODE;
import static edu.rit.flick.config.DefaultOptionSet.HELP_FLAG;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import edu.rit.flick.DefaultFlickFile;
import edu.rit.flick.FlickFile;
import edu.rit.flick.config.Configuration;
import edu.rit.flick.config.ConfigurationProcessor;
import edu.rit.flick.config.DeflationConfiguration;

/**
 * @author Alex Aiezza
 *
 */
public class Flick
{
    public static final String  FLICK_USAGE_FILE = "FlickUsage.txt";

    /*
     * USAGE statement
     */
    private static final String USAGE_FORMAT;

    static
    {
        final StringBuilder usage = new StringBuilder();
        final BufferedReader br = new BufferedReader( new InputStreamReader( Flick.class
            .getClassLoader().getResourceAsStream( FLICK_USAGE_FILE ) ) );
        br.lines().forEach( line -> usage.append( line ).append( "\n" ) );
        USAGE_FORMAT = usage.toString();
    }

    public static void main( final String... args )
    {
        try
        {
            final Configuration configuration = new DeflationConfiguration();
            ConfigurationProcessor.processConfiguration( configuration, args );
            configuration.setFlag( ARCHIVE_MODE, DEFLATION_ARCHIVE_MODE );

            if ( args.length <= 0 || configuration.getFlag( HELP_FLAG ) )
            {
                USAGE();
                return;
            }

            final FlickFile flickFile = new DefaultFlickFile( configuration );

            flickFile.deflate();

        } catch ( final Exception e )
        {
            System.err.println( e.getMessage().trim() );
            e.printStackTrace();
            System.err.println();
        }
    }

    static final void USAGE()
    {
        System.out.printf( USAGE_FORMAT );
    }
}
