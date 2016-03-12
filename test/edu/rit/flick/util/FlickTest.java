/**
 *  COPYRIGHT (C) 2015 Alex Aiezza. All Rights Reserved.
 *
 * Licensed under the Geneopedia License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.geneopedia.com/licenses/LICENSE-1.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either expressed or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package edu.rit.flick.util;

import static java.lang.String.format;
import static junitx.framework.FileAssert.assertEquals;
import static org.apache.commons.io.FileUtils.getFile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import it.unimi.dsi.io.ByteBufferInputStream;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.io.Files;

import edu.rit.flick.DefaultFlickFile;
import edu.rit.flick.genetics.FastaFileArchiver;
import edu.rit.flick.genetics.FastqFileArchiver;

/**
 *
 * @author Alex Aiezza
 *
 */
public class FlickTest
{
    private static final String                VERBOSE_FLAG                    = "--verbose";

    private static final String                HELP_FLAG                       = "--help";

    private static final String                FILES_DO_NOT_MATCH_ERROR_FORMAT = "%n  File '%s' is different from%n  file '%s'%n    at byte# %d%n";

    private static final String                TEST_RESOURCES_FOLDER           = "test_resources" +
            File.separator;

    private static final String                RESOURCES_FOLDER                = "resources" +
            File.separator;

    private static final String                FULL_FASTA_EXTENSION            = ".fasta";

    private static final String                FULL_FASTQ_EXTENSION            = ".fastq";

    private static final ByteArrayOutputStream outContent                      = new ByteArrayOutputStream();
    private static final ByteArrayOutputStream errContent                      = new ByteArrayOutputStream();

    private static PrintStream                 oldOut, oldErr;

    @AfterClass
    public static void cleanUpStreams() throws IOException
    {
        outContent.close();
        errContent.close();
        System.setOut( oldOut );
        System.setErr( oldErr );
    }

    @BeforeClass
    public static void setUpStreams()
    {
        oldOut = System.out;
        oldErr = System.err;
        System.setOut( new PrintStream( outContent ) );
        System.setErr( new PrintStream( errContent ) );
    }

    private File             originalFile, flickpedFile, unflickpedFile;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @After
    public void cleanUpTempFiles() throws InterruptedException
    {
        System.gc();
        Thread.sleep( 100 );

        assertTrue( FileUtils.deleteQuietly( flickpedFile ) );
        assertTrue( FileUtils.deleteQuietly( unflickpedFile ) );
    }

    @Test
    public void directoryExperiment()
    {
        testDirectory( "experiment" );
    }

    @Test
    public void fake_fna()
    {
        testFASTAfile( "fake" );
    }

    @Test
    public void fakeSeqData_2_fq()
    {
        testFASTQfile( "fakeSeqData_2" );
    }

    @Test
    public void fakeSeqData_fq()
    {
        testFASTQfile( "fakeSeqData" );
    }

    @Test
    public void helpFlag() throws IOException
    {
        outContent.flush();
        outContent.reset();

        Flick.main( HELP_FLAG );
        String expectedUsageStatement = Files.toString( new File( RESOURCES_FOLDER +
            Flick.FLICK_USAGE_FILE ), Charset.defaultCharset() );
        String actualUsageStatement = outContent.toString();
        assertEquals( expectedUsageStatement, actualUsageStatement );

        outContent.flush();
        outContent.reset();

        Unflick.main( HELP_FLAG );
        expectedUsageStatement = Files.toString( new File( RESOURCES_FOLDER +
            Unflick.UNFLICK_USAGE_FILE ), Charset.defaultCharset() ) +
            "\n";
        actualUsageStatement = outContent.toString();
        assertEquals( expectedUsageStatement, actualUsageStatement );

        outContent.flush();
        outContent.reset();

        exception.expect( AssertionError.class );
    }

    @Test
    public void NC_008512_fna()
    {
        testFASTAfile( "NC_008512" );
    }

    @Test
    public void NC_018414_fna()
    {
        testFASTAfile( "NC_018414" );
    }

    @Test
    public void NC_018415_fna()
    {
        testFASTAfile( "NC_018415" );
    }

    @Test
    public void NC_018416_fna()
    {
        testFASTAfile( "NC_018416" );
    }

    @Test
    public void NC_018417_fna()
    {
        testFASTAfile( "NC_018417" );
    }

    @Test
    public void NC_018418_fna()
    {
        testFASTAfile( "NC_018418" );
    }

    @Test
    public void NC_021894_fna()
    {
        testFASTAfile( "NC_021894" );
    }

