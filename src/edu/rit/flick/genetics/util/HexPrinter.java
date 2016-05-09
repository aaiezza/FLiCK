/**
 *  COPYRIGHT (C) 2015 Alex Aiezza. All Rights Reserved.
 *
 *  See the LICENSE for the specific language governing permissions and
 *  limitations under the License provided with this project.
 */
package edu.rit.flick.genetics.util;

import static org.apache.commons.lang.StringUtils.leftPad;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @author Alex Aiezza
 *
 */
public class HexPrinter
{
    public static final int RADIX = Short.SIZE;

    public static void shortToFile( final short s, final OutputStream out ) throws IOException
    {
        out.write( new byte [] { (byte) ( s >> 8 ), (byte) ( s & 0b1111_1111 ) } );
    }

    public static String shortToHexString( final short s )
    {
        return leftPad( Integer.toHexString( s ), 4, '0' );
    }

    public static String shortToBinaryString( final short s )
    {
        return intToBinaryString( Short.toUnsignedInt( s ) );
    }

    public static String intToBinaryString( final int i )
    {
        final StringBuilder str = new StringBuilder( org.apache.commons.lang.StringUtils
                .leftPad( Integer.toBinaryString( i ), 16, '0' ) );

        for ( int idx = str.length() - 4; idx > 0; idx -= 4 )
            str.insert( idx, "_" );

        return str.toString();
    }

    private HexPrinter()
    {}
}
