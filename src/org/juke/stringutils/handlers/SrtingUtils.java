/*
 * StringUtils - <a
 * href="https://github.com/kbss/StringUtils_plugin">https://github.com/kbss/StringUtils_plugin</a><br>
 * 
 * Copyright (C) 2013 Serhii Krivtsov<br>
 * 
 * SQLPatcher is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.<br>
 * <br>
 * StringUtils is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>. <br>
 * 
 * @author Serhii Krivtsov
 */
package org.juke.stringutils.handlers;

import java.io.Closeable;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.hibernate.engine.jdbc.internal.BasicFormatterImpl;
import org.hibernate.engine.jdbc.internal.Formatter;
import org.juke.stringutils.Activator;
import org.juke.stringutils.StringUtilsPreferencePage;
import org.juke.stringutils.dialog.InfoPopupDialog;

/***************************************************************************
 * String utility plug-in for Eclipse.
 * 
 * @author Serhii Krivtsov
 ***************************************************************************/
// TODO: Add REGEXP description
public class SrtingUtils extends AbstractHandler {

    private static final int INVALID_LINE_NUM = -1;

    private static final int PART_COUNT = 2;

    private static final String REPLACEMENT_KEY = "?";

    private static final String WHITESPACE_REGEXP = "\"(\\s+|)\\+(\\s+|)\\w+.?(\\s+|)\\+(\\s+|)\\\"";

    private static final String LINE_REGEXP = "\\\"(\\s+|)\\+(\\s+|)\\\"";

    private static final Pattern PATTERN = Pattern.compile("(?<=\\\").*(?=\")");

    private static final String UNESCAPE_REGEXP = "\\{\\_\\d.?\\_\\}";

    private static final String REPLACMENT_SYMBOL = "_";

    private static final String TEMPLATE_KEY = "{_%s_}";

    private static final String REPLACMENT_REGEXP = "(\\\"(\\s+|)\\+(\\s+|)|(\\s+|)\\+(\\s+|)\\\")";

    private static final String NEW_LINE_REGEXP = "^(\\+|\\\")";

    private static final String STRING_NEW_LINE = "+ \"";

    private static final String SEMICOLON = ";";

    private static final String BRACE = ")";

    private static final String CLOSING_BRACE = "}";

    private static final String OPENING_BRACE = "{";

    private static final String COMMANDS_FORMATE_SQL_STRING = "SQLFormater.commands.formateSQLString";

    private static final String COMMANDS_SHOW_STRING_CONTENT = "SQLFormater.commands.showStringContent";

    private static final String SINGL_SPACE = " ";

    private static final String QUOTE = "\"";

    private static final String EMPTY_STRING = "";

    private static final String NEW_LINE = "\n";

    private static final int FORMATE_SQL_STRING_ACTION = 1;

    private static final int SHOW_STRING_CONTETNT_ACTION = 0;

    public SrtingUtils() {
        new StringUtilsPreferencePage().init(PlatformUI.getWorkbench());
    }

