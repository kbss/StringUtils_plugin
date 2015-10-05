package org.forsp.stringhelper.popup.actions;

import java.util.UUID;
import java.util.regex.Pattern;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.forsp.strinhelper.StringUtils;
import org.juke.stringutils.dialog.InfoPopupDialog;

public class NewAction implements IObjectActionDelegate {

	private static final int INVALID_LINE_NUM = -1;

	private static final String REPLACEMENT_KEY = "?";

	private static final String UNESCAPE_REGEXP = "\\{\\_\\d.?\\_\\}";

	private static final Pattern NEW_LINE_REGEXP = Pattern.compile("^(\\+|\\\")");

	private static final Pattern END_OF_LIEN_PATTERN = Pattern.compile("(\\{|\\}|\\;)$");

	private static final String EMPTY_STRING = "";

	private static final int SHOW_STRING_CONTETNT_ACTION = 0;

	private static final String CURSOR_TOKEN = String.format("${%s}", UUID.randomUUID());

	private static final Pattern STRING_CONCAT_PATTERN = Pattern.compile("(\\\"\\s+\\+?\\s+\\\"?)");

	private StringUtils stringSearcher;

	/**
	 * Constructor for Action1.
	 */
	public NewAction() {
		super();
		stringSearcher = new StringUtils();
	}

	/**
	 * @see IObjectActionDelegate#setActivePart(IAction, IWorkbenchPart)
	 */
	@Override
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
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
	private void proceedSelectedString(IDocument doc, TextSelection selection, int action) throws BadLocationException {
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
		if (lineString.length() > cursorOffset) {
			lineString = lineString.substring(0, cursorOffset) + CURSOR_TOKEN + lineString.substring(cursorOffset);
		}
		lineString = replaceStringConcat(lineString);
		int offset = lineString.indexOf(CURSOR_TOKEN);
		lineString = lineString.replace(CURSOR_TOKEN, "");
		int[] stringBound = stringSearcher.getStringBound(lineString, offset);
		if (stringBound == null) {
			return;
		}
		lineString = lineString.substring(stringBound[0] + 1, stringBound[1] - 1);
		unescapeAndShow(lineString);
	}

	private String replaceStringConcat(String str) {
		return STRING_CONCAT_PATTERN.matcher(str).replaceAll("");
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
				stringSearcher.unescapeJava((text).trim().replaceAll(UNESCAPE_REGEXP, REPLACEMENT_KEY)));
		infoPopup.open();
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
	}
}
