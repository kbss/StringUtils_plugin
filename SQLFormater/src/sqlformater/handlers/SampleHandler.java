package sqlformater.handlers;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * 
 * @see org.eclipse.core.commands.IHandler
 * @see org.eclipse.core.commands.AbstractHandler
 */
public class SampleHandler extends AbstractHandler {
	private int minWidth = 80;
	private StringBuilder sqlBuilder;

	/**
	 * The constructor.
	 */
	public SampleHandler() {
	}

	/**
	 * the command has been executed, so extract extract the needed information
	 * from the application context.
	 */
	public Object execute(ExecutionEvent event) throws ExecutionException {
		findString();
		return null;
	}

	private void findString() {
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
					parseString(doc, textSel);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private boolean match(String text, String regexp) {
		return Pattern.compile(regexp).matcher(text).find();
	}

	private int findStartlineNum(IDocument doc, int lineNum)
			throws BadLocationException {
		int lineLength = doc.getLineLength(lineNum);
		int lineOffset = doc.getLineOffset(lineNum);
		String lineString = doc.get(lineOffset, lineLength).trim();
		int lineStart = lineNum;
		if (match(lineString, "^(\\+|\\\")")) {
			lineStart = findStartlineNum(doc, lineNum - 1);
		}
		return lineStart;
	}

	private int findEndlineNum(IDocument doc, int lineNum)
			throws BadLocationException {
		int lineLength = doc.getLineLength(lineNum);
		int lineOffset = doc.getLineOffset(lineNum);
		String lineString = doc.get(lineOffset, lineLength).trim();
		int lineStart = lineNum;
		if (!lineString.endsWith(";")) {
			lineStart = findEndlineNum(doc, lineNum + 1);
		}
		return lineStart + 1;
	}

	private String getVariableKey(int num) {
		return "{_" + num + "_}";
	}

	public void parseString(IDocument doc, TextSelection selection)
			throws BadLocationException {
		int position = selection.getOffset();
		doc.getLineOfOffset(position);
		int lineNum = selection.getStartLine();
		int lineStart = findStartlineNum(doc, lineNum);
		int lineEnd = findEndlineNum(doc, lineNum);
		int startLineOffset = doc.getLineOffset(lineStart);
		int endLineOffset = doc.getLineOffset(lineEnd);
		doc.getLineOffset(lineEnd);
		String lineString = doc.get(startLineOffset,
				endLineOffset - startLineOffset).trim();
		System.out.println(lineString);

		Map<String, String> variableMap = new HashMap<String, String>();
		Matcher matcher = Pattern.compile(
				"\"(\\s+|)\\+(\\s+|)\\w+.?\\+(\\s+|)\\\"").matcher(lineString);
		while (matcher.find()) {
			String variableKey = getVariableKey(variableMap.size());
			variableMap.put(matcher.group(), variableKey);
			lineString = lineString.replace(matcher.group(), variableKey);
		}
		
		//\"(\s+|)\+(\s+|)"
		System.out.println(lineString);
		// while (tokenizer.hasMoreTokens()) {
		// String token = tokenizer.nextToken();
		// //\".?(\s+|)\+(\s+|)\w+.*\+(\s+|)\"
		// if(token.startsWith("\"")){}
		// if (!isBeginOfString && token.startsWith("\"")) {
		// isBeginOfString = true;
		// System.out.println(token.substring(1));
		// } else if(isBeginOfString) {
		// System.out.println(token);
		// }
		//
		// }
		// if (match(lineString, "^\\w")) {
		// System.out.println("FFFFFFFFFFFFUUUUU");
		// } else if (match(lineString, "^(\\+|\\\")")) {
		// System.out.println("Not end");
		// }

	}

	/*
	 * 
	 */
	public void tests() {
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
					formateSelectedString(textSel.getText());
					doc.replace(textSel.getOffset(), textSel.getLength(),
							formateSelectedString(textSel.getText()));
					findStartPositionOfDeclaration(doc, textSel);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private String formateSelectedString(String text) {

		StringTokenizer st = new StringTokenizer(text);
		sqlBuilder = new StringBuilder();
		StringBuilder lineBuilder = new StringBuilder();
		int i = 0;
		String element = "";
		String previousElement = "";
		String lastElement = "";
		while (st.hasMoreElements()) {
			element = st.nextToken();

			if (element.equalsIgnoreCase("SELECT")) {
				lineBuilder = terminateLine(lineBuilder, element.toUpperCase());
			} else if (element.equalsIgnoreCase("FROM")) {
				lineBuilder = terminateLine(lineBuilder, element.toUpperCase());
			} else if (element.equalsIgnoreCase("WHERE")) {
				lineBuilder = terminateLine(lineBuilder, element.toUpperCase());
			} else if (element.equalsIgnoreCase("ORDER")) {
				if (st.nextToken().equalsIgnoreCase("BY")) {
					lineBuilder = terminateLine(lineBuilder, "ORDER BY");
				} else {
					return text;
				}
			} else {
				if (st.hasMoreTokens()) {
					lineBuilder.append(element);
				} else if (!element.trim().endsWith("\"")
						|| !element.trim().endsWith(";")) {
					if (element.trim().endsWith(";")) {
						lastElement = ";";
					}
				}

			}
			System.out.print(element + "|");
			i++;
			lineBuilder.append(" ");
			previousElement = element;
		}
		if (element.trim().endsWith("\"")) {
			lineBuilder.delete(lineBuilder.length() - 1, lineBuilder.length());
		}
		lineBuilder = terminateLine(lineBuilder, "");
		System.out.print(sqlBuilder.toString());
		return sqlBuilder.toString();

	}

	private void showError(String title, String error) {

	}

	private StringBuilder terminateLine(StringBuilder lineBuilder, String text) {
		return terminateLine(lineBuilder, text, "+ \" ");
	}

	private StringBuilder terminateLine(StringBuilder lineBuilder, String text,
			String newLineSeparator) {
		String lineTerminator = "\"" + System.getProperty("line.separator");
		sqlBuilder.append(newLineSeparator)
				.append(addSpaces(lineBuilder.toString()))
				.append(lineTerminator);
		return new StringBuilder().append(text);
	}

	private String addSpaces(String text) {
		int width = minWidth - text.length();
		String result = text;
		if (width > 0) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < width; i++) {
				sb.append(" ");
			}
			result = text + sb.toString();
		}
		return result;
	}// + "asdasd

	private int findStartPositionOfDeclaration(IDocument document,
			TextSelection selection) throws BadLocationException {
		int cursorPos = selection.getOffset();
		// // document.getLineDelimiter(selection.getStartLine()).get
		// System.out.println("Start of selection line:"
		// + selection.getStartLine());
		// System.out.println("End of selection line:" +
		// selection.getEndLine());
		// System.out.println("Documnet type:"
		// + document.getContentType(selection.getOffset()).toString());
		// // TypedRegion region =
		// ((TypedRegion)document.getPartition(cursorPos));
		// System.out.println("Char at cursor position:"
		// + document.getChar(cursorPos));

		return 0;
	}

}
