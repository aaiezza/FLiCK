package edu.rit.flick.genetics.nucleotide.kmer;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import edu.rit.flick.genetics.nucleotide.Nucleotide;

/**
 * @author Alex Aiezza
 *
 */
public class Kmer
{
    private final String kmer;

    public Kmer( final String kmer )
    {
        this.kmer = kmer;
    }

    @Override
    public boolean equals( final Object obj )
    {
        if ( obj instanceof Kmer )
            return ( (Kmer) obj ).kmer.equals( kmer );
        return false;
    }

    public String firstNucleotide()
    {
        return kmer.charAt( 0 ) + "";
    }

    public String getKmer()
    {
        return kmer;
    }

    @Override
    public int hashCode()
    {
        return kmer.hashCode();
    }

    public String lastNucleotide()
    {
        return kmer.charAt( kmer.length() - 1 ) + "";
    }

    public Kmer makeChild( final byte suffixNucleotide )
    {
        return new Kmer( suffix() + (char) suffixNucleotide );
    }

    public String prefix()
    {
        return kmer.substring( 0, kmer.length() - 1 );
    }

    public Set<Kmer> getPossibleSuccessors()
    {
        return Stream.of( Nucleotide.A, Nucleotide.C, Nucleotide.G, Nucleotide.T )
                .map( n -> new Kmer( suffix() + (char) n.byteValue() ) )
                .collect( Collectors.toSet() );
    }

    public int size()
    {
        return kmer.length();
    }

    public String suffix()
    {
        return kmer.substring( 1 );
    }

    @Override
    public String toString()
    {
        return String.format( "%s[%d]", kmer, kmer.length() );
    }
}
