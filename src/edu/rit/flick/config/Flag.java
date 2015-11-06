/**
 *  COPYRIGHT (C) 2015 Alex Aiezza. All Rights Reserved.
 *
 *  See the LICENSE for the specific language governing permissions and
 *  limitations under the License provided with this project.
 */
package edu.rit.flick.config;

/**
 * @author Alex Aiezza
 *
 */
public interface Flag extends Option<Boolean>
{
    @Override
    public default boolean isFlag()
    {
        return true;
    }

    @Override
    public default Boolean parseValue( final String value )
    {
        return Boolean.parseBoolean( value );
    }
}
