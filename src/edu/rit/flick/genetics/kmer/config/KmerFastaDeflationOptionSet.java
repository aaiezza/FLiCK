/**
 *  COPYRIGHT (C) 2015 Alex Aiezza. All Rights Reserved.
 */
package edu.rit.flick.genetics.kmer.config;

import edu.rit.flick.config.IntegerOption;
import edu.rit.flick.genetics.config.FastaDeflationOptionSet;

/**
 * @author Alex Aiezza
 *
 */
public class KmerFastaDeflationOptionSet extends FastaDeflationOptionSet
{
    public static final IntegerOption KMER_SIZE = new IntegerOption( "kmer size", "kmer-size", "k",
            31 );

    {
        options.add( KMER_SIZE );
    }
}