    /***************************************************************************
     * UnEscapes java special charters.
     * 
     * @param stringValue
     * @return UnEscaped string.
     */
    private static String unescapeJava(String stringValue) {
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

    /***************************************************************************
     * Returns maximum line width for string. Calculates as
     * {@link AbstractDecoratedTextEditorPreferenceConstants#EDITOR_PRINT_MARGIN_COLUMN}
     * minus {@link #FORMATER_STRING_OFFSET} = {@value #FORMATER_STRING_OFFSET}.
     * 
     * @return calculated maximum line width
     */
    private int getMaximumLineWidth() {
        return getPreferenceStore().getInt(StringUtilsPreferencePage.LINE_WIDTH);
    }

    // TODO: FIXME: Use Apache StringEscapeUtils
    /***************************************************************************
     * Unescapes any Java literals found in the <code>String</code> to a
     * <code>Writer</code>. </p>
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
    public static void unescapeJava(Writer out, String str) throws IOException {
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

    /***************************************************************************
     * Returns Eclipse Preference Store.
     * 
     * @return Eclipse Preference Store.
     */
    private IPreferenceStore getPreferenceStore() {
        return Activator.getDefault().getPreferenceStore();
    }

    /***************************************************************************
     * Add quotes to given SQL query.
     * 
     * @param sqlQuery
     * @param firstLineLenght
     * @return
     */
    private String addQoutes(String sqlQuery, int firstLineLenght) {
        String[] lines = sqlQuery.split(NEW_LINE);
        if (lines.length < 3) {
            return sqlQuery;
        }

        int formaterOffset = getPreferenceStore().getInt(StringUtilsPreferencePage.NEXT_LINE_OFFSET);
        int maxLineLength = getMaximumLineWidth();
        String scpaces = addSpaces(EMPTY_STRING, formaterOffset);
        for (String line : lines) {
            if (line.length() > maxLineLength) {
                maxLineLength = line.length();
            }
        }
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                sb.append(NEW_LINE).append(scpaces).append(STRING_NEW_LINE).append(addSpaces(line, maxLineLength)).append(QUOTE);
            }
        }

