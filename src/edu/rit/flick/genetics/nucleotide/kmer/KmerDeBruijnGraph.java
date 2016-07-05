package edu.rit.flick.genetics.nucleotide.kmer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnel;
import com.google.common.hash.Funnels;

import edu.rit.flick.genetics.nucleotide.Nucleotide;

/**
 * @author Alex Aiezza
 *
 */
public class KmerDeBruijnGraph
{
    protected static final String     KMER_SIZE_EXCEPTION_FORMAT = "Can't accept kmer that is not size %d";

    private Map<Kmer, LongAdder>      kmers;

    private Kmer                      anchor;

    private Set<Kmer>                 anchors;
    private Set<String>               falsePositives;
    private Set<Kmer>                 connectingKmers;

    private BloomFilter<CharSequence> kmerBloom;
    private Funnel<CharSequence>      kmerFunnel;

    // I've had enough kmers! It is time to build a graph!
    private boolean                   hadEnough                  = false;
    private boolean                   finalized                  = false;

    private final LongAdder           sum                        = new LongAdder();

    private int                       kmerSize;

    private long                      sampleSize;

    public KmerDeBruijnGraph( final int kmerSize )
    {
        this.kmerSize = kmerSize;
        kmers = Collections.synchronizedMap( new HashMap<Kmer, LongAdder>() );
        sampleSize = (long) Math.pow( 4, kmerSize );

        falsePositives = new HashSet<String>();
        anchors = new HashSet<Kmer>();
        connectingKmers = new HashSet<Kmer>();

        kmerFunnel = Funnels.stringFunnel( Charset.defaultCharset() );
    }

    private void buildKmerLibrary()
    {
        // TODO Switch is for debugging
        switch ( "average" )
        {
        case "minOf3":
            buildKmerLibrary( Long.max( (long) ( 1e-6 * sum.longValue() ), 3 ),
                (int) ( kmers.size() * 0.25 ) );
            break;
        case "average":
            buildKmerLibrary(
                Long.max( 3, (long) kmers.values().parallelStream()
                        .mapToLong( kc -> kc.longValue() ).average().getAsDouble() ),
                (int) ( kmers.size() * 0.25 ) );
            break;
        }
    }

    private void buildKmerLibrary( final long Tsol, final int kmerLimit )
    {
        // Make bloom filter with 10% FPP
        kmerBloom = BloomFilter.create( kmerFunnel, kmerLimit, 1e-2 );

        // Get solid kmers
        kmers.entrySet().stream()
                .sorted( ( e1, e2 ) -> e2.getValue().intValue() - e1.getValue().intValue() )
                .filter( e ->
                {
                    return e.getValue().longValue() >= Tsol;
                } ).limit( kmerLimit ).map( e -> e.getKey() ).forEach( k -> {
                    anchors.add( k );
                    kmerBloom.put( k.getKmer() );
                } );

        // Make sure we have the first anchor
        kmerBloom.put( anchor.getKmer() );
        anchors.add( anchor );

        printStats( Tsol, kmerLimit );
        // Don't need you anymore!
        // kmers = null;
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
        final Kmer kmer = new Kmer( kmerStr );
        if ( !hadEnough() )
        {
            kmers.computeIfAbsent( kmer, k -> new LongAdder() ).increment();
            sum.increment();

            if ( sum.longValue() == 1 )
                // We just always want the first one in our graph
                anchor = kmer;

            if ( sum.longValue() >= sampleSize )
                startOptimization();
        }

        return hadEnough();
    }

    public void addKmer( final String kmerStr )
    {
        if ( kmerStr.length() != kmerSize )
            throw new IllegalArgumentException(
                    String.format( KMER_SIZE_EXCEPTION_FORMAT, kmerSize ) );

        kmerBloom.put( kmerStr );
        connectingKmers.add( new Kmer( kmerStr ) );
        falsePositives.remove( kmerStr );
        falsePositives.addAll( new Kmer( kmerStr ).getPossibleSuccessors().stream()
                .map( k -> k.getKmer() ).collect( Collectors.toSet() ) );
    }

    /**
     * @return the anchor
     */
    public Kmer getAnchor()
    {
        return anchor;
    }

    public Kmer getKmer( final Kmer kmer )
    {
        // Track kmers that are not anchors and are not connectingKmers but that
        // the bloom filter says is in our de bruijn graph.
        // These are false positives.
        if ( kmerBloom.mightContain( kmer.getKmer() ) )
        {
            if ( !anchors.contains( kmer ) && !connectingKmers.contains( kmer ) )
            {
                falsePositives.add( kmer.getKmer() );
                return null;
            } else return kmer;
        } else return null;
    }

    public Kmer getKmer( final String kmer )
    {
        return getKmer( new Kmer( kmer ) );
    }

    public long getKmersCounted()
    {
        return sum.longValue();
    }

    public boolean contains( final Kmer kmer )
    {
        return kmerBloom.mightContain( kmer.getKmer() ) &&
                !falsePositives.contains( kmer.getKmer() );
    }

    public long getKmerTerminalNodes()
    {
        return kmers.keySet().parallelStream().filter( k -> kmers.keySet().parallelStream()
                .filter( k2 -> k.suffix().equals( k2.prefix() ) ).count() > 0 ).count();
    }

