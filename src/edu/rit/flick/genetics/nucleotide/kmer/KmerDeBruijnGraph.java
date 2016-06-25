package edu.rit.flick.genetics.nucleotide.kmer;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Alex Aiezza
 *
 */
public class KmerDeBruijnGraph
{
    private Map<Kmer, LongAdder> kmers;

    private Set<Kmer>            graph;
    private Kmer                 anchor;

    // I've had enough kmers! It is time to build a graph!
    private boolean              hadEnough = false;

    private final LongAdder      sum       = new LongAdder();

    // Some sort of optimization for maintaining the size of the kmer counter.
    // TODO Might want to actual NOT read through whole file on first run, but
    // rather do it the second time around considering that optimization here
    // may lead to fewer reads needing to be taken in initially.
    //
    private long                 Tsol      = 3;

    public KmerDeBruijnGraph()
    {
        super();
        this.kmers = Collections.synchronizedMap( new HashMap<Kmer, LongAdder>() );
        this.graph = Collections.synchronizedSet( new HashSet<Kmer>() );
    }

    private void buildKmerLibrary()
    {
        Tsol = Long.min( (long) ( 0.05 * sum.longValue() ), (long) kmers.values().parallelStream()
                .mapToLong( kc -> kc.longValue() ).average().getAsDouble() );
        final int kmerLimit = (int) ( kmers.size() * 0.25 );
        graph = Collections.unmodifiableSet( Stream
                .concat( Stream.of( anchor ),
                    kmers.entrySet().stream().filter( e -> e.getValue().longValue() >= Tsol )
                            .limit( kmerLimit ).map( e -> e.getKey() ) )
                .collect( Collectors.toSet() ) );

        System.out.printf( "Average #ofKmers: %f\n", kmers.values().parallelStream()
                .mapToLong( kc -> kc.longValue() ).average().getAsDouble() );
        final long maxKmer = kmers.values().parallelStream().mapToLong( kc -> kc.longValue() ).max()
                .getAsLong();
        System.out
                .printf( "Max occurring Kmer: %s - %d\n",
                    kmers.entrySet().stream().filter( e -> e.getValue().longValue() == maxKmer )
                            .map( e -> e.getKey().getKmer() ).collect( Collectors.toSet() ),
                    maxKmer );
        System.out.printf( "# of total kmers: %d\n", kmers.size() );
        System.out.printf( "# of kmers that made it into the graph: %d\n", graph.size() );
        // TODO there might be unconnected children in here! Heads up!

        // Don't need you anymore!
        kmers = null;
    }

    /**
     * Add kmer to list of counted kmers. If it already has been counted before,
     * the count is incremented. If it has not, the given kmer is added to the
     * map of kmers with a count of 1.
     * 
     * @param kmer
     * @return
     */
    public boolean countKmer( final String kmerStr )
    {
        final Kmer kmer = new Kmer( kmerStr.substring( 0, kmerStr.length() - 1 ) );
        if ( !hadEnough() )
        {
            kmers.computeIfAbsent( kmer, k -> new LongAdder() ).increment();
            sum.increment();

            kmers.keySet().parallelStream().forEach( pkmer -> {
                if ( kmer.prefix().equals( pkmer.suffix() ) )
                    pkmer.addChild( kmer );
                else if ( kmer.suffix().equals( pkmer.prefix() ) )
                    kmer.addChild( pkmer );
            } );

            if ( sum.longValue() == 1 )
                // Gaurunteed beginning anchor!
                anchor = kmer;

            // TODO based on some optimization, check to see if more kmers are
            // necessary.
            // Right now, this is just a flat size restriction
            if ( kmers.size() >= Integer.MAX_VALUE / 128 )
                startOptimization();
        }

        return hadEnough();
    }

    public long kmerCount( final Kmer kmer )
    {
        return kmers.get( kmer ).longValue();
    }

    public long kmerCount( final String kmer )
    {
        return kmerCount( new Kmer( kmer ) );
    }

    public Kmer getKmer( final Kmer kmer )
    {
        // TODO Do I return something special if an asked-for kmer is not found.
        return graph.stream().filter( kmer::equals ).findFirst().orElse( null );
    }

    public long getKmersCounted()
    {
        return sum.longValue();
    }

    public Kmer getKmer( final String kmer )
    {
        return getKmer( new Kmer( kmer.substring( 0, kmer.length() - 1 ) ) );
    }

    /**
     * Whilst iterating over kmers initially for the counts, a small sample may
     * be all that is needed to optimize the graph. If that threshold is
     * reached, don't bother continuing. The deflator using this graph object
     * may check this itself, or simply respond to the boolean that is also
     * returned from this same method from the
     * {@link KmerDeBruijnGraph#countKmer(String) countKmer(kmer)} method to
     * know when to begin actual decompression.
     * 
     * @return
     */
    public boolean hadEnough()
    {
        return hadEnough;
    }

    /**
     * Should be called by the deflator if no more kmers are available to read
     * in.
     */
    public void startOptimization()
    {
        if ( hadEnough )
            return;
        hadEnough = true;
        buildKmerLibrary();
    }

    @Override
    public String toString()
    {
        // TODO Serialization of this graph!
        // return graph.toString();
        return "Nice graph bro!";
    }
}
