/**
 *  COPYRIGHT (C) 2015 Alex Aiezza. All Rights Reserved.
 *
 *  See the LICENSE for the specific language governing permissions and
 *  limitations under the License provided with this project.
 */
package edu.rit.flick;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import edu.rit.flick.config.DeflationOptionSet;
import edu.rit.flick.config.InflationOptionSet;

/**
 * @author Alex Aiezza
 *
 *
 */
@Retention ( RetentionPolicy.RUNTIME )
public @interface RegisterFileDeflatorInflator
{
    String [] inflatedExtensions();

    String deflatedExtension() default "flick";

    Class<? extends FileDeflator> fileDeflator() default FlickFile.class;

    Class<? extends FileInflator> fileInflator() default FlickFile.class;

    Class<? extends DeflationOptionSet> fileDeflatorOptionSet();

    Class<? extends InflationOptionSet> fileInflatorOptionSet();
}
