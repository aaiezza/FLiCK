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
package edu.rit.flick.genetics;

import static java.lang.String.format;

/**
 * @author Alex Aiezza
 *
 */
public class TetramerNotFoundException extends RuntimeException
{
    /**
     *
     */
    private static final long   serialVersionUID          = 1L;

    private static final String TETRAMER_NOT_FOUND_FORMAT = "Tetramer: '%s' cannot be found in the byteConverter.";

    /**
     * @param message
     */
    public TetramerNotFoundException( final String tetramer )
    {
        super( format( TETRAMER_NOT_FOUND_FORMAT, tetramer ) );
    }

}
