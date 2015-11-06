/**
 *  COPYRIGHT (C) 2015 Alex Aiezza. All Rights Reserved.
 *
 *  See the LICENSE for the specific language governing permissions and
 *  limitations under the License provided with this project.
 */
package edu.rit.util;

import static java.lang.Integer.toHexString;

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
        final byte w1 = (byte) ( s >> 8 );
        final byte w2 = (byte) ( s & 0b1111_1111 );
        out.write( new byte [] { w1, w2 } );
    }

    public static String shortToHexString( final short s )
    {
        final StringBuilder hex = new StringBuilder();
        final int w1 = s >> 8;
        final int w2 = s & 0b1111_1111;
        String num = toHexString( w1 );
        hex.append( ( "00" + num ).substring( num.length() ) );
        num = Integer.toHexString( w2 );
        hex.append( ( "00" + num ).substring( num.length() ) );

        return hex.toString();
    }

    private HexPrinter()
    {}
}
