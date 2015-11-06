/**
 *  COPYRIGHT (C) 2015 Alex Aiezza. All Rights Reserved.
 *
 *  See the LICENSE for the specific language governing permissions and
 *  limitations under the License provided with this project.
 */
package edu.rit.flick.config;

import java.util.Set;

/**
 * @author Alex Aiezza
 *
 */
public interface OptionSet <V>
{
    public Set<Option<V>> getOptions();
}
