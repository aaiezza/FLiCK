/**
 *  COPYRIGHT (C) 2015 Alex Aiezza. All Rights Reserved.
 *
 *  See the LICENSE for the specific language governing permissions and
 *  limitations under the License provided with this project.
 */
package edu.rit.flick.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import edu.rit.flick.FileArchiver;
import edu.rit.flick.FileDeflator;
import edu.rit.flick.FileInflator;
import edu.rit.flick.RegisterFileDeflatorInflator;

/**
 * A singleton helper class for registering and storing mappings of file
 * extensions to file archivers.
 *
 * @author Alex Aiezza
 *
 */
public final class FileArchiverExtensionRegistry
{
    private class FileDeflatorInflator
    {
        private final FileDeflator fileDeflator;
        private final FileInflator fileInflator;
        private final List<String> extensions;

        private FileDeflatorInflator(
            final FileDeflator fileDeflator,
            final FileInflator fileInflator,
            final List<String> extensions )
        {
            this.fileDeflator = fileDeflator;
            this.fileInflator = fileInflator;
            this.extensions = extensions;
        }
    }

    // Singleton
    private static FileArchiverExtensionRegistry FILE_ARCHIVER_REGISTRY;

    /**
     * Retrieve the registry.
     *
     * @return The single existing instance of the FileArchiverExtensionRegistry
     */
    public static synchronized FileArchiverExtensionRegistry getInstance()
    {
        if ( FILE_ARCHIVER_REGISTRY == null )
            FILE_ARCHIVER_REGISTRY = new FileArchiverExtensionRegistry();
        return FILE_ARCHIVER_REGISTRY;
    }

    private final Map<String, FileDeflatorInflator> registry;
    private final List<DeflationOptionSet>          deflationOptionSets;

    private final List<InflationOptionSet>          inflationOptionSets;

    /**
     * Constructs the file archiver extension registry and scans for
     * FileArchiver classes.
     */
    private FileArchiverExtensionRegistry()
    {
        registry = new HashMap<String, FileDeflatorInflator>();
        deflationOptionSets = new ArrayList<DeflationOptionSet>();
        inflationOptionSets = new ArrayList<InflationOptionSet>();

        final ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(
                false )
        {
            @Override
            protected boolean isCandidateComponent( final AnnotatedBeanDefinition beanDefinition )
            {
                return beanDefinition.getMetadata().isIndependent();
            }
        };

        scanner.addIncludeFilter( new AnnotationTypeFilter( RegisterFileDeflatorInflator.class ) );

        for ( final BeanDefinition bd : scanner.findCandidateComponents( "*" ) )
            try
            {
                final Class<?> cl = Class.forName( bd.getBeanClassName() );
                final RegisterFileDeflatorInflator fileDIP = cl
                        .getAnnotation( RegisterFileDeflatorInflator.class );
                if ( fileDIP != null )
                    registerFileArchiverExtensions( fileDIP );
            } catch ( final Exception e )
            {}
    }

    public String getDeflatedExtension( final String extension )
    {
        final FileDeflatorInflator fileDIP = registry.get( extension );
        if ( fileDIP != null )
            return fileDIP.fileDeflator.getDefaultDeflatedExtension();

        return null;
    }

    public List<DeflationOptionSet> getDeflationOptionSets()
    {
        return deflationOptionSets;
    }

    public List<String> getExtensions( final Class<? extends FileArchiver> fileArchiver )
    {
        final Optional<FileDeflatorInflator> fileDIPo = registry.values().stream()
                .filter(
                    fileDIP -> fileArchiver.isAssignableFrom( fileDIP.fileDeflator.getClass() ) ||
                            fileArchiver.isAssignableFrom( fileDIP.fileInflator.getClass() ) )
                .findFirst();
        return fileDIPo.isPresent() ? fileDIPo.get().extensions : null;
    }

    public List<String> getExtensions( final FileArchiver fileArchiver )
    {
        final Optional<FileDeflatorInflator> fileDIPo = registry.values().stream()
                .filter( fileDIP -> fileDIP.fileDeflator.equals( fileArchiver ) ||
                        fileDIP.fileInflator.equals( fileArchiver ) )
                .findFirst();
        return fileDIPo.isPresent() ? fileDIPo.get().extensions : null;
    }

    public FileDeflator getFileDeflator( final String extension )
    {
        final FileDeflatorInflator fileDIP = registry.get( extension );
        if ( fileDIP != null )
            return fileDIP.fileDeflator;

        return null;
    }

    public FileInflator getFileInflator( final String extension )
    {
        final Optional<FileDeflatorInflator> fileDIo = registry.values().stream().filter(
            fileDI -> fileDI.fileInflator.getDefaultDeflatedExtension().equals( extension ) )
                .findFirst();
        return fileDIo.isPresent() ? fileDIo.get().fileInflator : null;
    }

    public List<InflationOptionSet> getInflationOptionSets()
    {
        return inflationOptionSets;
    }

    public synchronized void registerFileArchiverExtensions(
            final RegisterFileDeflatorInflator fileDIP )
            throws InstantiationException, IllegalAccessException
    {
        final List<String> extensions = Arrays.asList( fileDIP.inflatedExtensions() );

        for ( int e = 0; e < extensions.size(); e++ )
        {
            String extension = extensions.get( e );
            if ( !extension.startsWith( "." ) )
                extension = "." + extension;
            if ( extension.matches( "\\.{2,}.+" ) )
                extension = extension.replaceFirst( "\\.{2,}", "." );

            extensions.set( e, extension );
        }

        for ( final String extension : extensions )
            registry.put( extension, new FileDeflatorInflator( fileDIP.fileDeflator().newInstance(),
                    fileDIP.fileInflator().newInstance(), extensions ) );

        deflationOptionSets.add( fileDIP.fileDeflatorOptionSet().newInstance() );
        inflationOptionSets.add( fileDIP.fileInflatorOptionSet().newInstance() );
    }
}
