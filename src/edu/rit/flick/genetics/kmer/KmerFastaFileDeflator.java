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
    protected ByteBufferOutputStream theRealNFile;
    protected BufferedOutputStream   theRealIupacFile;

    protected BufferedOutputStream   debruijnfile;
    protected BufferedOutputStream   differencefile;

    protected CyclicBuffer<Byte>     kmerBuffer;
    protected byte []                kbuff;

    protected KmerDeBruijnGraph      deBruijn;

    protected boolean                isBuildingDeBruijn;

    protected int                    kmerSize;

    @Override
    public File deflate( final Configuration configuration, final File fileIn, final File fileOut )
    {
        kmerSize = configuration.getOption( KMER_SIZE );
        return super.deflate( configuration, fileIn, fileOut );
    }

    @Override
    protected void createOutputFiles( final File fastFile, final String tempOutputDirectory )
            throws IOException
    {
        debruijnfile = new BufferedOutputStream(
                new FileOutputStream( tempOutputDirectory + DE_BRUIJN_FILE ), DEFAULT_BUFFER );
        differencefile = new BufferedOutputStream(
                new FileOutputStream( tempOutputDirectory + DIFFERENCE_FILE ), DEFAULT_BUFFER );
        super.createOutputFiles( fastFile, tempOutputDirectory );
    }

    @Override
    protected void initializeDeflator()
    {
        // Just business as usual, let the super class do its thing
        super.initializeDeflator();

        kmerBuffer = new CyclicBuffer<Byte>( kmerSize );
        kbuff = new byte [kmerSize];
        deBruijn = new KmerDeBruijnGraph();
        isBuildingDeBruijn = true;

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
        datahcf = new ByteBufferOutputStream( ByteBuffer.allocate( 0 ) )
        {
            @Override
            public void put( final byte b ) throws IOException
            {
                // Keep these incoming bytes for ourself
                // for building the de bruijn
                // on the first run through of the input file!
                kmerBuffer.add( b );
                if ( kmerBuffer.length() == kmerBuffer.getMaxSize() )
                    deBruijn.countKmer( getKmerBuffer() );
            }
        };

        theRealNFile = nfile;
        nfile = new ByteBufferOutputStream( ByteBuffer.allocate( 0 ) )
        {
            @Override
            public void write( final int b ) throws IOException
            {
                // No need for these nucleotides just yet
            }

        };

        theRealIupacFile = iupacfile;
        iupacfile = new BufferedOutputStream( new OutputStream()
        {
            @Override
            public void write( final int b ) throws IOException
            {
                // No need for these nucleotides just yet
            }
        } );
    }

    @Override
    protected String processSequenceIdentifier() throws IOException
    {
        String seqID = null;
        if ( !deBruijn.hadEnough() )
            while ( dnaByte != NEWLINE )
                dnaByte = (byte) fastIn.read();
        else seqID = super.processSequenceIdentifier();

        return seqID;
    }

    @Override
    protected void progressLineType()
    {
        super.progressLineType();

        /*
         * Here is where we check if we are done building the De Bruijn and put
         * everything back to normal
         */
        if ( isBuildingDeBruijn && ( deBruijn.hadEnough() || fastIn.available() <= 0 ) )
        {
            isBuildingDeBruijn = false;
            super.initializeDeflator();
            fastIn.position( 0 );
            kmerBuffer.clear();

            deBruijn.startOptimization();

            // Drop the de bruijn into the top of the data file.
            try
            {
                debruijnfile.write( deBruijn.toString().getBytes() );
            } catch ( final IOException e )
            {
                e.printStackTrace();
            }

            // Wrapper of actual mapped nucleotide file
            datahcf = new ByteBufferOutputStream( ByteBuffer.allocate( 0 ) )
            {
                @Override
                public void put( byte b ) throws IOException
                {
                    /*
                     * Given incoming bytes here, get information for the
                     * deBruijn -> Do your puts to theRealDataHCF!
                     */
                    kmerBuffer.add( b );
                    if ( kmerBuffer.length() == kmerBuffer.getMaxSize() )
                        kmerMapping();
                }

                @Override
                public long position()
                {
                    return theRealDataHCF.position();
                }

                @Override
                public synchronized void close() throws IOException
                {
                    super.close();
                    theRealDataHCF.close();
                    theRealDataHCF = null;
                }
            };

            try
            {
                nfile.close();
                iupacfile.close();
            } catch ( final IOException e )
            {}
            nfile = theRealNFile;
            iupacfile = theRealIupacFile;
        }
    }

    protected void kmerMapping() throws IOException
    {
        final String kmerStr = getKmerBuffer();
        final Kmer kmer;
        if ( ( kmer = deBruijn.getKmer( kmerStr ) ) != null )
        {
            if ( theRealDataHCF.position() == 0 )
            {
                theRealDataHCF.write( kmer );
            }

            final String nextNucleotide = "" + kmerStr.charAt( kmerStr.length() - 1 );
            // final String nextKmer = kmer.suffix() + nextNucleotide;
            final Set<Kmer> successors = kmer.getChildren();
            if ( successors.size() == 1 )
            {
                if ( !successors.contains( kmer ) )
                {
                    // probable sequencing error
                    theRealDataHCF.write( nextNucleotide );
                    // Track that at this location we have a
                    // mutation
                    differencefile.write(
                        new String( Long.toHexString( theRealDataHCF.position() ) ).getBytes() );
                    differencefile.write( DIFFERENCE_DELIMITER.getBytes() );

                }
                // Make sure we fake this nucleotide from
                // here on out though
                for ( final byte nb : successors.stream().findFirst().get().getKmer().getBytes() )
                    kmerBuffer.add( nb );
            } else
            {
                theRealDataHCF.write( nextNucleotide );
            }
        }
    }

    @Override
    public void removeUnusedBufferSpace( final String tmpOutputDirectory )
            throws IOException, InterruptedException
    {
        super.removeUnusedBufferSpace( tmpOutputDirectory );

        debruijnfile.close();
        differencefile.close();
    }

    @Override
    protected void processProperties() throws IOException
    {
        super.processProperties();

        // NOTE that multiplying by 4 is to ensure that the kmerSize stored in
        // the meta file, actually has to do with the tetramers that the
        // nucleotides come in as
        metafile.write( format( META_KMER_SIZE, kmerSize * 4 ) );
    }

    private String getKmerBuffer()
    {
        final Byte [] bytes = kmerBuffer.asList().stream().toArray( Byte []::new );
        for ( int i = 0; i < bytes.length; i++ )
            kbuff[i] = bytes[i];

        return new String( kbuff );
    }
}
