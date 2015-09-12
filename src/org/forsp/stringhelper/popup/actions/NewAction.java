package org.forsp.stringhelper.popup.actions;

import java.io.Closeable;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.juke.stringutils.dialog.InfoPopupDialog;

public class NewAction implements IObjectActionDelegate {

	private static final int INVALID_LINE_NUM = -1;

	private static final String REPLACEMENT_KEY = "?";

	private static final String LINE_REGEXP = "\\\"(\\s+|)\\+(\\s+|)\\\"";

	private static final Pattern PATTERN = Pattern.compile("(?<=\\\").*(?=\")");

	private static final Pattern BAD_POSSITION_PATTERN = Pattern.compile("^(\\;|\\,|\\)\\;)$");

	private static final String UNESCAPE_REGEXP = "\\{\\_\\d.?\\_\\}";

	private static final Pattern NEW_LINE_REGEXP = Pattern.compile("^(\\+|\\\")");

	private static final Pattern END_OF_LIEN_PATTERN = Pattern.compile("(\\{|\\}|\\;)$");

	private static final String EMPTY_STRING = "";

	private static final int SHOW_STRING_CONTETNT_ACTION = 0;

	private static final String CURSOR_TOKEN = String.format("${%s}", System.currentTimeMillis());

	private static final String QUOTE_ESCAPE = "@{#!}";

	private static final Pattern CURSOR_PATTERN = Pattern
			.compile("(\\\".*?" + Pattern.quote(CURSOR_TOKEN) + ".*?)(\\\"|$)");

	private static final String SLASH_SAFE_REPLCER = "121";

	private Shell shell;

	/**
	 * Constructor for Action1.
	 */
	public NewAction() {
		super();

	}

	/**
	 * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		shell = targetPart.getSite().getShell();
	}

	/**
	 * @see IActionDelegate#run(IAction)
	 */
	public void run(IAction action) {
		try {
			IEditorPart part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
			if (part instanceof ITextEditor) {
				final ITextEditor editor = (ITextEditor) part;
				IDocumentProvider prov = editor.getDocumentProvider();
				IDocument doc = prov.getDocument(editor.getEditorInput());
				ISelection sel = editor.getSelectionProvider().getSelection();
				if (sel instanceof TextSelection) {
					final TextSelection textSel = (TextSelection) sel;
					proceedSelectedString(doc, textSel, SHOW_STRING_CONTETNT_ACTION);
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
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
		for (int i = lineNum; i < doc.getNumberOfLines(); i++) {
			int lineLength = doc.getLineLength(i);
			int lineOffset = doc.getLineOffset(i);
			String lineString = doc.get(lineOffset, lineLength).trim();
			if (match(lineString, END_OF_LIEN_PATTERN)) {
				return i + 1;
			}
		}
		return INVALID_LINE_NUM;
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
		if (lineNum > 0) {
			try {
				String prevString = EMPTY_STRING;
				for (int i = lineNum; i > 1; i--) {
					int lineLength = doc.getLineLength(i);
					int lineOffset = doc.getLineOffset(i);
					String lineString = doc.get(lineOffset, lineLength).trim();
					debug(lineString);
					// Strings separated with comma with new line
					// e.g doStuff("text1",[$newline] "text2")
					if (prevString.startsWith("\"") && lineString.endsWith(",")) {
						return i + 1;
					}
					if (!match(lineString, NEW_LINE_REGEXP)) {
						return i;
					}
					prevString = lineString;
				}
			} catch (BadLocationException e) {
				return INVALID_LINE_NUM;
			}
		}
		return lineNum;
	}

	private boolean match(String text, Pattern regexp) {
		return regexp.matcher(text).find();
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
		int cursorOffset = selection.getOffset() - startLineOffset;
		boolean cursorBound = false;
		if (cursorBound = lineString.length() > cursorOffset) {
			lineString = lineString.substring(0, cursorOffset) + CURSOR_TOKEN + lineString.substring(cursorOffset);
		}
		lineString = lineString.replaceAll(LINE_REGEXP, EMPTY_STRING);
		if (cursorBound) {
			// Replace escaped quotes
			lineString = lineString.replace("\\\\", SLASH_SAFE_REPLCER);
			lineString = lineString.replace("\\\"", QUOTE_ESCAPE);
			Matcher mat = CURSOR_PATTERN.matcher(lineString);
			if (mat.find()) {
				lineString = mat.group(1).replaceAll(".*\"", "\"");
				lineString = lineString.replace(CURSOR_TOKEN, "") + "\"";
			}
		}

		Matcher stringMatcher = PATTERN.matcher(lineString);
		if (stringMatcher.find()) {
			lineString = stringMatcher.group();
			lineString = lineString.replace(SLASH_SAFE_REPLCER, "\\\\");
			lineString = lineString.replaceAll(Pattern.quote(QUOTE_ESCAPE), "\"");
			// unescape
			debug(lineString);
			if (match(lineString, BAD_POSSITION_PATTERN)) {
				debug("Bad possition, exit");
				return;
			}
			unescapeAndShow(lineString);
		}
	}

	private void debug(Object msg) {
		System.out.println(msg);
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
		InfoPopupDialog infoPopup = new InfoPopupDialog(Display.getCurrent().getActiveShell(),
				PopupDialog.INFOPOPUP_SHELLSTYLE, true, true, false, false, false, "String content extractor",
				unescapeJava(text).trim().replaceAll(UNESCAPE_REGEXP, REPLACEMENT_KEY));
		infoPopup.open();
	}

	/**
	 * @see IActionDelegate#selectionChanged(IAction, ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
	}

}
