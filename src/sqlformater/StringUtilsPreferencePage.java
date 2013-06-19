package sqlformater;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/***************************************************************************
 * {@link FieldEditorPreferencePage} for StringUtils plugin.
 * 
 * @author Serhii Krivtsov
 ***************************************************************************/
public class StringUtilsPreferencePage extends FieldEditorPreferencePage
        implements IWorkbenchPreferencePage {

    public static final String CLAUSE_TO_UPPERCASE = "clauseToUppercase";
    public static final String FORMATTE_SQL_ON_EXTRACT = "formatteSqlOnExtract";
    public static final String SQL_QUERY_OFFSET = "sqlQueryOffset";
    public static final String STRING_BORDER_OFFSET = "stringBorderOffset";

    public StringUtilsPreferencePage() {
        super(GRID);
    }

    public void createFieldEditors() {

        addField(new BooleanFieldEditor(CLAUSE_TO_UPPERCASE,
                "&Conver SQL clauses to upper case", getFieldEditorParent()));
        addField(new BooleanFieldEditor(FORMATTE_SQL_ON_EXTRACT,
                "&Formatte SQL query on extract", getFieldEditorParent()));

        BooleanFieldEditor fitToEditorMargin = new BooleanFieldEditor(
                STRING_BORDER_OFFSET, "&String border offset",
                getFieldEditorParent());
        final StringFieldEditor sqlOffset = new StringFieldEditor(
                SQL_QUERY_OFFSET, "SQL Offset", 10, getFieldEditorParent());
        fitToEditorMargin
                .setPropertyChangeListener(new IPropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent arg0) {
                        if ((Boolean) arg0.getNewValue()) {
                            sqlOffset.setEnabled(false, getFieldEditorParent());
                        }
                        getPreferenceStore().
                    }
                });
        addField(fitToEditorMargin);

        addField(sqlOffset);
    }

    @Override
    public void init(IWorkbench workbench) {
        setPreferenceStore(Activator.getDefault().getPreferenceStore());
        setDescription("String utility settings");
        getPreferenceStore().setDefault(STRING_BORDER_OFFSET, "true");
    }
}