/**
 *  COPYRIGHT (C) 2015 Alex Aiezza. All Rights Reserved.
 */
package edu.rit.flick.genetics.kmer;

import static java.lang.String.format;

import edu.rit.flick.RegisterFileDeflatorInflator;
import edu.rit.flick.genetics.FastaFileArchiver;
import edu.rit.flick.genetics.kmer.config.KmerFastaDeflationOptionSet;
import edu.rit.flick.genetics.kmer.config.KmerFastaInflationOptionSet;

/**
 * @author Alex Aiezza
 *
 */
@RegisterFileDeflatorInflator (
    deflatedExtension = FastaFileArchiver.DEFAULT_DEFLATED_FASTA_EXTENSION,
    inflatedExtensions =
{ "fna", "fa", "fasta" },
    fileDeflator = KmerFastaFileDeflator.class,
    fileInflator = KmerFastaFileInflator.class,
    fileDeflatorOptionSet = KmerFastaDeflationOptionSet.class,
    fileInflatorOptionSet = KmerFastaInflationOptionSet.class )

public interface KmerFastaFileArchiver extends FastaFileArchiver
{
    public final static String BLOOM_FILTER_FILE     = "bloom.hcf";

    public final static String META_KMER_SIZE        = "kmerSize";
    public final static String META_NODES_HIT        = "nodesHit";
    public final static String META_KMER_SIZE_FORMAT = format( "%s=%%d\n", META_KMER_SIZE );
    public final static String META_NODES_HIT_FORMAT = format( "%s=%%d\n", META_NODES_HIT );
}