        int initialLineLength = firstLineLenght - formaterOffset;
        String firstLine = addSpaces(EMPTY_STRING, (maxLineLength < initialLineLength ? initialLineLength : maxLineLength - initialLineLength)
                + getPreferenceStore().getInt(StringUtilsPreferencePage.SQL_INITIAL_LINE_OFFSET));
        return QUOTE + firstLine + QUOTE + sb.toString();
    }

    /***************************************************************************
     * Adds number of given spaces to given string
     * 
     * @param text
     * @param count
     *            space count to add
     * @return given string with added spaces
     */
    private String addSpaces(String text, int count) {
        return repeat(text, SINGL_SPACE, count);
    }

    /***************************************************************************
     * the command has been executed, so extract extract the needed information
     * from the application context.
     */
    public Object execute(ExecutionEvent event) throws ExecutionException {
        if (COMMANDS_SHOW_STRING_CONTENT.equalsIgnoreCase(event.getCommand().getId())) {
            performAction(SHOW_STRING_CONTETNT_ACTION);
        } else if (COMMANDS_FORMATE_SQL_STRING.equalsIgnoreCase(event.getCommand().getId())) {
            performAction(FORMATE_SQL_STRING_ACTION);
        }
        return null;
    }

    /***************************************************************************
     * Finds end line of field declaration regarding to given line number.
     * 
     * @param doc
     * @param lineNum
     * @return line number of field declaration end, regarding to given line
     *         number.
     * @throws BadLocationException
     */
    private int findEndlineNum(IDocument doc, int lineNum) throws BadLocationException {
        if (doc.getNumberOfLines() - 1 < lineNum) {
            return INVALID_LINE_NUM;
        }
        int lineLength;
        int lineOffset;
        String lineString;
        int lineEnd = lineNum;
        boolean isNotFound = true;
        do {
            lineLength = doc.getLineLength(lineEnd);
            lineOffset = doc.getLineOffset(lineEnd);
            lineString = doc.get(lineOffset, lineLength).trim();

            if (lineString.endsWith(OPENING_BRACE) || lineString.endsWith(BRACE) || lineString.endsWith(CLOSING_BRACE)) {
                return INVALID_LINE_NUM;
            } else if (lineString.endsWith(SEMICOLON)) {
                isNotFound = false;
                break;
            }
            lineEnd++;
        } while (isNotFound);
        return lineEnd + 1;
    }

    /***************************************************************************
     * Finds start line of field declaration regarding to given line number.
     * 
     * @param doc
     *            Text Editor document.
     * @param lineNum
     * @return start line number of field declaration, regarding to given line
     *         number.
     */
    private int findStartlineNum(IDocument doc, int lineNum) {
        if (lineNum == 0) {
            return 0;
        }
        int lineStart = lineNum;
        String lineString;
        try {
            do {
                int lineLength = doc.getLineLength(lineStart);
                int lineOffset = doc.getLineOffset(lineStart);
                lineString = doc.get(lineOffset, lineLength).trim();
                lineStart--;
            } while (match(lineString, NEW_LINE_REGEXP));
        } catch (BadLocationException e) {
            return INVALID_LINE_NUM;
        }
        return lineStart--;
    }

    /***************************************************************************
     * Creates replacement key for given variable.
     * 
     * @param id
     *            variable id
     * @param variableName
     * @return String key for given variable.
     */
    private String getVariableKey(int id, String variableName) {
        String result = variableName.replaceAll(REPLACMENT_REGEXP, EMPTY_STRING);
        result = String.format(TEMPLATE_KEY, repeat(result, REPLACMENT_SYMBOL, String.format(TEMPLATE_KEY, result).length()));
        return result;
    }

    /***************************************************************************
     * Tests given text by given RegExp pattern.
     * 
     * @param text
     *            text for test
     * @param regexp
     *            RegExp pattern
     * @return true if find the subsequence of the input sequence that matches
     *         the pattern
     */
    private boolean match(String text, String regexp) {
        return Pattern.compile(regexp).matcher(text).find();
    }

    /***************************************************************************
     * Performs actions on selected strings variable.
     * 
     * @param doc
     *            Text Editor document
     * @param selection
     *            {@link TextSelection} in current document
     * @param action
     *            The action that will be performed on selected string.
     * @throws BadLocationException
     */
    public void proceedSelectedString(IDocument doc, TextSelection selection, int action) throws BadLocationException {
        int lineNum = selection.getStartLine();
        int lineStart = findStartlineNum(doc, lineNum);
        int lineEnd = findEndlineNum(doc, lineNum);
        if (lineStart == INVALID_LINE_NUM || lineEnd == INVALID_LINE_NUM || lineStart > lineEnd) {
            return;
        }
        int startLineOffset = doc.getLineOffset(lineStart);
        int endLineOffset = doc.getLineOffset(lineEnd);
        String lineString = doc.get(startLineOffset, endLineOffset - startLineOffset);
        int startOfStringOffset = startLineOffset + lineString.indexOf(QUOTE);
        int endOfStringLength = lineString.lastIndexOf(QUOTE) + 1 - lineString.indexOf(QUOTE);
        Map<String, String> variableMap = new HashMap<String, String>();
        lineString = lineString.replaceAll(LINE_REGEXP, EMPTY_STRING);
        Matcher matcher = Pattern.compile(WHITESPACE_REGEXP).matcher(lineString);
        while (matcher.find()) {
            String variableKey = getVariableKey(variableMap.size(), matcher.group());
            variableMap.put(variableKey, matcher.group());
            lineString = lineString.replace(matcher.group(), variableKey);
        }
        Matcher stringMatcher = PATTERN.matcher(lineString);

        if (stringMatcher.find()) {
            Formatter sqlFormater = new BasicFormatterImpl(getPreferenceStore().getBoolean(StringUtilsPreferencePage.CLAUSE_TO_UPPERCASE));

            String formatedString = sqlFormater.format(stringMatcher.group());
            if (action == FORMATE_SQL_STRING_ACTION) {
                fornatSqkString(lineString, startOfStringOffset, endOfStringLength, variableMap, formatedString);

            } else if (action == SHOW_STRING_CONTETNT_ACTION) {
                showStringContent(variableMap, stringMatcher, formatedString);
            }
        }
    }

    private void showStringContent(Map<String, String> variableMap, Matcher stringMatcher, String formatedString) {
        for (Entry<String, String> entry : variableMap.entrySet()) {
            formatedString = formatedString.replace(entry.getKey(), REPLACEMENT_KEY);
        }
        String content;
        if (getPreferenceStore().getBoolean(StringUtilsPreferencePage.FORMATTE_SQL_ON_EXTRACT)) {
            content = formatedString;
        } else {
            content = stringMatcher.group();
        }
        unescapeAndShow(content);
    }

    private void fornatSqkString(String lineString, int startOfStringOffset, int endOfStringLength, Map<String, String> variableMap, String formatedString) {
        String result = addQoutes(formatedString, lineString.indexOf(QUOTE));
        if (formatedString.split(NEW_LINE).length > PART_COUNT) {
            for (Entry<String, String> entry : variableMap.entrySet()) {
                result = result.replace(entry.getKey(), entry.getValue());
            }
            replaceTextInEditor(startOfStringOffset, endOfStringLength, result);
        }
    }

    /***************************************************************************
     * Performs given action on selected in editor string.<br/>
     * Available actions:<br/>
     * <li>
     * {@link #FORMATE_SQL_STRING_ACTION} - Formats selected in editor SQL query
     * string with {@link BasicFormatterImpl#format(String)}</li> <li>
     * {@link #SHOW_STRING_CONTETNT_ACTION} - shows UnQouted, UnEscaped string
     * content of selected in editor string.</li>
     * 
     * @param action
     *            action id.
     * 
     * @see BasicFormatterImpl
     */
    private void performAction(int action) {

        try {
            IEditorPart part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
            if (part instanceof ITextEditor) {
                final ITextEditor editor = (ITextEditor) part;
                IDocumentProvider prov = editor.getDocumentProvider();
                IDocument doc = prov.getDocument(editor.getEditorInput());
                ISelection sel = editor.getSelectionProvider().getSelection();
                if (sel instanceof TextSelection) {
                    final TextSelection textSel = (TextSelection) sel;
                    proceedSelectedString(doc, textSel, action);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /***************************************************************************
     * @param text
     * @param chr
     * @param count
     * @return
     */
    private String repeat(String text, String chr, int count) {
        int width = count - text.length();
        String result = text;
        if (width > 0) {
            StringBuilder sb = new StringBuilder().append(text);
            for (int i = 0; i < width; i++) {
                sb.append(chr);
            }
            result = sb.toString();
        }
        return result;
    }

    /***************************************************************************
     * Replace text in Text Editor starts from given offset till specified
     * length with given text.
     * 
     * @param offset
     *            offset regarding to Text Editor
     * @param length
     *            of replacement in editor.
     * @param text
     *            text of replacement.
     */
    private void replaceTextInEditor(int offset, int length, String text) {
        try {
            IEditorPart part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
            if (part instanceof ITextEditor) {
                final ITextEditor editor = (ITextEditor) part;
                IDocumentProvider prov = editor.getDocumentProvider();
                IDocument doc = prov.getDocument(editor.getEditorInput());
                ISelection sel = editor.getSelectionProvider().getSelection();
                if (sel instanceof TextSelection) {
                    doc.replace(offset, length, text);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /***************************************************************************
     * @param startOffset
     * @param length
     */
    public void selectText(int startOffset, int length) {
        IEditorPart part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
        if (part instanceof ITextEditor) {
            final ITextEditor editor = (ITextEditor) part;
            editor.selectAndReveal(startOffset, length);
        }
    }

    /***************************************************************************
     * UnEscape given java string and show it in window.
     * 
     * @param text
     *            java string.
     */
    private void unescapeAndShow(String text) {
        InfoPopupDialog infoPopup = new InfoPopupDialog(Display.getCurrent().getActiveShell(), PopupDialog.INFOPOPUP_SHELLSTYLE, true, false, false, false,
                false, SrtingUtils.class.getSimpleName(), unescapeJava(text).trim().replaceAll(UNESCAPE_REGEXP, REPLACEMENT_KEY));
        infoPopup.open();
    }
}