/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2009-2010, Open Source Geospatial Foundation (OSGeo)
 *    (C) 2009-2010, Geomatys
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotoolkit.util;

import java.util.Arrays;
import org.geotoolkit.lang.Static;


/**
 * Utility methods working on {@link String} or {@link CharSequence} instances. Some methods
 * defined in this class duplicate the functionalities already provided in the {@code String}
 * class, but works on a generic {@code CharSequence} instance instead than {@code String}.
 * Other methods perform their work directly on the provided {@link StringBuilder}.
 *
 * {@section Handling of null values}
 * Some methods accept a {@code null} argument, in particular the methods converting the
 * given {@code String} to another {@code String} which may be the same. For example the
 * {@link #camelCaseToAcronym(String)} method returns {@code null} if the string to convert is
 * {@code null}. Some other methods like {@link #count(String, char)} handles {@code null}
 * argument like an empty string. The methods that don't accept a {@code null} argument are
 * explicitly documented as throwing a {@link NullPointerException}.
 *
 * @author Martin Desruisseaux (Geomatys)
 * @version 3.09
 *
 * @since 3.09 (derived from 3.00)
 * @module
 */
@Static
public final class Strings {
    /**
     * An array of strings containing only white spaces. Strings' lengths are equal to their
     * index in the {@code spaces} array. For example, {@code spaces[4]} contains a string of
     * length 4. Strings are constructed only when first needed.
     */
    private static final String[] spaces = new String[21];
    static {
        final int last = spaces.length - 1;
        final char[] blancs = new char[last];
        Arrays.fill(blancs, ' ');
        spaces[last] = new String(blancs).intern();
    }

    /**
     * Do not allow instantiation of this class.
     */
    private Strings() {
    }

    /**
     * Returns a string of the specified length filled with white spaces.
     * This method tries to return a pre-allocated string if possible.
     *
     * @param  length The string length. Negative values are clamped to 0.
     * @return A string of length {@code length} filled with white spaces.
     */
    public static String spaces(int length) {
        /*
         * No need to synchronize.  In the unlikely event of two threads calling this method
         * at the same time and the two calls creating a new string, the String.intern() call
         * will take care of canonicalizing the strings.
         */
        if (length < 0) {
            length = 0;
        }
        String s;
        if (length < spaces.length) {
            s = spaces[length];
            if (s == null) {
                s = spaces[spaces.length - 1].substring(0, length).intern();
                spaces[length] = s;
            }
        } else {
            char[] blancs = new char[length];
            Arrays.fill(blancs, ' ');
            s = new String(blancs);
        }
        return s;
    }

    /**
     * Counts the number of occurence of the given character in the given string. This
     * method performs the same work than {@link #count(CharSequence, char)}, but is faster.
     *
     * @param  text The text in which to count the number of occurence.
     * @param  c The character to count, or 0 if {@code text} was null.
     * @return The number of occurences of the given character.
     */
    public static int count(final String text, final char c) {
        int n = 0;
        if (text != null) {
            for (int i=text.indexOf(c); ++i!=0; i=text.indexOf(c, i)) {
                n++;
            }
        }
        return n;
    }

    /**
     * Counts the number of occurence of the given character in the given character sequence.
     * This method performs the same work than {@link #count(String, char)}, but on a more
     * generic interface.
     *
     * @param  text The text in which to count the number of occurence.
     * @param  c The character to count, or 0 if {@code text} was null.
     * @return The number of occurences of the given character.
     */
    public static int count(final CharSequence text, final char c) {
        if (text instanceof String) {
            return count((String) text, c);
        }
        int n = 0;
        if (text != null) {
            for (int i=text.length(); --i>=0;) {
                if (text.charAt(i) == c) {
                    n++;
                }
            }
        }
        return n;
    }

    /**
     * Replaces every occurences of the given string in the given buffer.
     * This method invokes {@link StringBuilder#replace(int, int, String)}
     * for each occurence of {@code search} found in the buffer.
     *
     * @param buffer The string in which to perform the replacements.
     * @param search The string to replace.
     * @param replacement The replacement for the target string.
     * @throws NullPointerException if any of the arguments is null.
     *
     * @see String#replace(char, char)
     * @see StringBuilder#replace(int, int, String)
     */
    public static void replace(final StringBuilder buffer, final String search, final String replacement) {
        final int length = search.length();
        int i = buffer.length();
        while ((i = buffer.lastIndexOf(search, i)) >= 0) {
            buffer.replace(i, i+length, replacement);
            i -= length;
        }
    }

