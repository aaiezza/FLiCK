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
import java.io.OutputStream;
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

    protected CyclicBuffer<Byte>     kmerBuffer;
    protected byte []                kbuff;

    protected KmerDeBruijnGraph      deBruijn;

    protected boolean                isBuildingDeBruijn;

    protected int                    kmerSize;

    protected LongAdder              kmersRead;
    protected LongAdder              readErrors, readSplits, readGaps;

    @Override
    protected void createOutputFiles( final File fastFile, final String tempOutputDirectory )
            throws IOException
    {
        bloomfile = new BufferedOutputStream(
                new FileOutputStream( tempOutputDirectory + BLOOM_FILTER_FILE ), DEFAULT_BUFFER );
        super.createOutputFiles( fastFile, tempOutputDirectory );

        try
        {
            nfile.close();
            iupacfile.close();
        } catch ( final IOException e )
        {}
    }

    @Override
    public File deflate( final Configuration configuration, final File fileIn, final File fileOut )
    {
        kmerSize = configuration.getOption( KMER_SIZE ) + 1;
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
        deBruijn = new KmerDeBruijnGraph( kmerSize );
        isBuildingDeBruijn = true;
        kmersRead = new LongAdder();
        readErrors = new LongAdder();
        readSplits = new LongAdder();
        readGaps = new LongAdder();

        // fastIn.mark( 0 );

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

        nfile = new ByteBufferOutputStream( ByteBuffer.allocate( 0 ) )
        {
            @Override
            public void write( final int b ) throws IOException
            {
                // TODO Do I need to alter Ns to something else?
                processConventionalNucleotide();
            }

        };

        iupacfile = new BufferedOutputStream( new OutputStream()
        {
            @Override
            public void write( final int b ) throws IOException
            {
                // TODO Do I need to alter RYKMSWBDVHs to something else?
                processConventionalNucleotide();
            }
        } )
        {
            @Override
            public void close() throws IOException
            {}
        };
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
        try
        {
            final String kmerStr = getKmerBuffer();
            final Kmer kmer;

            // Write out the anchor
            if ( theRealDataHCF.position() == 0 )
                for ( final byte nb : kmerStr.substring( 0, kmerStr.length() - 1 ).getBytes() )
                    write2Bit( nb );

            if ( ( kmer = deBruijn
                    .getKmer( kmerStr.substring( 0, kmerStr.length() - 1 ) ) ) != null )
            {
                final String nextNucleotide = "" + kmerStr.charAt( kmerStr.length() - 1 );
                final Kmer nextKmer = new Kmer( kmer.suffix() + nextNucleotide );
                final Set<Kmer> successors = deBruijn.getSuccessors( kmer );
                if ( successors.size() == 0 )
                {
                    readGaps.increment();
                    deBruijn.forceAddKmer( nextKmer.getKmer() );
                    kmerBuffer.add( (byte) nextNucleotide.charAt( 0 ) );
                    return;
                }
                if ( successors.size() == 1 )
                {
                    if ( !successors.contains( nextKmer ) )
                    {
                        readErrors.increment();
                        // Probable sequencing error
                        write2Bit( (byte) nextNucleotide.charAt( 0 ) );
                    }
                    kmerBuffer.add(
                        (byte) successors.stream().findFirst().get().lastNucleotide().charAt( 0 ) );
                } else
                {
                    readSplits.increment();
                    write2Bit( (byte) nextNucleotide.charAt( 0 ) );
                    kmerBuffer.add( (byte) nextNucleotide.charAt( 0 ) );
                }
            } else write2Bit( (byte) kmerStr.charAt( kmerStr.length() - 1 ) );
            kmersRead.increment();
        } catch ( final IOException e )
        {
            System.out.println( "\n\n(IOException)" );
            printStats();
            throw e;
        }
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

        // Keep these incoming bytes for ourself
        // for building the de bruijn
        // on the first run through of the input file!
        kmerBuffer.add( dnaByte );
        if ( kmerBuffer.length() == kmerBuffer.getMaxSize() )
            if ( !deBruijn.hadEnough() )
                deBruijn.countKmer( getKmerBuffer() );
            else if ( !isBuildingDeBruijn )
                kmerMapping();

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
        if ( !deBruijn.hadEnough() )
            while ( fastIn.read() != NEWLINE )
                ;
        else seqID = super.processSequenceIdentifier();

        return seqID;
    }

    @Override
    protected void progressLineType()
    {
        super.progressLineType();

        if ( shouldStopBuildingDeBruijn() )
            stopBuildingDeBruijn();
    }

    @Override
    public void removeUnusedBufferSpace( final String tmpOutputDirectory )
            throws IOException, InterruptedException
    {
        deBruijn.writeTo( bloomfile );

        super.removeUnusedBufferSpace( tmpOutputDirectory );

        bloomfile.close();

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
        isBuildingDeBruijn = false;
        super.initializeDeflator();
        fastIn.position( 0 );
        if ( fastIn.available() > 0 )
            dnaByte = (byte) fastIn.read();
        kmerBuffer.clear();

        deBruijn.startOptimization();
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
        dnaByte = b;
        super.processConventionalNucleotide();
    }
}
