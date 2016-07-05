/**
 *  COPYRIGHT (C) 2015 Alex Aiezza. All Rights Reserved.
 */
package edu.rit.flick.genetics.kmer;

import static edu.rit.flick.genetics.kmer.config.KmerFastaDeflationOptionSet.KMER_SIZE;
import static java.lang.String.format;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.concurrent.atomic.LongAdder;

import ch.qos.logback.core.helpers.CyclicBuffer;
import edu.rit.flick.config.Configuration;
import edu.rit.flick.genetics.FastaFileDeflator;
import edu.rit.flick.genetics.nucleotide.kmer.Kmer;
import edu.rit.flick.genetics.nucleotide.kmer.KmerDeBruijnGraph;
import edu.rit.flick.genetics.util.ByteBufferOutputStream;

/**
 * @author Alex Aiezza
 *
 */
public class KmerFastaFileDeflator extends FastaFileDeflator implements KmerFastaFileArchiver
{
    protected ByteBufferOutputStream theRealDataHCF;

    protected BufferedOutputStream   bloomfile;
    protected BufferedOutputStream   falsePositivesfile;
    protected BufferedOutputStream   diffsfile;

    protected CyclicBuffer<Byte>     kmerBuffer;
    protected byte []                kbuff;

    protected KmerDeBruijnGraph      deBruijn;

    protected boolean                isBuildingDeBruijn;
    protected boolean                startedWritingNucleotides;

    protected boolean                writingDifferenceRange;

    protected int                    kmerSize;

    protected LongAdder              kmersRead;
    protected LongAdder              readErrors, readSplits, readGaps;

    @Override
    protected void createOutputFiles( final File fastFile, final String tempOutputDirectory )
            throws IOException
    {
        bloomfile = new BufferedOutputStream(
                new FileOutputStream( tempOutputDirectory + BLOOM_FILTER_FILE ), DEFAULT_BUFFER );
        falsePositivesfile = new BufferedOutputStream(
                new FileOutputStream( tempOutputDirectory + FALSE_POSITIVES_FILE ),
                DEFAULT_BUFFER );
        diffsfile = new BufferedOutputStream(
                new FileOutputStream( tempOutputDirectory + DIFFERENCES_FILE ), DEFAULT_BUFFER );

        super.createOutputFiles( fastFile, tempOutputDirectory );

        // try
        // {
        // nfile.close();
        // iupacfile.close();
        // } catch ( final IOException e )
        // {}

        /*
         * Here's the magic. Let this variable dictate when things actually get
         * written to files, or if we should just do 'dummy' traversal.
         */
        /*
         * Since we want to redefine what the nucleotides file actually
         * contains, we can hide the actual reference in this deflator instance
         * and hijack what happens in the super class's calls to the datahcf put
         * method with our own sneaky implementation.
         */
        theRealDataHCF = datahcf;
        // Wrapper of actual mapped nucleotide file
        datahcf = new ByteBufferOutputStream( ByteBuffer.allocate( 0 ) )
        {
            @Override
            public synchronized void close() throws IOException
            {
                super.close();
                theRealDataHCF.close();
                theRealDataHCF = null;
            }

            @Override
            public long position()
            {
                return theRealDataHCF.position();
            }

            @Override
            public void put( final byte b ) throws IOException
            {
                theRealDataHCF.put( b );
            }
        };

        // nfile = new ByteBufferOutputStream( ByteBuffer.allocate( 0 ) )
        // {
        // @Override
        // public void write( final int b ) throws IOException
        // {
        // // TODO Do I need to alter Ns to something else and maybe still
        // // write everything out?
        // processConventionalNucleotide();
        // }
        //
        // };
        //
        // iupacfile = new BufferedOutputStream( new OutputStream()
        // {
        // @Override
        // public void write( final int b ) throws IOException
        // {
        // // TODO Do I need to alter RYKMSWBDVHs to something else?
        // processConventionalNucleotide();
        // }
        // } )
        // {
        // @Override
        // public void close() throws IOException
        // {}
        // };

        isBuildingDeBruijn = true;
        startedWritingNucleotides = false;

        deBruijn = new KmerDeBruijnGraph( kmerSize );
    }

    @Override
    public File deflate( final Configuration configuration, final File fileIn, final File fileOut )
    {
        kmerSize = configuration.getOption( KMER_SIZE );
        return super.deflate( configuration, fileIn, fileOut );
    }

