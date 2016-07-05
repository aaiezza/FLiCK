/**
 *  COPYRIGHT (C) 2015 Alex Aiezza. All Rights Reserved.
 */
package edu.rit.flick.genetics.kmer;

import static org.apache.commons.io.FileUtils.getFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Queue;
import java.util.Scanner;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.LongAdder;

import ch.qos.logback.core.helpers.CyclicBuffer;
import edu.rit.flick.genetics.FastaFileInflator;
import edu.rit.flick.genetics.nucleotide.kmer.Kmer;
import edu.rit.flick.genetics.nucleotide.kmer.KmerDeBruijnGraph;

/**
 * @author Alex Aiezza
 *
 */
public class KmerFastaFileInflator extends FastaFileInflator implements KmerFastaFileArchiver
{
    // Input files
    protected FileInputStream    bloomfile;
    protected FileInputStream    falsePositivesfile;
    protected Scanner            diffsfile;

    protected KmerDeBruijnGraph  deBruijn;

    protected int                kmerSize;

    protected CyclicBuffer<Byte> kmerBuffer;
    protected byte []            kbuff;

    protected final LongAdder    nodeCounter     = new LongAdder();
    protected long               maxNodes;

    protected long               consecDiffs     = -1;
    protected long               diffNodeIndex;
    protected Queue<Character>   diffNucleotides = new ConcurrentLinkedQueue<Character>()
    // @formatter:off
    {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isEmpty()
        {
            if ( super.isEmpty() )
                getNextDifference();
            return super.isEmpty();
        }

        @Override
        public synchronized Character peek()
        {
            isEmpty();
            return super.peek();
        }

        @Override
        public synchronized Character poll()
        {
            peek();
            final Character c = super.poll();
            if ( super.isEmpty() )
                peek();
            return c;
        }
    }; // @formatter:on

    @Override
    protected void close() throws IOException, InterruptedException
    {
        super.close();
        bloomfile.close();
        falsePositivesfile.close();
        diffsfile.close();
    }

    @SuppressWarnings ( "resource" )
    @Override
    protected void createOutputFiles( final String tempOutputDirectory, final File fastFile )
            throws IOException
    {
        super.createOutputFiles( tempOutputDirectory, fastFile );

        bloomfile = new FileInputStream( getFile( tempOutputDirectory, BLOOM_FILTER_FILE ) );
        falsePositivesfile = new FileInputStream(
                getFile( tempOutputDirectory, FALSE_POSITIVES_FILE ) );
        diffsfile = new Scanner(
                new FileInputStream( getFile( tempOutputDirectory, DIFFERENCES_FILE ) ) )
                        .useDelimiter( "\\" + PIPE );

        kmerBuffer = new CyclicBuffer<Byte>( kmerSize );
        kbuff = new byte [kmerSize];
        while ( kmerBuffer.length() != kmerBuffer.getMaxSize() )
            kmerBuffer.add( (byte) diffNucleotides.poll().charValue() );
        deBruijn = new KmerDeBruijnGraph( kmerSize );
        deBruijn.readFrom( bloomfile, falsePositivesfile );
    }


    private String getKmerBuffer()
    {
        final Byte [] bytes = kmerBuffer.asList().stream().toArray( Byte []::new );
        for ( int i = 0; i < bytes.length; i++ )
            kbuff[i] = bytes[i];

        return new String( kbuff );
    }

    protected boolean getNextDifference()
    {
        if ( datahcf.available() > 0 )
        {
            final String tetramer = byteConverter.inverse().get( (byte) datahcf.read() );
            tetramer.chars().mapToObj( n -> Character.valueOf( (char) n ) )
                    .forEachOrdered( diffNucleotides::add );
            return true;
        }
        if ( tailfile.hasNext() )
        {
            tailfile.next().chars().mapToObj( n -> Character.valueOf( (char) n ) )
                    .forEachOrdered( diffNucleotides::add );
            return true;
        }
        return false;
    }

    protected void getNextDifferenceIndex()
    {
        if ( consecDiffs > 0 )
        {
            diffNodeIndex++;
            consecDiffs--;
            return;
        }
        if ( ( nodeCounter.longValue() >= diffNodeIndex || diffNodeIndex < 0 ) &&
                diffsfile.hasNext() )
        {
            final long diffEnd;
            final StringTokenizer diffs;
            final String line = diffsfile.next().trim();
            if ( !line.isEmpty() )
            {
                diffs = new StringTokenizer( line, RANGE );
                diffNodeIndex = Long.parseLong( diffs.nextToken(), 16 );
                diffEnd = Long.parseLong( diffs.nextToken(), 16 );
                consecDiffs = diffEnd - diffNodeIndex - 1;
            }
        }
    }

    // @Override
    // protected void getNextIupacBase()
    // {}
    //
    // @Override
    // protected void getNextNs()
    // {}

    @Override
    protected boolean getNextNucleotides()
    {
        if ( nodeCounter.longValue() == 0 )
        {
            nucleotides.replace( getKmerBuffer() );
            nodeCounter.increment();
            return true;
        }

        if ( nodeCounter.longValue() > maxNodes || !fastOut.hasRemaining() )
            return false;

        final Kmer kmer;
        if ( ( kmer = new Kmer( getKmerBuffer() ) ) != null )
        {
            final Set<Kmer> successors = deBruijn.getSuccessors( kmer );
            if ( successors.size() == 1 )
            {
                final String successor = successors.stream().findFirst().get().lastNucleotide();

                if ( nodeCounter.longValue() == diffNodeIndex )
                {
                    if ( successor.equals( diffNucleotides.peek() ) )
                        throw new IllegalStateException( String.format(
                            "We are at node %d and the diffNodeIndex (%d) says we should use this (%s) nucleotide but it matches the nucleotide of the only sucessor anyways!",
                            nodeCounter.longValue(), diffNodeIndex, diffNucleotides.peek() ) );

                    // **** We are just a little different here
                    nucleotides.replace( diffNucleotides.poll() );
                    getNextDifferenceIndex();
                } else
                    // **** We have a simple path!
                    nucleotides.replace( successors.stream().findFirst().get().lastNucleotide() );

                kmerBuffer.add( (byte) successor.charAt( 0 ) );

            } else
            {
                // **** We are branching; a split
                final char nextNucleotide = diffNucleotides.poll();
                if ( successors.stream()
                        .filter( k -> k.lastNucleotide().equals( nextNucleotide + "" ) )
                        .count() <= 0 )
                    throw new IllegalStateException( String.format(
                        "Something's fishy here... The next nucleotide (%s) doesn't match any of the graph's successors from this node!\n (node #: %d)",
                        nextNucleotide + "", nodeCounter.longValue() ) );

                nucleotides.replace( "" + nextNucleotide );
                kmerBuffer.add( (byte) nextNucleotide );
            }
            nodeCounter.increment();
        }
        return true;
    }

    @Override
    protected void initializeInflator()
    {
        diffNodeIndex = -1;
        nodeCounter.reset();

        super.initializeInflator();

        getNextDifference();
        getNextDifferenceIndex();
    }

    @Override
    protected void parseProperties()
    {
        super.parseProperties();
        kmerSize = Integer.parseInt( (String) metafile.get( META_KMER_SIZE ) );
        maxNodes = Long.parseLong( (String) metafile.get( META_NODES_HIT ) );
    }
}