    public void freeChildren( final Kmer terminalKmer )
    {
        for ( int i = 0; i < terminalKmer.size() - 1; i++ )
        {
            final String suffix = terminalKmer.suffix().substring( i );
            final Optional<Kmer> descendent = Stream
                    .concat( anchors.stream(), connectingKmers.stream() )
                    .filter( k -> !getSuccessors( k ).isEmpty() )
                    .filter( pk -> pk.getKmer().startsWith( suffix ) ).findAny();

            if ( !descendent.isPresent() )
                continue;

            Kmer parent = null;
            Kmer baby = descendent.get();
            for ( int m = 0; m <= i; m++ )
            {
                parent = new Kmer( terminalKmer.getKmer().substring( i - m ) +
                        baby.getKmer().substring( baby.size() - i + m - 1, baby.size() - 1 ) );
                addKmer( baby.getKmer() );
                baby = parent;
            }
            addKmer( baby.getKmer() );
            break;
        }
    }

    public Set<Kmer> getSuccessors( final Kmer kmer )
    {
        final Set<Kmer> successors = kmer.getPossibleSuccessors().stream().filter( this::contains )
                .collect( Collectors.toSet() );
        if ( successors.size() <= 0 && kmers != null )
            return kmer.getPossibleSuccessors().stream()
                    .filter( k -> kmerBloom.mightContain( k.getKmer() ) )
                    .collect( Collectors.toSet() );
        return successors;
    }

    public Set<Kmer> getSuccessors( final Kmer kmer, final boolean deflating )
    {
        if ( finalized )
            return getSuccessors( kmer );

        // final Map<Boolean, Set<Kmer>> successors = Stream
        return Stream.of( Nucleotide.A, Nucleotide.C, Nucleotide.G, Nucleotide.T )
                .map( n -> new Kmer( kmer.suffix() + (char) n.byteValue() ) )
                .filter( k -> anchors.contains( k ) || connectingKmers.contains( k ) )
                .collect( Collectors.toSet() );


        // .collect( Collectors.partitioningBy( this::contains,
        // Collectors.toSet() ) );
        //
        // successors.get( false ).stream().map( k -> k.getKmer() ).forEach(
        // falsePositives::add );
        // return successors.get( true );
    }

    public long getTerminalNodes()
    {
        return Stream.concat( anchors.stream(), connectingKmers.stream() )
                .filter( k -> getSuccessors( k ).isEmpty() ).count();
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

    public boolean isFinalized()
    {
        return finalized;
    }

    private void printStats( final long Tsol, final int kmerLimit )
    {
        System.out.printf( "Average Kmer Frequency: %f\n", kmers.values().parallelStream()
                .mapToLong( kc -> kc.longValue() ).average().getAsDouble() );
        final long maxKmer = kmers.values().parallelStream().mapToLong( kc -> kc.longValue() ).max()
                .getAsLong();
        final Set<String> highestFreqKmers = kmers.entrySet().stream()
                .filter( e -> e.getValue().longValue() == maxKmer ).map( e -> e.getKey().getKmer() )
                .collect( Collectors.toSet() );
        System.out.printf( "Highest Kmer Frequency: %d - %s\n  %d - %.2f%%\n", maxKmer,
            highestFreqKmers, highestFreqKmers.size(),
            highestFreqKmers.size() / (float) kmers.size() * 100 );
        System.out.printf( "# of total kmers: %d\n", kmers.size() );
        System.out.printf( "# of kmers that made it into the graph: %d\n",
            anchors.size() + connectingKmers.size() );
        System.out.printf( " Tsol: %d\n Kmer limit: %d\n\n", Tsol, kmerLimit );
    }

    public void setAnchor( final String anchorStr )
    {
        if ( anchorStr.length() != kmerSize )
            throw new IllegalArgumentException(
                    String.format( KMER_SIZE_EXCEPTION_FORMAT, kmerSize ) );

        anchor = new Kmer( anchorStr );
        kmerBloom.put( anchorStr );
        falsePositives.remove( anchorStr );
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

    public void finalizeGraph()
    {
        Stream.concat( anchors.stream(), connectingKmers.stream() )
                .filter( k -> getSuccessors( k, true ).isEmpty() ).collect( Collectors.toSet() )
                .forEach( this::freeChildren );

        kmers.keySet().parallelStream().map( k -> k.getKmer() )
                .filter( k -> kmerBloom.mightContain( k ) && !anchors.contains( new Kmer( k ) ) &&
                        !connectingKmers.contains( new Kmer( k ) ) )
                .forEach( falsePositives::add );

        falsePositives.removeIf( k -> !kmerBloom.mightContain( k ) );
        Stream.concat( anchors.stream(), connectingKmers.stream() )
                .filter( falsePositives::contains ).forEach( falsePositives::remove );

        finalized = true;
    }

    @Override
    public String toString()
    {
        final StringBuilder out = new StringBuilder();
        out.append( String.format( "Graph Size: %d (Terminal: %d) | ",
            anchors.size() + connectingKmers.size(), getTerminalNodes() ) );
        if ( kmers != null )
            out.append( String.format( "Kmer Counter Size: %d ~ ", kmers.size() ) );
        out.append( String.format( "Counted Kmers: %d", sum.longValue() ) );
        return out.toString();
    }

    public void writeTo( final OutputStream bloomfile, final OutputStream falsePositivesfile )
            throws IOException
    {
        kmerBloom.writeTo( bloomfile );

        for ( final String kmer : falsePositives )
            falsePositivesfile.write( ( kmer + "\n" ).getBytes() );
    }

    public void readFrom( final InputStream bloomfile, final InputStream falsePositivesfile )
            throws IOException
    {
        kmerBloom = BloomFilter.readFrom( bloomfile, kmerFunnel );
        try ( final Scanner sc = new Scanner( falsePositivesfile ) )
        {
            while ( sc.hasNextLine() )
                falsePositives.add( sc.nextLine() );
        }
    }
}