    /**
     * Removes every occurences of the given string in the given buffer. This method invokes
     * {@link StringBuilder#delete(int, int)} for each occurence of {@code search} found in
     * the buffer.
     *
     * @param buffer The string in which to perform the removals.
     * @param search The string to remove.
     * @throws NullPointerException if any of the arguments is null.
     *
     * @see StringBuilder#delete(int, int)
     */
    public static void remove(final StringBuilder buffer, final String search) {
        final int length = search.length();
        for (int i=buffer.lastIndexOf(search); i>=0; i=buffer.lastIndexOf(search, i)) {
            buffer.delete(i, i + length);
        }
    }

    /**
     * Returns a string with leading and trailing whitespaces omitted. This method is similar
     * in purpose to {@link String#trim()}, except that the {@link Character#isWhitespace(char)}
     * method is used in order to determine if a character is a whitespace. This is in contrast
     * with the {@code String.trim()} method, which consider every ASCII control code below 32
     * to be a whitespace. The later has the effect of removing {@linkplain org.geotoolkit.io.X364
     * X3.64} escape sequences as well. The method defined here will preserve them.
     *
     * @param text The string from which to remove leading and trailing whitespaces.
     * @return A string with leading and trailing whitespaces removed.
     *
     * @see String#trim()
     */
    public static String trim(String text) {
        if (text != null) {
            int upper = text.length();
            while (upper != 0 && Character.isWhitespace(text.charAt(upper-1))) upper--;
            int lower = 0;
            while (lower < upper && Character.isWhitespace(text.charAt(lower))) lower++;
            text = text.substring(lower, upper);
        }
        return text;
    }

    /**
     * Trims the fractional part of the given formatted number, provided that it doesn't change
     * the value. This method assumes that the number is formatted in the US locale, typically
     * by the {@link Double#toString(double)} method.
     * <p>
     * More specifically if the given string ends with a {@code '.'} character followed by a
     * sequence of {@code '0'} characters, then those characters are omitted. Otherwise this
     * method returns the string unchanged. This is a "<cite>all or nothing<cite>" method:
     * either the fractional part is completly removed, or either it is left unchanged.
     *
     * {@section Examples}
     * This method returns {@code "4"} if the given value is {@code "4."}, {@code "4.0"} or
     * {@code "4.00"}, but returns {@code "4.10"} unchanged (including the trailing {@code '0'}
     * character) if the input is {@code "4.10"}.
     *
     * {@section Use case}
     * This method is useful before to {@linkplain Integer#parseInt(String) parse a number}
     * if that number should preferrably be parsed as an integer before attempting to parse
     * it as a floating point number.
     *
     * @param  value The value to trim if possible, or {@code null}.
     * @return The value without the trailing {@code ".0"} part (if any),
     *         or {@code null} if the given string was null.
     */
    public static String trimFractionalPart(final String value) {
        if (value != null) {
            for (int i=value.length(); --i>=0;) {
                switch (value.charAt(i)) {
                    case '0': continue;
                    case '.': return value.substring(0, i);
                    default : return value;
                }
            }
        }
        return value;
    }

    /**
     * Trims the fractional part of the given formatted number, provided that it doesn't change
     * the value. This method performs the same work than {@link #trimFractionalPart(String)}
     * except that it modifies the given buffer in-place.
     *
     * {@section Use case}
     * This method is useful after a {@linkplain StringBuilder#append(double) double value has
     * been appended to the buffer}, in order to make it appears like an integer when possible.
     *
     * @param buffer The buffer to trim if possible.
     * @throws NullPointerException if the argument is null.
     */
    @SuppressWarnings("fallthrough")
    public static void trimFractionalPart(final StringBuilder buffer) {
        for (int i=buffer.length(); --i>=0;) {
            switch (buffer.charAt(i)) {
                case '0': continue;
                case '.': buffer.setLength(i);
                default : return;
            }
        }
    }

