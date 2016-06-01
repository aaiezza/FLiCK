package edu.rit.flick.genetics.nucleotide.kmer;

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
        return new Kmer( suffix() + suffixNucleotide );
    }

    public Kmer makeChildAndAdd( final byte suffixNucleotide )
    {
        final Kmer kmer = makeChild( suffixNucleotide );

        return kmer;
    }

    public String prefix()
    {
        return kmer.substring( 0, kmer.length() - 1 );
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
