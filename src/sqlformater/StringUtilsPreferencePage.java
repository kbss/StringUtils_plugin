package sqlformater;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class StringUtilsPreferencePage extends FieldEditorPreferencePage
        implements IWorkbenchPreferencePage {

    public static final String CLAUSE_TO_UPPERCASE = "clauseToUppercase";

    public StringUtilsPreferencePage() {
        super(GRID);
    }

    public void createFieldEditors() {

        addField(new BooleanFieldEditor(CLAUSE_TO_UPPERCASE,
                "&Conver SQL clauses to upper case", getFieldEditorParent()));
    }

    @Override
    public void init(IWorkbench workbench) {
        setPreferenceStore(Activator.getDefault().getPreferenceStore());
        setDescription("String utility settings");
    }
}
