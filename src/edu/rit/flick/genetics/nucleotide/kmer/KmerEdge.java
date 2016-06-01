package edu.rit.flick.genetics.nucleotide.kmer;

import org.jgrapht.graph.DefaultEdge;

public class KmerEdge extends DefaultEdge
{
    private static final long serialVersionUID = 1L;

    @Override
    public Kmer getSource()
    {
        return (Kmer) super.getSource();
    }

    @Override
    public Kmer getTarget()
    {
        return (Kmer) super.getTarget();
    }

    // TODO Override equals!

}
