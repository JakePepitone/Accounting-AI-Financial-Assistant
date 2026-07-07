package com.accountingai.controller;

import com.accountingai.AppServices;
import com.accountingai.model.Settings;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;

/**
 * Controller for the settings dialog ({@code settings-dialog.fxml}).
 *
 * <p>Lets the user pick a default export folder, default export format, UI theme
 * and the number of rows per page. Values are loaded from and saved back through
 * the shared {@link com.accountingai.service.SettingsService}.</p>
 */
public class SettingsController {

    @FXML
    private TextField exportFolderField;

    @FXML
    private ComboBox<String> formatCombo;

    @FXML
    private ComboBox<String> themeCombo;

    @FXML
    private Spinner<Integer> pageSizeSpinner;

    /**
     * Populates the combo boxes and spinner, then loads the current settings.
     */
    @FXML
    public void initialize() {
        // Available export formats (must match Settings.defaultExportFormat values).
        formatCombo.getItems().setAll("CSV", "XLSX", "PDF");
        themeCombo.getItems().setAll("Light", "Dark");

        // Rows per page: 5..200, matching the SettingsService clamp range.
        pageSizeSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(5, 200, 50));

        loadCurrent();
    }

    /**
     * Loads the persisted settings into the form controls, falling back to sane
     * defaults if a stored value is missing or invalid.
     */
    private void loadCurrent() {
        Settings s = AppServices.get().settingsService().getCurrent();

        exportFolderField.setText(s.getDefaultExportFolder() == null ? "" : s.getDefaultExportFolder());

        String format = s.getDefaultExportFormat();
        formatCombo.setValue(formatCombo.getItems().contains(format) ? format : "CSV");

        String theme = s.getTheme();
        themeCombo.setValue(themeCombo.getItems().contains(theme) ? theme : "Light");

        int pageSize = s.getPageSize();
        if (pageSize < 5) {
            pageSize = 5;
        } else if (pageSize > 200) {
            pageSize = 200;
        }
        pageSizeSpinner.getValueFactory().setValue(pageSize);
    }

    /**
     * Handles "Browse...": opens a directory chooser and, if a folder is picked,
     * writes its absolute path into the export folder field.
     */
    @FXML
    void handleBrowse() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choose default export folder");

        // Start from the current value if it points at a real directory.
        String current = exportFolderField.getText();
        if (current != null && !current.isBlank()) {
            File dir = new File(current);
            if (dir.isDirectory()) {
                chooser.setInitialDirectory(dir);
            }
        }

        File selected = chooser.showDialog(stageOf(exportFolderField));
        if (selected != null) {
            exportFolderField.setText(selected.getAbsolutePath());
        }
    }

    /**
     * Handles "Save": builds a {@link Settings} from the form, persists it and
     * closes the dialog.
     */
    @FXML
    void handleSave() {
        Settings s = new Settings();
        s.setDefaultExportFolder(exportFolderField.getText() == null ? "" : exportFolderField.getText().trim());

        String format = formatCombo.getValue();
        s.setDefaultExportFormat(format == null ? "CSV" : format);

        String theme = themeCombo.getValue();
        s.setTheme(theme == null ? "Light" : theme);

        Integer pageSize = pageSizeSpinner.getValue();
        s.setPageSize(pageSize == null ? 50 : pageSize);

        AppServices.get().settingsService().updateAndSave(s);
        close();
    }

    /**
     * Handles "Cancel": closes the dialog without saving.
     */
    @FXML
    void handleCancel() {
        close();
    }

    /**
     * Closes the settings window.
     */
    private void close() {
        Stage stage = stageOf(exportFolderField);
        if (stage != null) {
            stage.close();
        }
    }

    /**
     * Returns the {@link Stage} that owns the given control, or {@code null} if
     * the control is not yet attached to a scene/window.
     *
     * @param node any control in this dialog
     * @return the owning stage, or null
     */
    private Stage stageOf(Node node) {
        if (node == null || node.getScene() == null) {
            return null;
        }
        return (Stage) node.getScene().getWindow();
    }
}
