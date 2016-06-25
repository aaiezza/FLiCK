package edu.rit.flick.genetics.nucleotide.kmer;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Alex Aiezza
 *
 */
public class Kmer
{
    private final String    kmer;
    // private final List<Kmer> parents;
    private final Set<Kmer> children;

    public Kmer( final String kmer )
    {
        this.kmer = kmer;
        this.children = Collections.synchronizedSet( new HashSet<Kmer>( 4 ) );
    }

    public Set<Kmer> getChildren()
    {
        return children;
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

    public void addChild( final Kmer kmer )
    {
        children.add( kmer );
    }

    public String getKmer()
    {
        return kmer;
    }

    public String prefix()
    {
        return kmer.substring( 0, kmer.length() - 1 );
    }

    public String suffix()
    {
        return kmer.substring( 1 );
    }

    public String firstNucleotide()
    {
        return kmer.charAt( 0 ) + "";
    }

    public String lastNucleotide()
    {
        return kmer.charAt( kmer.length() - 1 ) + "";
    }

    @Override
    public int hashCode()
    {
        return kmer.hashCode();
    }

    @Override
    public boolean equals( final Object obj )
    {
        if ( obj instanceof Kmer )
            return ( (Kmer) obj ).kmer.equals( this.kmer );
        return false;
    }

    @Override
    public String toString()
    {
        return kmer;
    }
}