    /**
     * Given a string in camel cases, returns a string with the same words separated by spaces.
     * A word begins with a upper-case character following a lower-case character. For example
     * if the given string is {@code "PixelInterleavedSampleModel"}, then this method returns
     * "<cite>Pixel Interleaved Sample Model</cite>" or "<cite>Pixel interleaved sample model</cite>"
     * depending on the value of the {@code toLowerCase} argument.
     * <p>
     * If {@code toLowerCase} is {@code false}, then this method inserts spaces but does not change
     * the case of characters. If {@code toLowerCase} is {@code true}, then this method changes
     * {@linkplain Character#toLowerCase(char) to lower case} the first character after each spaces
     * inserted by this method (note that this intentionnaly exclude the very first character in
     * the given string), except if the second character {@linkplain Character#isUpperCase(char)
     * is upper case}, in which case the words is assumed an accronym.
     * <p>
     * The given string is usually a programmatic identifier like a class name or a method name.
     *
     * @param  identifier An identifier with no space, words begin with an upper-case character.
     * @param  toLowerCase {@code true} for changing the first character of words to lower case,
     *         except for the first word and accronyms.
     * @return The identifier with spaces inserted after what looks like words.
     * @throws NullPointerException if the {@code identifier} argument is null.
     */
    public static StringBuilder camelCaseToWords(final CharSequence identifier, final boolean toLowerCase) {
        final int length = identifier.length();
        final StringBuilder buffer = new StringBuilder(length + 8);
        int last = 0;
        for (int i=1; i<=length; i++) {
            if (i == length ||
                (Character.isUpperCase(identifier.charAt(i)) &&
                 Character.isLowerCase(identifier.charAt(i-1))))
            {
                final int pos = buffer.length();
                buffer.append(identifier, last, i).append(' ');
                if (toLowerCase && pos!=0 && last<length-1 && Character.isLowerCase(identifier.charAt(last+1))) {
                    buffer.setCharAt(pos, Character.toLowerCase(buffer.charAt(pos)));
                }
                last = i;
            }
        }
        /*
         * Removes the trailing space, if any.
         */
        int lg = buffer.length();
        if (lg != 0 && Character.isSpaceChar(buffer.charAt(--lg))) {
            buffer.setLength(lg);
        }
        return buffer;
    }

    /**
     * Creates an acronym from the given text. If every characters in the given text are upper
     * case, then the text is returned unchanged on the assumption that it is already an accronym.
     * Otherwise this method returns a string containing the first character of each word, where
     * the words are separated by the camel case convention, the {@code '_'} character, or any
     * character which is not a {@linkplain Character#isJavaIdentifierPart(char) java identifier
     * part} (including spaces).
     * <p>
     * <b>Examples:</b> given {@code "northEast"}, this method returns {@code "NE"}.
     * Given {@code "Open Geospatial Consortium"}, this method returns {@code "OGC"}.
     *
     * @param  text The text for which to create an acronym, or {@code null}.
     * @return The acronym, or {@code null} if the given text was null.
     */
    public static String camelCaseToAcronym(String text) {
        if (text != null && !isUpperCase(text = text.trim())) {
            final int length = text.length();
            final StringBuilder buffer = new StringBuilder();
            boolean wantChar = true;
            for (int i=0; i<length; i++) {
                final char c = text.charAt(i);
                if (wantChar) {
                    if (Character.isJavaIdentifierStart(c)) {
                        buffer.append(c);
                        wantChar = false;
                    }
                } else if (!Character.isJavaIdentifierPart(c) || c == '_') {
                    wantChar = true;
                } else if (Character.isUpperCase(c)) {
                    // Test for mixed-case (e.g. "northEast").
                    // Note that the buffer is garanteed to contain at least 1 character.
                    if (Character.isLowerCase(buffer.charAt(buffer.length() - 1))) {
                        buffer.append(c);
                    }
                }
            }
            final int acrlg = buffer.length();
            if (acrlg != 0) {
                /*
                 * If every characters except the first one are upper-case, ensure that the
                 * first one is upper-case as well. This is for handling the identifiers which
                 * are compliant to Java-Beans convention (e.g. "northEast").
                 */
                if (isUpperCase(buffer, 1, acrlg)) {
                    buffer.setCharAt(0, Character.toUpperCase(buffer.charAt(0)));
                }
                final String acronym = buffer.toString();
                if (!text.equals(acronym)) {
                    text = acronym;
                }
            }
        }
        return text;
    }