    private String getKmerBuffer()
    {
        final Byte [] bytes = kmerBuffer.asList().stream().toArray( Byte []::new );
        for ( int i = 0; i < bytes.length; i++ )
            kbuff[i] = bytes[i];

        return new String( kbuff );
    }

    @Override
    protected void initializeDeflator()
    {
        // Just business as usual, let the super class do its thing
        super.initializeDeflator();

        kmerBuffer = new CyclicBuffer<Byte>( kmerSize );
        kbuff = new byte [kmerSize];
        writingDifferenceRange = false;
        kmersRead = new LongAdder();
        readErrors = new LongAdder();
        readSplits = new LongAdder();
        readGaps = new LongAdder();
    }

    /**
     * This method should only be accessed after the de bruijn graph is created
     * and ready for querying.
     *
     * The deflator works to use the super class's protected field,
     * {@link edu.rit.flick.genetics.FastFileDeflator#dnaByte}, and store it in
     * our cyclic kmerBuffer which is done in our overriding implementation of
     * the {@link KmerFastaFileDeflator#processConventionalNucleotide} method.
     * <br/>
     * <br/>
     * This method now seeks to fill the compressed data file with only the
     * differences from the de bruijn graph reference as they appear from the
     * anchor kmer (which is stored in the meta file in the overriding
     * implementation of the {@link KmerFastaFileDeflator#processProperties}
     * method).
     *
     * @throws IOException
     *             When the datahcf file does not want to be written to. Reasons
     *             for this:
     *             <ul>
     *             <li>File not mapped correctly to byte buffer</li>
     *             <li>The imposed file limit may have been reached incidentally
     *             </li>
     *             </ul>
     */
    protected void kmerMapping() throws IOException
    {
        final String kmerStr = getKmerBuffer();
        final Kmer kmer;
        final String nextNucleotide = "" + (char) dnaByte;

        // Write out the anchor
        if ( datahcf.position() == 0 )
            for ( final byte nb : kmerStr.getBytes() )
                write2Bit( nb );

        if ( ( kmer = deBruijn.getKmer( kmerStr ) ) != null )
        {
            kmersRead.increment();

            final Kmer nextKmer = new Kmer( kmer.suffix() + nextNucleotide );
            final Set<Kmer> successors = deBruijn.getSuccessors( kmer, true );
            if ( successors.size() == 0 )
            {
                // **** No successors means we fill in the gap so at least
                // there's something there
                readGaps.increment();
                deBruijn.addKmer( nextKmer.getKmer() );
                kmerBuffer.add( (byte) nextNucleotide.charAt( 0 ) );
                return;
            }
            if ( successors.size() == 1 )
            {
                if ( !successors.contains( nextKmer ) )
                {
                    readErrors.increment();
                    // **** Probable sequencing error; Mark Difference
                    write2Bit( (byte) nextNucleotide.charAt( 0 ) );
                    processDifference();
                } else if ( writingDifferenceRange )
                {
                    final String diffPositionStr = Long.toHexString( kmersRead.longValue() )
                            .toUpperCase() + PIPE;
                    diffsfile.write( diffPositionStr.getBytes() );
                    writingDifferenceRange = false;
                }

                kmerBuffer.add(
                    (byte) successors.stream().findFirst().get().lastNucleotide().charAt( 0 ) );
            } else
            {
                // **** Mark a split in the graph
                readSplits.increment();
                write2Bit( (byte) nextNucleotide.charAt( 0 ) );
                kmerBuffer.add( (byte) nextNucleotide.charAt( 0 ) );

                if ( writingDifferenceRange )
                {
                    final String diffPositionStr = Long.toHexString( kmersRead.longValue() )
                            .toUpperCase() + PIPE;
                    diffsfile.write( diffPositionStr.getBytes() );
                    writingDifferenceRange = false;
                }
            }
        } else
        {
            throw new IllegalStateException( String.format(
                "While deflating at node (%d), we encounter no kmer not in the debruijn to kmer (%s)!",
                kmersRead.longValue(), kmerStr ) );
        }
    }

    protected void processDifference() throws IOException
    {
        if ( !startedWritingNucleotides )
            return;

        if ( !writingDifferenceRange )
        {
            writingDifferenceRange = true;

            final String diffPositionStr = Long.toHexString( kmersRead.longValue() ).toUpperCase() +
                    RANGE;
            diffsfile.write( diffPositionStr.getBytes() );
        }
    }

