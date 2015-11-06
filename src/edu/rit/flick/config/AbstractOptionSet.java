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
public abstract class AbstractOptionSet <V extends Object> implements OptionSet<V>
{
    protected final Set<Option<V>> options;

    {
        options = new HashSet<Option<V>>();
    }

    @Override
    public Set<Option<V>> getOptions()
    {
        return options;
    }
}
