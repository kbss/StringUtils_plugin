package sqlformater.dialog;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/***************************************************************************
 * Pop-up dialog with text area field.
 * 
 * @author Serhii Krivtsov
 ***************************************************************************/
public class InfoPopupDialog extends PopupDialog {
    private String informationText;
    private int FONT_SIZE = 11;
    private InfoPopupDialog instance;

    public InfoPopupDialog(Shell parent, int shellStyle,
            boolean takeFocusOnOpen, boolean persistSize,
            boolean persistLocation, boolean showDialogMenu,
            boolean showPersistActions, String titleText, String infoText) {
        super(parent, shellStyle, takeFocusOnOpen, persistSize,
                persistLocation, showDialogMenu, showPersistActions, titleText,
                "Content of string");
        this.informationText = infoText;
        instance = this;
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);
        Text text = new Text(composite, SWT.MULTI | SWT.BORDER | SWT.WRAP
                | SWT.V_SCROLL);
        text.setText(informationText);
        text.setFont(new Font(getShell().getDisplay(), "Courier New",
                FONT_SIZE, SWT.NORMAL));
        text.setLayoutData(new GridData(GridData.FILL_BOTH));
        Link link = new Link(composite, SWT.NO);

        link.setText("<a>Copy to clipboard</a>");
        // link.setFont( new Font(getShell().getDisplay(), "Courier New", 9,
        // SWT.UNDERLINE_LINK));
        link.setTouchEnabled(true);
        instance.setShellStyle(HOVER_SHELLSTYLE);
        link.addMouseListener(new MouseListener() {

            @Override
            public void mouseUp(MouseEvent e) {
            }

            @Override
            public void mouseDown(MouseEvent e) {
                StringSelection stringSelection = new StringSelection(
                        informationText);
                Clipboard clpbrd = Toolkit.getDefaultToolkit()
                        .getSystemClipboard();
                clpbrd.setContents(stringSelection, null);
                instance.close();
            }

            @Override
            public void mouseDoubleClick(MouseEvent e) {
            }
        });
        return composite;
    }
}