    @After
    public void printOutput() throws IOException
    {
        oldOut.println( outContent.toString() );
        oldErr.println( errContent.toString() );

        outContent.flush();
        outContent.reset();
    }

    @Test
    public void SRR390728_1_fq()
    {
        testFASTQfile( "SRR390728_1" );
    }

    @Test
    public void test_fna()
    {
        testFASTAfile( "test" );
    }

    @Test
    public void test_fq()
    {
        testFASTQfile( "test" );
    }

    public void testDirectory( final String directory )
    {
        final String outputDirectory = directory + "-inflated";
        originalFile = getFile( TEST_RESOURCES_FOLDER, directory );
        flickpedFile = new File( TEST_RESOURCES_FOLDER + directory +
            DefaultFlickFile.DEFAULT_DEFLATED_EXTENSION );
        unflickpedFile = getFile( TEST_RESOURCES_FOLDER, outputDirectory );

        Flick.main( VERBOSE_FLAG, originalFile.getPath() );

        Unflick.main( VERBOSE_FLAG, flickpedFile.getPath(), unflickpedFile.getPath() );

        Files.fileTreeTraverser()
        .breadthFirstTraversal( originalFile )
        .toList()
        .stream()
        .filter( file -> !file.isDirectory() )
        .forEach(
            file -> {
                final String fileName1 = file.getPath().replaceAll(
                    "((?!" + directory + ").+)" + directory + "(\\\\.+)",
                    "$1" + outputDirectory );
                final String fileName2 = file.getPath().replaceAll(
                    "((?!" + directory + ").+)" + directory + "(\\\\.+)", directory + "$2" );

                assertEquals( file, getFile( fileName1, File.separator, fileName2 ) );
            } );
    }

    private final void testFASTAfile( final String fileBase )
    {
        originalFile = getFile( TEST_RESOURCES_FOLDER, fileBase + FastaFileArchiver.FASTA_EXTENSION );
        flickpedFile = getFile( TEST_RESOURCES_FOLDER, fileBase +
            FastaFileArchiver.FASTA_EXTENSION +
            FastaFileArchiver.DEFAULT_DEFLATED_FASTA_EXTENSION );
        unflickpedFile = getFile( TEST_RESOURCES_FOLDER, fileBase + FULL_FASTA_EXTENSION );

        try
        {
            testForLosslessness();
        } catch ( final IOException | InterruptedException e )
        {
            assertNull( e.getMessage(), e );
        }
    }

    private final void testFASTQfile( final String fileBase )
    {
        originalFile = getFile( TEST_RESOURCES_FOLDER, fileBase + FastqFileArchiver.FASTQ_EXTENSION );
        flickpedFile = getFile( TEST_RESOURCES_FOLDER, fileBase +
            FastqFileArchiver.FASTQ_EXTENSION +
            FastqFileArchiver.DEFAULT_DEFLATED_FASTQ_EXTENSION );
        unflickpedFile = getFile( TEST_RESOURCES_FOLDER, fileBase + FULL_FASTQ_EXTENSION );

        try
        {
            testForLosslessness();
        } catch ( final IOException | InterruptedException e )
        {
            assertNull( e.getMessage(), e );
        }
    }

    private final void testForLosslessness() throws IOException, InterruptedException
    {
        Flick.main( VERBOSE_FLAG, originalFile.getPath() );

        Unflick.main( VERBOSE_FLAG, flickpedFile.getPath(), unflickpedFile.getPath() );

        final FileInputStream origFIS = new FileInputStream( originalFile );
        ByteBufferInputStream orig = ByteBufferInputStream.map( origFIS.getChannel() );
        final FileInputStream comAndDecomFIS = new FileInputStream( unflickpedFile );
        ByteBufferInputStream comAndDecom = ByteBufferInputStream.map( comAndDecomFIS.getChannel() );

        if ( !FileUtils.contentEquals( originalFile, unflickpedFile ) )
        {
            long position = 0;
            while ( orig.available() > 0 )
            {
                position++;
                final int o = orig.read();
                final int c = comAndDecom.read();
                assertEquals(
                    format( FILES_DO_NOT_MATCH_ERROR_FORMAT, originalFile, unflickpedFile, position ),
                    (char) o + "", (char) c + "" );
            }

            assertEquals( orig.available(), comAndDecom.available() );

            origFIS.close();
            orig.close();
            comAndDecomFIS.close();
            comAndDecom.close();

            orig = null;
            comAndDecom = null;
        }
    }
}