    @Override
    protected void processTail() throws IOException
    {
        if ( writingDifferenceRange )
        {
            final String diffPositionStr = Long.toHexString( kmersRead.longValue() + 1 )
                    .toUpperCase();
            diffsfile.write( diffPositionStr.getBytes() );
            writingDifferenceRange = false;
        }

        super.processTail();
    }

    private void printStats()
    {
        System.out.printf(
            "Kmers Read: %d\nRead Errors: %d\nRead Splits: %d\nGaps in graph: %d\nDe Bruijn by end: %s\n\n",
            kmersRead.longValue(), readErrors.longValue(), readSplits.longValue(),
            readGaps.longValue(), deBruijn );
    }

    /**
     * This method gives us an interesting perspective into how this compressor
     * works. We funnel ALL IUPAC into this spot for nucleotide processing
     * suddenly making them... conventional!
     */
    @Override
    protected boolean processConventionalNucleotide() throws IOException
    {
        if ( shouldStopBuildingDeBruijn() )
            return false;

        if ( !isBuildingDeBruijn )
        {
            if ( kmerBuffer.length() < kmerBuffer.getMaxSize() )
            {
                kmerBuffer.add( dnaByte );
            } else kmerMapping();
        } else
        {
            // Keep these incoming bytes for ourself
            // for building the de bruijn
            // on the first run through of the input file!
            kmerBuffer.add( dnaByte );
            if ( kmerBuffer.length() == kmerBuffer.getMaxSize() && !deBruijn.hadEnough() )
                deBruijn.countKmer( getKmerBuffer() );
        }

        if ( fastIn.available() <= 0 )
            return false;

        return true;
    }

    @Override
    protected void processProperties() throws IOException
    {
        super.processProperties();

        metafile.write( format( META_KMER_SIZE_FORMAT, kmerSize ) );
        metafile.write( format( META_NODES_HIT_FORMAT, kmersRead.longValue() ) );

        // TODO DEBUGging
        printStats();
    }

    @Override
    protected String processSequenceIdentifier() throws IOException
    {
        String seqID = null;
        if ( !startedWritingNucleotides )
            while ( fastIn.read() != NEWLINE )
                ;
        else seqID = super.processSequenceIdentifier();

        return seqID;
    }

    @Override
    protected void progressLineType()
    {
        super.progressLineType();

        if ( isBuildingDeBruijn )
        {
            if ( shouldStopBuildingDeBruijn() )
                stopBuildingDeBruijn();
        } else if ( fastIn.available() <= 0 && !startedWritingNucleotides )
            startWritingNucleotides();
    }

    @Override
    public void removeUnusedBufferSpace( final String tmpOutputDirectory )
            throws IOException, InterruptedException
    {
        deBruijn.writeTo( bloomfile, falsePositivesfile );

        super.removeUnusedBufferSpace( tmpOutputDirectory );

        bloomfile.close();
        diffsfile.close();
        falsePositivesfile.close();

        /*
         * I would like to delete these, but it definitely upsets the Inflator
         *
         * final File nFile = new File( tmpOutputDirectory + N_FILE ); final
         * File iupacFile = new File( tmpOutputDirectory + IUPAC_CODE_FILE );
         *
         * FileUtils.deleteQuietly( nFile ); FileUtils.deleteQuietly( iupacFile
         * );
         */
    }

    /*
     * Here is where we check if we are done building the De Bruijn and put
     * everything back to normal
     */
    protected boolean shouldStopBuildingDeBruijn()
    {
        return isBuildingDeBruijn && ( deBruijn.hadEnough() || fastIn.available() <= 0 );
    }

    protected void stopBuildingDeBruijn()
    {
        initializeDeflator();
        fastIn.position( 0 );
        if ( fastIn.available() > 0 )
            dnaByte = (byte) fastIn.read();

        isBuildingDeBruijn = false;
        deBruijn.startOptimization();
    }

    protected void startWritingNucleotides()
    {
        initializeDeflator();
        fastIn.position( 0 );
        if ( fastIn.available() > 0 )
            dnaByte = (byte) fastIn.read();

        startedWritingNucleotides = true;
        deBruijn.finalizeGraph();
    }

    /**
     * Keeping in line with the 2-bit compression method, we might as well make
     * the attempt to compress even further the nucleotides stored as
     * differences from the reference.
     *
     * @param b
     * @throws IOException
     */
    protected void write2Bit( final byte b ) throws IOException
    {
        if ( !startedWritingNucleotides )
            return;
        dnaByte = b;
        super.processConventionalNucleotide();
    }
}
