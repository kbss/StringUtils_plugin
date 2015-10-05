package org.forsp.strinhelper;

import java.io.Closeable;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

/**
 * 
 * @author skrivtsov
 *
 */
public class StringUtils {

	private static final char SLASH = '\\';
	private static final char QUOTE = '"';

	public int[] getStringBound(String text, int offset) {
		if (text == null || offset < 0 || text.length() < offset) {
			return null;
		}
		char[] chars = text.toCharArray();
		if (chars.length > offset) {
			int startPoss = -1;
			for (int i = offset - 1; i > -1; i--) {
				if (isQuoteBound(chars, i)) {
					startPoss = i;
					break;
				}
			}
			if (startPoss == -1) {
				return null;
			}
			int endPoss = -1;
			for (int i = offset; i < chars.length; i++) {
				if (isQuoteBound(chars, i)) {
					endPoss = i + 1;
					break;
				}
			}
			if (!isValidStringBound(chars, endPoss)) {
				return null;
			}
			if (startPoss != -1) {
				return new int[] { startPoss, endPoss };
			}
		}
		return null;
	}

	/**
	 * Validates if founded string bound is valid
	 * 
	 * @param chars
	 *            - char sequence
	 * @param endPoss
	 *            last correct quote position
	 * @return true if founded bound is correct
	 */
	private boolean isValidStringBound(char[] chars, int endPoss) {
		int quoteCount = 0;
		for (int i = endPoss; i < chars.length; i++) {
			if (isQuoteBound(chars, i)) {
				quoteCount++;
			}
		}
		return quoteCount % 2 == 0;
	}

	private boolean isQuoteBound(char[] chars, int position) {
		char cur = chars[position];
		if (cur == QUOTE) {
			if (!isQuoteEscaped(chars, position)) {
				return true;
			}
		}
		return false;
	}

	private boolean isQuoteEscaped(char[] chars, int position) {
		int slashCount = 0;
		for (int i = position - 1; i > -1; i--) {
			char c = getCharAtPos(chars, i);
			if (c != SLASH) {
				if (slashCount == 0) {
					return false;
				}
				break;
			} else if (c == SLASH) {
				slashCount++;
			}
		}
		return slashCount % 2 != 0;
	}

	private char getCharAtPos(char[] chrs, int possition) {
		if (possition >= 0) {
			return chrs[possition];
		}
		return '0';
	}

	/***************************************************************************
	 * UnEscapes java special charters.
	 * 
	 * @param stringValue
	 * @return UnEscaped string.
	 */
	public String unescapeJava(String stringValue) {
		String result = null;
		if (stringValue != null) {
			StringWriter writer = null;
			try {
				writer = new StringWriter(stringValue.length());
				unescapeJava(writer, stringValue);
				result = writer.toString();
			} catch (IOException ioe) {
				// TODO: Create custom exception
				throw new RuntimeException(ioe);
			} finally {
				closeQuite(writer);
			}
		}
		return result;
	}

	/**
	 * Closes this stream and releases any system resources associated with it.
	 * If the stream is already closed then invoking this method has no effect.
	 * 
	 * IOException will be suppressed.
	 */
	private static void closeQuite(Closeable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (IOException e) {
				// DO NOTHING
			}
		}
	}

	// TODO: FIXME: Use Apache StringEscapeUtils
	/***************************************************************************
	 * Unescapes any Java literals found in the <code>String</code> to a
	 * <code>Writer</code>.
	 * </p>
	 * 
	 * <p>
	 * For example, it will turn a sequence of <code>'\'</code> and
	 * <code>'n'</code> into a newline character, unless the <code>'\'</code> is
	 * preceded by another <code>'\'</code>.
	 * </p>
	 * 
	 * <p>
	 * A <code>null</code> string input has no effect.
	 * </p>
	 * 
	 * @param out
	 *            the <code>Writer</code> used to output unescaped characters
	 * @param str
	 *            the <code>String</code> to unescape, may be null
	 * @throws IllegalArgumentException
	 *             if the Writer is <code>null</code>
	 * @throws IOException
	 *             if error occurs on underlying Writer
	 */
	private void unescapeJava(Writer out, String str) throws IOException {
		if (out == null) {
			throw new IllegalArgumentException("The Writer must not be null");
		}
		if (str == null) {
			return;
		}
		int sz = str.length();
		StringBuffer unicode = new StringBuffer(4);
		boolean hadSlash = false;
		boolean inUnicode = false;
		for (int i = 0; i < sz; i++) {
			char ch = str.charAt(i);
			if (inUnicode) {
				// if in unicode, then we're reading unicode
				// values in somehow
				unicode.append(ch);
				if (unicode.length() == 4) {
					// unicode now contains the four hex digits
					// which represents our unicode character
					try {
						int value = Integer.parseInt(unicode.toString(), 16);
						out.write((char) value);
						unicode.setLength(0);
						inUnicode = false;
						hadSlash = false;
					} catch (NumberFormatException nfe) {
						throw new RuntimeException("Unable to parse unicode value: " + unicode, nfe);
					}
				}
				continue;
			}
			if (hadSlash) {
				// handle an escaped value
				hadSlash = false;
				switch (ch) {
				case '\\':
					out.write('\\');
					break;
				case '\'':
					out.write('\'');
					break;
				case '\"':
					out.write('"');
					break;
				case 'r':
					out.write('\r');
					break;
				case 'f':
					out.write('\f');
					break;
				case 't':
					out.write('\t');
					break;
				case 'n':
					out.write('\n');
					break;
				case 'b':
					out.write('\b');
					break;
				case 'u': {
					// uh-oh, we're in unicode country....
					inUnicode = true;
					break;
				}
				default:
					out.write(ch);
					break;
				}
				continue;
			} else if (ch == '\\') {
				hadSlash = true;
				continue;
			}
			out.write(ch);
		}
		if (hadSlash) {
			// then we're in the weird case of a \ at the end of the
			// string, let's output it anyway.
			out.write('\\');
		}
	}
}