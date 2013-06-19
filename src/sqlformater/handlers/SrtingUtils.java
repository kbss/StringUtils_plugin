package sqlformater.handlers;

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
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.hibernate.engine.jdbc.internal.BasicFormatterImpl;
import org.hibernate.engine.jdbc.internal.Formatter;

import sqlformater.Activator;
import sqlformater.StringUtilsPreferencePage;
import sqlformater.dialog.InfoPopupDialog;

/***************************************************************************
 * Textual utility plugin for Eclipse.
 * 
 * @author Serhii Krivtsov
 ***************************************************************************/
public class SrtingUtils extends AbstractHandler {

    // TODO: should be fetched from editor parameters
    private static final int FORMATER_OFFSET = 12;
    private static final int FORMATER_STRING_OFFSET = 16;
    private static final int FORMATE_SQL_STRING_ACTION = 1;
    private static final int SHOW_STRING_CONTETNT_ACTION = 0;

    /***************************************************************************
     * UnEscapes java special charters.
     * 
     * @param value
     * @return UnEscaped string.
     */
    private static String unescapeJava(String value) {
        String result = null;
        if (value != null) {
            StringWriter writer = null;
            try {
                writer = new StringWriter(value.length());
                unescapeJava(writer, value);
                result = writer.toString();
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            } finally {
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return result;
    }

    /***************************************************************************
     * Returns maximum line width for string. Calculates as
     * {@link AbstractDecoratedTextEditorPreferenceConstants#EDITOR_PRINT_MARGIN_COLUMN}
     * minus {@link #FORMATER_STRING_OFFSET} = {@value #FORMATER_STRING_OFFSET}.
     * 
     * @return calculated maximum line width
     */
    private int getMaximumLineWidth() {
        return EditorsUI
                .getPreferenceStore()
                .getInt(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_PRINT_MARGIN_COLUMN)
                - FORMATER_STRING_OFFSET;
    }

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
                        throw new RuntimeException(
                                "Unable to parse unicode value: " + unicode,
                                nfe);
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
     * @param sqlQuery
     * @param firstLineLenght
     * @return
     */
    private String addQoutes(String sqlQuery, int firstLineLenght) {
        String[] lines = sqlQuery.split("\n");
        if (lines.length < 3) {
            return sqlQuery;
        }
        StringBuilder sb = new StringBuilder();
        int maxLineLength = getMaximumLineWidth();
        String scpaces = addSpaces("", FORMATER_OFFSET);
        for (String line : lines) {
            if (line.length() > maxLineLength) {
                maxLineLength = line.length();
            }
        }

        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                sb.append("\n").append(scpaces).append("+ \"")
                        .append(addSpaces(line, maxLineLength)).append("\"");
            }
        }

        int initialLineLength = firstLineLenght - FORMATER_OFFSET + 1;
        String firstLine = addSpaces("",
                maxLineLength < initialLineLength ? initialLineLength
                        : maxLineLength - initialLineLength);
        return "\"" + firstLine + "\"" + sb.toString();
    }

    /***************************************************************************
     * @param text
     * @param count
     * @return
     */
    private String addSpaces(String text, int count) {
        return repeat(text, " ", count);
    }

