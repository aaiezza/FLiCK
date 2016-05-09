package edu.rit.flick.genetics.util;

import static edu.rit.flick.genetics.util.HexPrinter.shortToFile;
import static edu.rit.flick.genetics.util.HexPrinter.shortToHexString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

public class HexPrinterTest
{
    private static final int NUMBER_OF_SHORTS_TO_TEST = 10000;

    private final File       testFile                 = new File( "test_resources/testShorts.txt" );

    @Test
    public void testSomeShortsToFile()
    {
        final Runnable cleanUp = () -> {
            if ( testFile.exists() )
                testFile.delete();
        };

        final List<Short> shawties = randomShorts().collect( Collectors.toList() );

        // Write out shorts
        try ( final FileOutputStream fw = new FileOutputStream( testFile ) )
        {
            for ( final short s : shawties )
                shortToFile( (short) s, fw );
        } catch ( final IOException e )
        {
            fail( e.getMessage() );
            cleanUp.run();
        }

        // Read them back to make sure they are correct
        try ( final FileInputStream fr = new FileInputStream( testFile ) )
        {
            final byte [] buffer = new byte [2];
            for ( final short s : shawties )
            {
                fr.read( buffer );
                assertEquals( ByteBuffer.wrap( buffer ).getShort(), s );
            }
        } catch ( final IOException e )
        {
            fail( e.getMessage() );
        } finally
        {
            cleanUp.run();
        }
    }

    @Test
    public void testSomeShorts()
    {
        randomShorts().forEach(
            s -> assertEquals( Integer.parseInt( shortToHexString( s ), 16 ), (short) s ) );
    }

    private Stream<Short> randomShorts()
    {
        return Stream.iterate( (short) 10, ( seed ) -> (short) ( Math.random() * Short.MAX_VALUE ) )
                .limit( NUMBER_OF_SHORTS_TO_TEST );
    }
}
