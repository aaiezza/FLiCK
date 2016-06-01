package edu.rit.flick.genetics.nucleotide.kmer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.event.GraphVertexChangeEvent;
import org.jgrapht.graph.ListenableDirectedGraph;

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
    private Map<Kmer, LongAdder>                  kmers;

    // TODO Consider making this a list, since edges are no long computed and
    // added
    private DirectedGraph<Kmer, KmerEdge>         kmerGraph;
    private ConnectivityInspector<Kmer, KmerEdge> kmerGraphInspector;

    private Kmer                                  anchor;

    private BloomFilter<CharSequence>             kmerBloom;
    private Funnel<CharSequence>                  kmerFunnel;

    // I've had enough kmers! It is time to build a graph!
    private boolean                               hadEnough = false;

    private final LongAdder                       sum       = new LongAdder();

    private int                                   kmerSize;

    private long                                  sampleSize;

    public KmerDeBruijnGraph( final int kmerSize )
    {
        this.kmerSize = kmerSize;
        kmers = Collections.synchronizedMap( new HashMap<Kmer, LongAdder>() );
        sampleSize = (long) Math.pow( 4, kmerSize - 1 );

        kmerFunnel = Funnels.stringFunnel( Charset.defaultCharset() );

        final ListenableDirectedGraph<Kmer, KmerEdge> kmerGraph = new ListenableDirectedGraph<Kmer, KmerEdge>(
                KmerEdge.class )
        {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean containsVertex( final Kmer kmer )
            {
                if ( kmerBloom.mightContain( kmer.getKmer() ) )
                    return super.containsVertex( kmer );
                return false;
            }

        };
        this.kmerGraph = kmerGraph;

        kmerGraphInspector = new ConnectivityInspector<Kmer, KmerEdge>( this.kmerGraph )
        {
            // Just look up successors with the generated prefixes!
            @Override
            public void vertexAdded( final GraphVertexChangeEvent<Kmer> e )
            {
                final Kmer newKmer = e.getVertex();
                kmerBloom.put( newKmer.getKmer() );
                super.vertexAdded( e );
            }
        };
        kmerGraph.addGraphListener( kmerGraphInspector );
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
        kmerBloom = BloomFilter.create( kmerFunnel, kmerLimit, 1e-30 );

        kmers.entrySet().stream()
                .sorted( ( e1, e2 ) -> e1.getValue().intValue() - e2.getValue().intValue() )
                .filter( e -> e.getValue().longValue() > Tsol ).limit( kmerLimit )
                .map( e -> e.getKey() ).forEach( kmerGraph::addVertex );

        // Make sure we have the anchor
        kmerGraph.addVertex( anchor );

        printStats( Tsol, kmerLimit );
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

            if ( sum.longValue() == 1 )
                // We just always want the first one in in our graph
                anchor = kmer;

            if ( sum.longValue() >= sampleSize )
                startOptimization();
        }

        return hadEnough();
    }

    public void forceAddKmer( final String kmerStr )
    {
        if ( kmerStr.length() != kmerSize - 1 )
            throw new IllegalArgumentException(
                    String.format( "Can't accept kmer that is not size %d", kmerSize - 1 ) );

        kmerGraph.addVertex( new Kmer( kmerStr ) );
    }

    public void freeChildren( final Kmer baronKmer )
    {
        final int i = 2;
        // for ( int i = 0; i < baronKmer.size() - 1; i++ )
        // {
        final String suffix = baronKmer.suffix().substring( i );
        final Optional<Kmer> descendent = kmerGraph.vertexSet().parallelStream()
                .filter( pk -> pk.getKmer().startsWith( suffix ) ).findAny();

        if ( !descendent.isPresent() )
            // continue;
            return;

        Kmer parent = null;
        Kmer baby = descendent.get();
        for ( int m = 0; m <= i; m++ )
        {
            parent = new Kmer( baronKmer.getKmer().substring( i - m ) +
                    baby.getKmer().substring( baby.size() - i + m - 1, baby.size() - 1 ) );
            kmerGraph.addVertex( baby );
            baby = parent;
        }
        kmerGraph.addVertex( baby );
        // break;
        // }
    }

    /**
     * @return the anchor
     */
    public Kmer getAnchor()
    {
        return anchor;
    }

    public long getBaronNodes()
    {
        return kmerGraph.vertexSet().parallelStream().filter( k -> getSuccessors( k ).isEmpty() )
                .count();
    }

    public Kmer getKmer( final Kmer kmer )
    {
        if ( kmerGraph.containsVertex( kmer ) )
            return kmerGraph.vertexSet().parallelStream().filter( kmer::equals ).findFirst()
                    .orElse( null );
        else return null;
    }

    public Kmer getKmer( final String kmer )
    {
        return getKmer( new Kmer( kmer ) );
    }

    public long getKmersCounted()
    {
        return sum.longValue();
    }

    public long getKmerTerminalNodes()
    {
        return kmers.keySet().parallelStream().filter( k -> kmers.keySet().parallelStream()
                .filter( k2 -> k.suffix().equals( k2.prefix() ) ).count() > 0 ).count();
    }

    public Set<Kmer> getSuccessors( final Kmer kmer )
    {
        return Stream.of( Nucleotide.A, Nucleotide.C, Nucleotide.G, Nucleotide.T )
                .map( n -> new Kmer( kmer.suffix() + (char) n.byteValue() ) )
                .filter( kmerGraph::containsVertex ).collect( Collectors.toSet() );
    }

    public long getTerminalNodes()
    {
        return kmerGraph.vertexSet().parallelStream().filter( k -> kmerGraph.outDegreeOf( k ) < 0 )
                .count();
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

    public boolean hasKmer( final Kmer kmer )

    {
        return kmerBloom.mightContain( kmer.getKmer() );
    }

    public Set<Kmer> possibleSuccessors( final Kmer kmer )
    {
        return Stream.of( Nucleotide.A, Nucleotide.C, Nucleotide.G, Nucleotide.T ).map( n -> {
            return new Kmer( kmer.suffix() + (char) n.byteValue() );
        } ).filter( k -> {
            return kmerBloom.mightContain( k.getKmer() );
        } ).collect( Collectors.toSet() );
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
            kmerGraph.vertexSet().size() );
        System.out.printf( " Tsol: %d\n Kmer limit: %d\n\n", Tsol, kmerLimit );
    }

    public void readFrom( final InputStream bloomfile ) throws IOException
    {
        kmerBloom = BloomFilter.readFrom( bloomfile, kmerFunnel );
    }

    public void setAnchor( final String anchorStr )
    {
        anchor = new Kmer( anchorStr.substring( 0, anchorStr.length() - 1 ) );
        kmerBloom.put( anchor.getKmer() );
        kmerGraph.addVertex( anchor );
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
        final StringBuilder out = new StringBuilder();
        if ( kmerGraph != null )
            out.append( String.format( "Graph Size: %d (Baron: %d, Terminal: %d) | ",
                kmerGraph.vertexSet().size(), getBaronNodes(), getTerminalNodes() ) );
        if ( kmers != null )
            out.append( String.format( "Kmer Counter Size: %d ~ ", kmers.size() ) );
        out.append( String.format( "Counted Kmers: %d", sum.longValue() ) );
        return out.toString();
    }

    public void writeTo( final OutputStream bloomfile ) throws IOException
    {
        kmerBloom.writeTo( bloomfile );
    }
}
