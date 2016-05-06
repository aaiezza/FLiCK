/**
 *  COPYRIGHT (C) 2015 Alex Aiezza. All Rights Reserved.
 *
 *  See the LICENSE for the specific language governing permissions and
 *  limitations under the License provided with this project.
 */
package edu.rit.flick;

/**
 * @author Alex Aiezza
 *
 */
public interface FileArchiver
{
    public String getDefaultDeflatedExtension();

    public default void deflationInflationVerification() throws Exception
    {}
}