    /**
     * Returns {@code true} if the first string is likely to be an acronym of the second string.
     * An acronym is a sequence of {@linkplain Character#isLetterOrDigit letters or digits}
     * built from at least one character of each word in the {@code words} string. More than
     * one character from the same word may appear in the acronym, but they must always
     * be the first consecutive characters.
     * <p>
     * <b>Example:</b> given the string {@code "Open Geospatial Consortium"}, the following
     * strings are recognized as acronyms: {@code "OGC"}, {@code "O.G.C."}, {@code "OpGeoCon"}.
     * The comparison is case-insensitive.
     *
     * @param  acronym A possible acronym of the sequence of words.
     * @param  words The sequence of words.
     * @return {@code true} if the first string is an acronym of the second one.
     * @throws NullPointerException if any of the arguments is null.
     */
    public static boolean isAcronymForWords(final CharSequence acronym, final CharSequence words) {
        final int lgc = words.length();
        final int lga = acronym .length();
        int ic=0, ia=0;
        char ca, cc;
        do if (ia >= lga) return false;
        while (!Character.isLetterOrDigit(ca = acronym.charAt(ia++)));
        do if (ic >= lgc) return false;
        while (!Character.isLetterOrDigit(cc = words.charAt(ic++)));
        if (Character.toUpperCase(ca) != Character.toUpperCase(cc)) {
            // The first letter must match.
            return false;
        }
cmp:    while (ia < lga) {
            if (ic >= lgc) {
                // There is more letters in the acronym than in the complete name.
                return false;
            }
            ca = acronym .charAt(ia++);
            cc = words.charAt(ic++);
            if (Character.isLetterOrDigit(ca)) {
                if (Character.toUpperCase(ca) == Character.toUpperCase(cc)) {
                    // Acronym letter matches the letter from the complete name.
                    // Continue the comparison with next letter of both strings.
                    continue;
                }
                // Will search for the next word after the 'else' block.
            } else do {
                if (ia >= lga) break cmp;
                ca = acronym.charAt(ia++);
            } while (!Character.isLetterOrDigit(ca));
            /*
             * At this point, 'ca' is the next acronym letter to compare and we
             * need to search for the next word in the complete name. We first
             * skip remaining letters, then we skip non-letter characters.
             */
            boolean skipLetters = true;
            do while (Character.isLetterOrDigit(cc) == skipLetters) {
                if (ic >= lgc) {
                    return false;
                }
                cc = words.charAt(ic++);
            } while ((skipLetters = !skipLetters) == false);
            // Now that we are aligned on a new word, the first letter must match.
            if (Character.toUpperCase(ca) != Character.toUpperCase(cc)) {
                return false;
            }
        }
        /*
         * Now that we have processed all acronym letters, the complete name can not have
         * any additional word. We can only finish the current word and skip trailing non-
         * letter characters.
         */
        boolean skipLetters = true;
        do {
            do {
                if (ic >= lgc) return true;
                cc = words.charAt(ic++);
            } while (Character.isLetterOrDigit(cc) == skipLetters);
        } while ((skipLetters = !skipLetters) == false);
        return false;
    }

