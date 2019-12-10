package com.fasterxml.jackson.databind.jsontype.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;

/**
 * Helper class used to encapsulate rules that determine subtypes that
 * are invalid to use, even with default typing, mostly due to security
 * concerns.
 * Used by <code>BeanDeserializerFacotry</code>
 *
 * @since 2.8.11
 */
public class SubTypeValidator
{
    /**
     * Set of allowed packages for bean deserialization.
     */
    protected final static Set<String> ALLOW_DESER_PACKAGES;

    static {
        String strlist = System.getProperty("jackson.deserialization.whitelist.packages");
        Set<String> s = new HashSet<String>();
        if (strlist != null) {
            s = new HashSet<String>(Arrays.asList(strlist.split(",")));
        }
        ALLOW_DESER_PACKAGES = Collections.unmodifiableSet(s);
    }


    /**
     * Set of class names of types that are never to be deserialized.
     */

    private final static SubTypeValidator instance = new SubTypeValidator();

    protected SubTypeValidator() { }

    public static SubTypeValidator instance() { return instance; }

    public void validateSubType(DeserializationContext ctxt, JavaType type)
            throws JsonMappingException
    {
        final Class<?> raw = type.getRawClass();
        String typeId = raw.getCanonicalName();

        if (typeId == null || "".equals(typeId)) {
            return;
        }

        // https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.3.2
        // decompose arrays
        while (typeId.charAt(0) == '[') {
            typeId = typeId.substring(1);
            if (!typeId.isEmpty() && typeId.charAt(0) == 'L') {
                typeId = typeId.substring(1);
            }
            // we don't have to skip last ';' because of startsWith()
        }
        if (typeId.length() == 1) {
            // base type B C D F I J S Z
            return;
        }

        Iterator<String> iter = ALLOW_DESER_PACKAGES.iterator();

        boolean pass = false;

        while (iter.hasNext()) {
            if (typeId.startsWith(iter.next())) {
                pass = true;
                break;
            }
        }
        if (!pass) {
            throw JsonMappingException.from(ctxt,
                    String.format("Illegal type (%s) to deserialize: prevented for security reasons", typeId));
        }
    }
}