    /***************************************************************************
     * the command has been executed, so extract extract the needed information
     * from the application context.
     */
    public Object execute(ExecutionEvent event) throws ExecutionException {
        if ("SQLFormater.commands.showStringContent".equalsIgnoreCase(event
                .getCommand().getId())) {
            performAction(SHOW_STRING_CONTETNT_ACTION);
        } else if ("SQLFormater.commands.formateSQLString"
                .equalsIgnoreCase(event.getCommand().getId())) {
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
    private int findEndlineNum(IDocument doc, int lineNum)
            throws BadLocationException {
        if (doc.getNumberOfLines() - 1 < lineNum) {
            return -1;
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

            if (lineString.endsWith("{") || lineString.endsWith(")")
                    || lineString.endsWith("}")) {
                return -1;
            } else if (lineString.endsWith(";")) {
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
        try {
            int lineLength = doc.getLineLength(lineNum);
            int lineOffset = doc.getLineOffset(lineNum);
            String lineString = doc.get(lineOffset, lineLength).trim();

            if (match(lineString, "^(\\+|\\\")")) {
                lineStart = findStartlineNum(doc, lineNum - 1);
            }
        } catch (BadLocationException e) {
            return -1;
        }
        return lineStart;
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
        String template = "{_%s_}";
        String result = variableName.replaceAll(
                "(\\\"(\\s+|)\\+(\\s+|)|(\\s+|)\\+(\\s+|)\\\")", "");
        result = String.format(template,
                repeat(result, "_", String.format(template, result).length()));
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
    public void proceedSelectedString(IDocument doc, TextSelection selection,
            int action) throws BadLocationException {
        int lineNum = selection.getStartLine();
        int lineStart = findStartlineNum(doc, lineNum);
        int lineEnd = findEndlineNum(doc, lineNum);
        if (lineStart == -1 || lineEnd == -1 || lineStart > lineEnd) {
            return;
        }
        int startLineOffset = doc.getLineOffset(lineStart);
        int endLineOffset = doc.getLineOffset(lineEnd);
        String lineString = doc.get(startLineOffset, endLineOffset
                - startLineOffset);
        int startOfStringOffset = startLineOffset + lineString.indexOf("\"");
        int endOfStringLength = lineString.lastIndexOf("\"") + 1
                - lineString.indexOf("\"");
        Map<String, String> variableMap = new HashMap<String, String>();
        lineString = lineString.replaceAll("\\\"(\\s+|)\\+(\\s+|)\\\"", "");
        Matcher matcher = Pattern.compile(
                "\"(\\s+|)\\+(\\s+|)\\w+.?(\\s+|)\\+(\\s+|)\\\"").matcher(
                lineString);
        while (matcher.find()) {
            String variableKey = getVariableKey(variableMap.size(),
                    matcher.group());

            variableMap.put(variableKey, matcher.group());

            lineString = lineString.replace(matcher.group(), variableKey);
        }
        Matcher stringMatcher = Pattern.compile("(?<=\\\").*(?=\")").matcher(
                lineString);

        if (stringMatcher.find()) {
            Formatter sqlFormater = new BasicFormatterImpl(getPreferenceStore()
                    .getBoolean(StringUtilsPreferencePage.CLAUSE_TO_UPPERCASE));

            String formatedString = sqlFormater.format(stringMatcher.group());
            if (action == FORMATE_SQL_STRING_ACTION) {
                String result = addQoutes(formatedString,
                        lineString.indexOf("\""));
                if (formatedString.split("\n").length > 2) {
                    for (Entry<String, String> entry : variableMap.entrySet()) {
                        result = result.replace(entry.getKey(),
                                entry.getValue());
                    }
                    replaceTextInEditor(startOfStringOffset, endOfStringLength,
                            result);
                }

            } else if (action == SHOW_STRING_CONTETNT_ACTION) {
                for (Entry<String, String> entry : variableMap.entrySet()) {
                    formatedString = formatedString
                            .replace(entry.getKey(), "?");
                }
                String content;
                if (getPreferenceStore().getBoolean(
                        StringUtilsPreferencePage.FORMATTE_SQL_ON_EXTRACT)) {
                    content = formatedString;
                } else {
                    content = stringMatcher.group();
                }
                unescapeAndShow(content);
            }
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
            IEditorPart part = PlatformUI.getWorkbench()
                    .getActiveWorkbenchWindow().getActivePage()
                    .getActiveEditor();
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
        } catch (Exception ex) {
            ex.printStackTrace();
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
            IEditorPart part = PlatformUI.getWorkbench()
                    .getActiveWorkbenchWindow().getActivePage()
                    .getActiveEditor();
            if (part instanceof ITextEditor) {
                final ITextEditor editor = (ITextEditor) part;
                IDocumentProvider prov = editor.getDocumentProvider();
                IDocument doc = prov.getDocument(editor.getEditorInput());
                ISelection sel = editor.getSelectionProvider().getSelection();
                if (sel instanceof TextSelection) {
                    doc.replace(offset, length, text);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /***************************************************************************
     * @param startOffset
     * @param length
     */
    public void selectText(int startOffset, int length) {
        IEditorPart part = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                .getActivePage().getActiveEditor();
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
        InfoPopupDialog infoPopup = new InfoPopupDialog(Display.getCurrent()
                .getActiveShell(), PopupDialog.INFOPOPUP_SHELLSTYLE, true,
                false, false, false, false, SrtingUtils.class.getSimpleName(),
                unescapeJava(text).trim().replaceAll("\\{\\_\\d.?\\_\\}", "?"));
        infoPopup.open();
    }
}