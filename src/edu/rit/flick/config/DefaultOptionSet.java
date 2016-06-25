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
public class DefaultOptionSet extends AbstractOptionSet
{
    public static final boolean        DEFLATION_ARCHIVE_MODE = true;
    public static final boolean        INFLATION_ARCHIVE_MODE = false;

    public static final Flag           ARCHIVE_MODE           = new DefaultFlag( "archive mode", "",
            "", DEFLATION_ARCHIVE_MODE );

    public static final Flag           HELP_FLAG              = new DefaultFlag( "help", "help", "",
            false );
    public static final Flag           DELETE_FLAG            = new DefaultFlag( "delete", "delete",
            "d", false );
    public static final Flag           VERBOSE_FLAG           = new DefaultFlag( "verbose",
            "verbose", "v", false );
    public static final Flag           FORCE_FLAG             = new DefaultFlag( "force", "force",
            "f", false );

    public static final Option<String> INPUT_PATH             = new StringOption( "input path", "",
            "", "" );
    public static final Option<String> OUTPUT_PATH            = new StringOption( "output path", "",
            "", null );


    {
        options.add( ARCHIVE_MODE );

        options.add( HELP_FLAG );
        options.add( DELETE_FLAG );
        options.add( VERBOSE_FLAG );
        options.add( FORCE_FLAG );

        options.add( INPUT_PATH );
        options.add( OUTPUT_PATH );
    }
}
