/**
 *  COPYRIGHT (C) 2015 Alex Aiezza. All Rights Reserved.
 */
package edu.rit.flick.config;

/**
 * @author Alex Aiezza
 *
 */
public class IntegerOption extends AbstractOption<Integer>
{
    /**
     * @param name
     * @param longFlag
     * @param shortFlag
     * @param defaultValue
     */
    public IntegerOption(
        final String name,
        final String longFlag,
        final String shortFlag,
        final int defaultValue )
    {
        super( name, longFlag, shortFlag, defaultValue );
    }

    @Override
    public Integer parseValue( final String value )
    {
        return Integer.parseInt( value );
    }

    @Override
    public String toString()
    {
        return String.format( "StringOption (%s)", getName() );
    }
}
