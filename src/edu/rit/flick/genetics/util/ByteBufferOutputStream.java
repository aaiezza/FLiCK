/**
 *  COPYRIGHT (C) 2015 Alex Aiezza. All Rights Reserved.
 *
 *  See the LICENSE for the specific language governing permissions and
 *  limitations under the License provided with this project.
 */
package edu.rit.flick.genetics.util;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.Arrays;

import it.unimi.dsi.fastutil.io.MeasurableOutputStream;
import it.unimi.dsi.fastutil.io.RepositionableStream;

/**
 * @author Alex Aiezza
 *
 */
public class ByteBufferOutputStream extends MeasurableOutputStream implements RepositionableStream
{
    private static int       CHUNK_SHIFT = 30;

    /**
     * The size of a chunk created by
     * {@link #map(FileChannel, FileChannel.MapMode)}.
     */
    public static final long CHUNK_SIZE  = 1L << CHUNK_SHIFT;

    public static ByteBufferOutputStream map(
            final File file,
            final MapMode mapMode,
            final long size ) throws IOException
    {
        final int chunks = (int) ( ( size + CHUNK_SIZE - 1 ) / CHUNK_SIZE );
        final ByteBuffer [] byteBuffer = new ByteBuffer [chunks];
        final RandomAccessFile raf = new RandomAccessFile( file, "rwd" );
        raf.setLength( size );

        final FileChannel fileChannel = raf.getChannel();

        for ( int i = 0; i < chunks; i++ )
            byteBuffer[i] = fileChannel.map( MapMode.READ_WRITE, i * CHUNK_SIZE,
                Math.min( CHUNK_SIZE, size - i * CHUNK_SIZE ) );
        byteBuffer[0].position( 0 );
        final boolean [] readyToUse = new boolean [chunks];
        Arrays.fill( readyToUse, true );
        raf.close();
        fileChannel.close();
        return new ByteBufferOutputStream( byteBuffer, size, 0, readyToUse );
    }

    /**
     * Creates a new read-only byte-buffer input stream by mapping a given file
     * channel.
     *
     * @param fileChannel
     *            the file channel that will be mapped.
     * @return a new read-only byte-buffer input stream over the contents of
     *         <code>fileChannel</code>.
     */
    public static ByteBufferOutputStream map( final FileChannel fileChannel ) throws IOException
    {
        return map( fileChannel, MapMode.READ_ONLY );
    }

    /**
     * Creates a new byte-buffer input stream by mapping a given file channel.
     *
     * @param fileChannel
     *            the file channel that will be mapped.
     * @param mapMode
     *            this must be {@link MapMode#READ_ONLY}.
     * @return a new byte-buffer input stream over the contents of
     *         <code>fileChannel</code>.
     */
    public static ByteBufferOutputStream map( final FileChannel fileChannel, final MapMode mapMode )
            throws IOException
    {
        final long size = fileChannel.size();
        final int chunks = (int) ( ( size + CHUNK_SIZE - 1 ) / CHUNK_SIZE );
        final ByteBuffer [] byteBuffer = new ByteBuffer [chunks];

        for ( int i = 0; i < chunks; i++ )
            byteBuffer[i] = fileChannel.map( mapMode, i * CHUNK_SIZE,
                Math.min( CHUNK_SIZE, size - i * CHUNK_SIZE ) );
        byteBuffer[0].position( 0 );
        final boolean [] readyToUse = new boolean [chunks];
        Arrays.fill( readyToUse, true );
        return new ByteBufferOutputStream( byteBuffer, size, 0, readyToUse );
    }

