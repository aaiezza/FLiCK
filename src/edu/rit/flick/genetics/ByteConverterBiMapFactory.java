/**
 *  COPYRIGHT (C) 2015 Alex Aiezza. All Rights Reserved.
 *
 *  See the LICENSE for the specific language governing permissions and
 *  limitations under the License provided with this project.
 */
package edu.rit.flick.genetics;

import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

/**
 *
 * @author Alex Aiezza
 *
 */
public class ByteConverterBiMapFactory
{
    private static final char [] DEFAULT_NUCLEOTIDES;

    static
    {
        // DEFAULT_NUCLEOTIDES = new char [] { 'A', 'C', 'G', 'T', 'N' };
        DEFAULT_NUCLEOTIDES = new char [] { 'A', 'C', 'G', 'T' };
    }

    public static void main( final String [] args )
    {
        final BiMap<String, Byte> byteConverter = new ByteConverterBiMapFactory()
                .getByteConverter( 4 );

        System.out.println( byteConverter.size() );

        System.out.println( "A    : " + byteConverter.get( "A" ) );
        System.out.println( "AT   : " + byteConverter.get( "AT" ) );
        System.out.println( "TNAG : " + byteConverter.get( "TNAG" ) );
        System.out.println( "TTAG : " + byteConverter.get( "TTAG" ) );
    }

    private final char [] NUCLEOTIDES;

    /**
     *
     */
    public ByteConverterBiMapFactory()
    {
        NUCLEOTIDES = DEFAULT_NUCLEOTIDES;
    }

    /**
     * @param nucleotides
     */
    public ByteConverterBiMapFactory( final char [] nucleotides )
    {
        NUCLEOTIDES = nucleotides;
    }

    /**
     *
     * @param permutation
     * @param permutationId
     * @param permutationLength
     * @param byteConverter
     */
    void fillConverterMap(
            final String permutation,
            final AtomicInteger permutationId,
            final int permutationLength,
            final BiMap<String, Byte> byteConverter )
    {
        if ( permutation.length() >= permutationLength )
            return;

        // nucleotideIndex
        for ( final char element : NUCLEOTIDES )
        {
            byteConverter.put( permutation + element,
                (byte) permutationId.getAndAdd( 0x0000_0001 ) );

            fillConverterMap( permutation + element, permutationId, permutationLength,
                byteConverter );
        }
    }

    /**
     *
     * @param permutation
     * @param permutationId
     * @param permutationLength
     * @param byteConverter
     */
    void fillConverterMapFullSize(
            final String permutation,
            final AtomicInteger permutationId,
            final int permutationLength,
            final BiMap<String, Byte> byteConverter )
    {
        if ( permutation.length() >= permutationLength )
            return;

        // nucleotideIndex
        for ( final char element : NUCLEOTIDES )
        {
            if ( ( permutation + element ).length() == permutationLength )
                byteConverter.put( permutation + element,
                    (byte) permutationId.getAndAdd( 0x0000_0001 ) );

            fillConverterMapFullSize( permutation + element, permutationId, permutationLength,
                byteConverter );
        }
    }

    /**
     * @param segmentLength
     * @return
     */
    public BiMap<String, Byte> getByteConverter( final int segmentLength )
    {
        final BiMap<String, Byte> byteConverter = HashBiMap.create();

        final AtomicInteger permutationId = new AtomicInteger( Byte.MIN_VALUE );

        fillConverterMapFullSize( "", permutationId, segmentLength, byteConverter );

        return byteConverter;
    }
}
