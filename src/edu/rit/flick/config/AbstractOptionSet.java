/**
 *  COPYRIGHT (C) 2015 Alex Aiezza. All Rights Reserved.
 *
 *  See the LICENSE for the specific language governing permissions and
 *  limitations under the License provided with this project.
 */
package edu.rit.flick.config;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Alex Aiezza
 *
 */
public abstract class AbstractOptionSet implements OptionSet
{
    protected final Set<Option<?>> options;

    {
        options = new HashSet<Option<?>>();
    }

    @Override
    public Set<Option<?>> getOptions()
    {
        return options;
    }
}