    public static ByteBufferOutputStream map(
            final FileChannel fileChannel,
            final MapMode mapMode,
            final long size ) throws IOException
    {
        final int chunks = (int) ( ( size + CHUNK_SIZE - 1 ) / CHUNK_SIZE );
        final ByteBuffer [] byteBuffer = new ByteBuffer [chunks];

        for ( int i = 0; i < chunks; i++ )
            byteBuffer[i] = fileChannel.map( mapMode, i * CHUNK_SIZE,
                Math.min( CHUNK_SIZE, size - i * CHUNK_SIZE ) );
        byteBuffer[0].position( 0 );
        final boolean [] readyToUse = new boolean [chunks];
        Arrays.fill( readyToUse, true );
        return new ByteBufferOutputStream( byteBuffer, size, 0, readyToUse );
    }

    /** The underlying byte buffers. */
    private final ByteBuffer [] byteBuffer;

    /**
     * An array parallel to {@link #byteBuffer} specifying which buffers do not
     * need to be {@linkplain ByteBuffer#duplicate() duplicated} before being
     * used.
     */
    private final boolean []    readyToUse;

    /** The number of byte buffers. */
    private final int           n;

    /** The current buffer. */
    private int                 curr;

    /** The current mark as a position, or -1 if there is no mark. */
    private long                mark;

    /** The overall size of this input stream. */
    private final long          size;

    /** The capacity of the last buffer. */
    private final int           lastBufferCapacity;

    /**
     * Creates a new byte-buffer input stream from a single {@link ByteBuffer}.
     *
     * @param byteBuffer
     *            the underlying byte buffer.
     */

    public ByteBufferOutputStream( final ByteBuffer byteBuffer )
    {
        this( new ByteBuffer [] { byteBuffer }, byteBuffer.capacity(), 0, new boolean [1] );
    }

    /**
     * Creates a new byte-buffer input stream.
     *
     * @param byteBuffer
     *            the underlying byte buffers.
     * @param size
     *            the sum of the {@linkplain ByteBuffer#capacity() capacities}
     *            of the byte buffers.
     * @param curr
     *            the current buffer (reading will start at this buffer from its
     *            current position).
     * @param readyToUse
     *            an array parallel to <code>byteBuffer</code> specifying which
     *            buffers do not need to be {@linkplain ByteBuffer#duplicate()
     *            duplicated} before being used (the process will happen
     *            lazily); the array will be used internally by the newly
     *            created byte-buffer input stream.
     */

    protected ByteBufferOutputStream(
        final ByteBuffer [] byteBuffer,
        final long size,
        final int curr,
        final boolean [] readyToUse )
    {
        this.byteBuffer = byteBuffer;
        n = byteBuffer.length;
        this.curr = curr;
        this.size = size;
        this.readyToUse = readyToUse;

        mark = -1;

        for ( int i = 0; i < n; i++ )
            if ( i < n - 1 && byteBuffer[i].capacity() != CHUNK_SIZE )
                throw new IllegalArgumentException();
        lastBufferCapacity = byteBuffer[n - 1].capacity();
    }

    public int available()
    {
        final long available = remaining();
        return available <= Integer.MAX_VALUE ? (int) available : Integer.MAX_VALUE;
    }


    private ByteBuffer byteBuffer( final int n )
    {
        if ( readyToUse[n] )
            return byteBuffer[n];
        readyToUse[n] = true;
        return byteBuffer[n] = byteBuffer[n].duplicate();
    }

    @Override
    public synchronized void close() throws IOException
    {
        for ( int b = 0; b < byteBuffer.length; b++ )
        {
            // we could use this type cast and call functions without reflection
            // code,
            // but static import from sun.* package is risky for non-SUN virtual
            // machine.
            // try { ((sun.nio.ch.DirectBuffer)cb).cleaner().clean(); } catch
            // (Exception ex) { }
            try
            {
                final Method cleaner = byteBuffer[b].getClass().getMethod( "cleaner" );
                cleaner.setAccessible( true );
                final Method clean = Class.forName( "sun.misc.Cleaner" ).getMethod( "clean" );
                clean.setAccessible( true );
                // clean.invoke( cleaner.invoke( byteBuffer[b] ) );
            } catch ( final Exception ex )
            {}
            byteBuffer[b].flip();
            byteBuffer[b].clear();
            byteBuffer[b] = null;
        }
        System.gc();
        super.close();
    }

