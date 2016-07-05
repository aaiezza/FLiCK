package edu.rit.flick.genetics;

import static edu.rit.flick.genetics.nucleotide.Nucleotide.A;
import static edu.rit.flick.genetics.nucleotide.Nucleotide.C;
import static edu.rit.flick.genetics.nucleotide.Nucleotide.G;
import static edu.rit.flick.genetics.nucleotide.Nucleotide.T;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;

public class TwoBitNucleotideConverter
{
    private final BiMap<Byte, Byte> nucleotideMap;

    {
        nucleotideMap = ImmutableBiMap.<Byte, Byte> builder().put( A, (byte) 0b00 )
                .put( C, (byte) 0b01 ).put( G, (byte) 0b10 ).put( T, (byte) 0b11 ).build();
    }

    public byte convert( final String tetramer )
    {
        if ( tetramer.length() != 4 )
            throw new IllegalArgumentException(
                    String.format( "%s is not a tetramer.", tetramer ) );

        byte twoBitTetramer = 0b0000_0000;
        for ( int base = 0; base < tetramer.length(); base++ )
        {
            twoBitTetramer += nucleotideMap.get( (byte) tetramer.charAt( base ) );
            if ( base + 1 < tetramer.length() )
                twoBitTetramer <<= 2;

        }
        return twoBitTetramer;
    }

    public String convert( final byte twoBitTetramer )
    {
        byte [] tetramer = new byte [4];
        short mask = 0b1100_0000;
        for ( int base = tetramer.length - 1; base >= 0; base-- )
        {
            tetramer[tetramer.length - 1 - base] = nucleotideMap.inverse()
                    .get( (byte) ( ( twoBitTetramer & mask ) >> ( base * 2 ) ) );
            mask >>= 2;
        }
        return new String( tetramer );
    }

    public static void main( final String [] args )
    {
        final TwoBitNucleotideConverter tbnc = new TwoBitNucleotideConverter();
        System.out.println( tbnc.nucleotideMap );

        final String [] tests = new String [] { "GTCA", "ACGT", "CGTC", "AAAA" };
        for ( final String test : tests )
        {
            byte tet = tbnc.convert( test );
            String back = tbnc.convert( tet );
            System.out.printf( " %s -> (%03d) %s -> %s\n", test, tet,
                String.format( "%8s", Integer.toBinaryString( Byte.toUnsignedInt( tet ) ) )
                        .replace( ' ', '0' ),
                back );
        }
    }
}
