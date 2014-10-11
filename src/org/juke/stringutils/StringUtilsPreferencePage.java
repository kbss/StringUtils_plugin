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
package org.juke.stringutils;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/***************************************************************************
 * {@link FieldEditorPreferencePage} for StringUtils plug-in.
 * 
 * @author Serhii Krivtsov
 ***************************************************************************/
public class StringUtilsPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    private static final String LINE_WIDTH_DEFAULT_VALUE = "64";
    private static final String NEXT_LINE_OFFSET_DEFAULT_VALUE = "12";
    private static final String INITIAL_OFFSET_VALUE = "2";
    private static final String TRUE_STRING_VALUE = "true";
    private static final String PLUGIN_TITLE = "String utility settings";
    private static final String LINE_WIDTH_LABEL = "Line width";
    private static final String SQL_INITIAL_LINE_OFFSET_LABEL = "SQL initial line Offset";
    private static final String NEXT_LINE_OFFSET_LABEL = "Next line offset";
    private static final String FORMATTE_SQL_QUERY_ON_EXTRACT = "&Formatte SQL query on extract";
    private static final String CONVER_SQL_CLAUSES_TO_UPPER_CASE = "&Conver SQL clauses to upper case";
    public static final String CLAUSE_TO_UPPERCASE = "clauseToUppercase";
    public static final String FORMATTE_SQL_ON_EXTRACT = "formatteSqlOnExtract";
    public static final String SQL_INITIAL_LINE_OFFSET = "sqlQueryOffset";
    public static final String STRING_BORDER_OFFSET = "stringBorderOffset";
    public static final String NEXT_LINE_OFFSET = "nextLineOffset";

    public static final String LINE_WIDTH = "lineWidth";

    public StringUtilsPreferencePage() {
        super(GRID);
    }

    public void createFieldEditors() {
        addField(new BooleanFieldEditor(CLAUSE_TO_UPPERCASE, CONVER_SQL_CLAUSES_TO_UPPER_CASE, getFieldEditorParent()));
        addField(new BooleanFieldEditor(FORMATTE_SQL_ON_EXTRACT, FORMATTE_SQL_QUERY_ON_EXTRACT, getFieldEditorParent()));
        addField(new StringFieldEditor(NEXT_LINE_OFFSET, NEXT_LINE_OFFSET_LABEL, 3, getFieldEditorParent()));
        addField(new StringFieldEditor(SQL_INITIAL_LINE_OFFSET, SQL_INITIAL_LINE_OFFSET_LABEL, 3, getFieldEditorParent()));
        addField(new StringFieldEditor(LINE_WIDTH, LINE_WIDTH_LABEL, 3, getFieldEditorParent()));
    }

    @Override
    public void init(IWorkbench workbench) {
        setPreferenceStore(Activator.getDefault().getPreferenceStore());
        setDescription(PLUGIN_TITLE);
        getPreferenceStore().setDefault(CLAUSE_TO_UPPERCASE, TRUE_STRING_VALUE);
        getPreferenceStore().setDefault(STRING_BORDER_OFFSET, TRUE_STRING_VALUE);
        getPreferenceStore().setDefault(FORMATTE_SQL_ON_EXTRACT, TRUE_STRING_VALUE);
        getPreferenceStore().setDefault(SQL_INITIAL_LINE_OFFSET, INITIAL_OFFSET_VALUE);
        getPreferenceStore().setDefault(NEXT_LINE_OFFSET, NEXT_LINE_OFFSET_DEFAULT_VALUE);
        getPreferenceStore().setDefault(LINE_WIDTH, LINE_WIDTH_DEFAULT_VALUE);
    }
}