    public ByteBufferOutputStream copy()
    {
        return new ByteBufferOutputStream( byteBuffer.clone(), size, curr, new boolean [n] );
    }

    public ByteBuffer [] getByteBuffers()
    {
        return byteBuffer;
    }

    public boolean hasRemaining()
    {
        return available() > 0;
    }

    @Override
    public long length()
    {
        return size;
    }

    public synchronized void mark( final int unused )
    {
        mark = position();
    }

    public boolean markSupported()
    {
        return true;
    }

    @Override
    public long position()
    {
        return ( (long) curr << CHUNK_SHIFT ) + byteBuffer( curr ).position();
    }

    @Override
    public void position( long newPosition )
    {
        newPosition = Math.min( newPosition, length() );
        if ( newPosition == length() )
        {
            final ByteBuffer buffer = byteBuffer( curr = n - 1 );
            buffer.position( buffer.capacity() );
            return;
        }

        curr = (int) ( newPosition >>> CHUNK_SHIFT );
        byteBuffer( curr ).position( (int) ( newPosition - ( (long) curr << CHUNK_SHIFT ) ) );
    }

    public void put( final byte b ) throws IOException
    {
        if ( !byteBuffer( curr ).hasRemaining() )
            if ( curr < n - 1 )
                byteBuffer( ++curr ).position( 0 );
            else throw new IOException( "Reached Limit!" );

        byteBuffer[curr].put( b );
    }

    public void put( final byte [] b ) throws IOException
    {
        if ( !byteBuffer( curr ).hasRemaining() )
            if ( curr < n - 1 )
                byteBuffer( ++curr ).position( 0 );
            else throw new IOException( "Reached Limit!" );

        for ( final byte subB : b )
            put( subB );
    }

    public int read()
    {
        if ( !byteBuffer( curr ).hasRemaining() )
            if ( curr < n - 1 )
                byteBuffer( ++curr ).position( 0 );
            else return -1;

        return byteBuffer[curr].get() & 0xFF;
    }

    public int read( final byte [] b, final int offset, final int length )
    {
        if ( length == 0 )
            return 0;
        final long remaining = remaining();
        if ( remaining == 0 )
            return -1;
        final int realLength = (int) Math.min( remaining, length );
        int read = 0;
        while ( read < realLength )
        {
            int rem = byteBuffer( curr ).remaining();
            if ( rem == 0 )
            {
                byteBuffer( ++curr ).position( 0 );
                rem = byteBuffer( curr ).remaining();
            }
            byteBuffer[curr].get( b, offset + read, Math.min( realLength - read, rem ) );
            read += Math.min( realLength - read, rem );
        }
        return realLength;
    }

    private long remaining()
    {
        return curr == n - 1 ? byteBuffer( curr ).remaining() : byteBuffer( curr ).remaining() +
                ( (long) ( n - 2 - curr ) << CHUNK_SHIFT ) + lastBufferCapacity;
    }

    public synchronized void reset() throws IOException
    {
        if ( mark == -1 )
            throw new IOException();
        position( mark );
    }

    public long skip( final long n ) throws IOException
    {
        final long toSkip = Math.min( remaining(), n );
        position( position() + toSkip );
        return toSkip;
    }

    @Override
    public String toString()
    {
        final StringBuilder out = new StringBuilder( super.toString() );
        out.append( "\n" );

        out.append( "position: " ).append( position() );

        return out.toString();
    }

    @Override
    public void write( final int b ) throws IOException
    {
        if ( !byteBuffer( curr ).hasRemaining() )
            if ( curr < n - 1 )
                byteBuffer( ++curr ).position( 0 );
            else throw new IOException( "Reached Limit!" );

        byteBuffer[curr].put( (byte) b );
    }
}