    /**
     * Returns {@code true} if the two given strings are equal, ignoring case. This method assumes
     * an ASCII character set, which is okay for simple needs like checking for a SQL keyword. For
     * comparaison that are valide in a wider range of Unicode character set, use the Java {@link
     * String#equalsIgnoreCase} method instead.
     *
     * @param  s1 The first string to compare.
     * @param  s2 The second string to compare.
     * @return {@code true} if the two given strings are equal, ignoring case.
     * @throws NullPointerException if any of the arguments is null.
     *
     * @see String#equalsIgnoreCase(String)
     */
    public static boolean equalsIgnoreCase(final CharSequence s1, final CharSequence s2) {
        final int length = s1.length();
        if (s2.length() != length) {
            return false;
        }
        for (int i=0; i<length; i++) {
            if (Character.toUpperCase(s1.charAt(i)) != Character.toUpperCase(s2.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns {@code true} if the given string at the given offset contains the given part.
     * The comparison is case-sensitive.
     *
     * @param string The string for which to tests for the presense of {@code part}.
     * @param offset The offset at which {@code part} is to be tested.
     * @param part   The part which may be present in {@code string}.
     * @return {@code true} if {@code string} contains {@code part} at the given {@code offset}.
     * @throws NullPointerException if any of the arguments is null.
     *
     * @see String#regionMatches(int, String, int, int)
     */
    public static boolean regionMatches(final CharSequence string, final int offset, final CharSequence part) {
        final int length = part.length();
        if (offset + length > string.length()) {
            return false;
        }
        for (int i=0; i<length; i++) {
            if (string.charAt(offset + i) != part.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns {@code true} if every characters in the given character sequence are
     * {@linkplain Character#isUpperCase(char) upper-case}.
     *
     * @param  text The character sequence to test.
     * @return {@code true} if every character are upper-case.
     * @throws NullPointerException if the argument is null.
     *
     * @see String#toUpperCase()
     */
    public static boolean isUpperCase(final CharSequence text) {
        return isUpperCase(text, 0, text.length());
    }

    /**
     * Same as {@link #isUpperCase(CharSequence)}, but on a sub-sequence.
     */
    private static boolean isUpperCase(final CharSequence text, int lower, final int upper) {
        while (lower < upper) {
            final char c = text.charAt(lower++);
            if (!Character.isUpperCase(c)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the longuest sequence of characters which is found at the begining of the
     * two given strings. If one of those string is {@code null}, then the other string is
     * returned.
     *
     * @param s1 The first string, or {@code null}.
     * @param s2 The second string, or {@code null}.
     * @return The common prefix of both strings, or {@code null} if both strings are null.
     */
    public static String commonPrefix(final String s1, final String s2) {
        if (s1 == null) return s2;
        if (s2 == null) return s1;
        final String shortest;
        final int lg1 = s1.length();
        final int lg2 = s2.length();
        final int length;
        if (lg1 <= lg2) {
            shortest = s1;
            length = lg1;
        } else {
            shortest = s2;
            length = lg2;
        }
        int i = 0;
        while (i < length) {
            if (s1.charAt(i) != s2.charAt(i)) {
                break;
            }
            i++;
        }
        return shortest.substring(0, i);
    }

    /**
     * Returns the longuest sequence of characters which is found at the end of the two given
     * strings. If one of those string is {@code null}, then the other string is returned.
     *
     * @param s1 The first string, or {@code null}.
     * @param s2 The second string, or {@code null}.
     * @return The common suffix of both strings, or {@code null} if both strings are null.
     */
    public static String commonSuffix(final String s1, final String s2) {
        if (s1 == null) return s2;
        if (s2 == null) return s1;
        final String shortest;
        final int lg1 = s1.length();
        final int lg2 = s2.length();
        final int length;
        if (lg1 <= lg2) {
            shortest = s1;
            length = lg1;
        } else {
            shortest = s2;
            length = lg2;
        }
        int i = 0;
        while (++i <= length) {
            if (s1.charAt(lg1 - i) != s2.charAt(lg2 - i)) {
                break;
            }
        }
        i--;
        return shortest.substring(length - i);
    }

    /**
     * Returns {@code true} if the given character sequence starts with the given prefix.
     *
     * @param sequence    The sequence to test.
     * @param prefix      The expected prefix.
     * @param ignoreCase  {@code true} if the case should be ignored.
     * @return {@code true} if the given sequence starts with the given prefix.
     * @throws NullPointerException if any of the arguments is null.
     */
    public static boolean startsWith(final CharSequence sequence, final CharSequence prefix, final boolean ignoreCase) {
        final int length = prefix.length();
        if (length > sequence.length()) {
            return false;
        }
        for (int i=0; i<length; i++) {
            char c1 = sequence.charAt(i);
            char c2 = prefix.charAt(i);
            if (c1 != c2) {
                if (ignoreCase) {
                    c1 = Character.toLowerCase(c1);
                    c2 = Character.toLowerCase(c2);
                    if (c1 == c2) {
                        continue;
                    }
                }
                return false;
            }
        }
        return true;
    }

    /**
     * Returns {@code true} if the given character sequence ends with the given suffix.
     *
     * @param sequence    The sequence to test.
     * @param suffix      The expected suffix.
     * @param ignoreCase  {@code true} if the case should be ignored.
     * @return {@code true} if the given sequence ends with the given suffix.
     * @throws NullPointerException if any of the arguments is null.
     */
    public static boolean endsWith(final CharSequence sequence, final CharSequence suffix, final boolean ignoreCase) {
        int j = suffix.length();
        int i = sequence.length();
        if (j > i) {
            return false;
        }

        while (--j >= 0) {
            char c1 = sequence.charAt(--i);
            char c2 = suffix.charAt(j);
            if (c1 != c2) {
                if (ignoreCase) {
                    c1 = Character.toLowerCase(c1);
                    c2 = Character.toLowerCase(c2);
                    if (c1 == c2) {
                        continue;
                    }
                }
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the index of the first character after the given number of lines.
     * This method counts the number of occurence of {@code '\n'}, {@code '\r'}
     * or {@code "\r\n"} starting from the given position. When {@code numToSkip}
     * occurences have been found, the index of the first character after the last
     * occurence is returned.
     *
     * @param string    The string in which to skip a determined amount of lines.
     * @param numToSkip The number of lines to skip. Can be positive, zero or negative.
     * @param startAt   Index at which to start the search.
     * @return Index of the first character after the last skipped line.
     */
    public static int skipLines(final CharSequence string, int numToSkip, int startAt) {
        final int length = string.length();
        /*
         * Go backward if the number of lines is negative.
         */
        if (numToSkip < 0) {
            do {
                char c;
                do {
                    if (startAt == 0) {
                        return startAt;
                    }
                    c = string.charAt(--startAt);
                    if (c == '\n') {
                        if (startAt != 0 && string.charAt(startAt - 1) == '\r') {
                            --startAt;
                        }
                        break;
                    }
                } while (c != '\r');
            } while (++numToSkip != 0);
            numToSkip = 1; // For skipping the "end of line" characters.
        }
        /*
         * Skips forward the given amount of lines.
         */
        while (--numToSkip >= 0) {
            char c;
            do {
                if (startAt >= length) {
                    return startAt;
                }
                c = string.charAt(startAt++);
                if (c == '\r') {
                    if (startAt != length && string.charAt(startAt) == '\n') {
                        startAt++;
                    }
                    break;
                }
            } while (c != '\n');
        }
        return startAt;
    }

    /**
     * Returns a {@link String} instance for each line found in a multi-lines string. Each element
     * in the returned array will be a single line. If the given text is already a single line,
     * then this method returns a singleton containing only the given text.
     *
     * @param  text The multi-line text from which to get the individual lines.
     * @return The lines in the text, or {@code null} if the given text was null.
     */
    public static String[] getLinesFromMultilines(final String text) {
        if (text == null) {
            return null;
        }
        /*
         * This method is implemented on top of String.indexOf(int,int), which is the
         * fatest method available while taking care of the complexity of code points.
         */
        int lf = text.indexOf('\n');
        int cr = text.indexOf('\r');
        if (lf < 0 && cr < 0) {
            return new String[] {
                text
            };
        }
        int count = 0;
        String[] splitted = new String[8];
        int last = 0;
        boolean hasMore;
        do {
            int skip = 1;
            final int splitAt;
            if (cr < 0) {
                // There is no "\r" character in the whole text, only "\n".
                splitAt = lf;
                hasMore = (lf = text.indexOf('\n', lf+1)) >= 0;
            } else if (lf < 0) {
                // There is no "\n" character in the whole text, only "\r".
                splitAt = cr;
                hasMore = (cr = text.indexOf('\r', cr+1)) >= 0;
            } else if (lf < cr) {
                // There is both "\n" and "\r" characters with "\n" first.
                splitAt = lf;
                hasMore = true;
                lf = text.indexOf('\n', lf+1);
            } else {
                // There is both "\r" and "\n" characters with "\r" first.
                // We need special care for the "\r\n" sequence.
                splitAt = cr;
                if (lf == ++cr) {
                    cr = text.indexOf('\r', cr+1);
                    lf = text.indexOf('\n', lf+1);
                    hasMore = (cr >= 0 || lf >= 0);
                    skip = 2;
                } else {
                    cr = text.indexOf('\r', cr+1);
                    hasMore = true; // Because there is lf.
                }
            }
            if (count >= splitted.length) {
                splitted = Arrays.copyOf(splitted, count*2);
            }
            splitted[count++] = text.substring(last, splitAt);
            last = splitAt + skip;
        } while (hasMore);
        /*
         * Add the remaining string and we are done.
         */
        if (count >= splitted.length) {
            splitted = Arrays.copyOf(splitted, count+1);
        }
        splitted[count++] = text.substring(last);
        return XArrays.resize(splitted, count);
    }
}
