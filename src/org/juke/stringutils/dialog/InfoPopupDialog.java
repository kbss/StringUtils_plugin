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
package org.juke.stringutils.dialog;

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
        instance.setShellStyle(HOVER_SHELLSTYLE);
        link.addMouseListener(new MouseListener() {

            @Override
            public void mouseUp(MouseEvent e) {
            }

            @Override
            public void mouseDown(MouseEvent e) {
                StringSelection stringSelection = new StringSelection(
                        informationText);
                Clipboard clipboard = Toolkit.getDefaultToolkit()
                        .getSystemClipboard();
                clipboard.setContents(stringSelection, null);
                instance.close();
            }

            @Override
            public void mouseDoubleClick(MouseEvent e) {
            }
        });
        return composite;
    }
}
