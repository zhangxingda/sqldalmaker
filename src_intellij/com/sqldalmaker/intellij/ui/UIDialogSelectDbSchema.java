/*
 * Copyright 2011-2018 sqldalmaker@gmail.com
 * SQL DAL Maker Website: http://sqldalmaker.sourceforge.net
 * Read LICENSE.txt in the root of this project/archive for details.
 */
package com.sqldalmaker.intellij.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.sqldalmaker.cg.DbUtils;
import com.sqldalmaker.common.ISelectDbSchemaCallback;
import com.sqldalmaker.common.InternalException;
import com.sqldalmaker.jaxb.settings.Settings;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.Connection;
import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: sqldalmaker@gmail.com
 * Date: 21.06.12
 */
public class UIDialogSelectDbSchema extends JDialog {

    private Project project;

    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTable table;
    private JCheckBox chk_Skip;
    private JCheckBox chk_DeleteS;
    private JRadioButton crudRadioButton;
    private JRadioButton crudAutoRadioButton;
    private JPanel radioPanel;
    private JCheckBox chk_add_fk_access;
    private JCheckBox chk_including_views;

    private final Settings settings;

    private UIDialogSelectDbSchema(Project project, VirtualFile profile, ISelectDbSchemaCallback callback, boolean dto, boolean fk) throws Exception {
        $$$setupUI$$$();

        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        this.project = project;
        this.callback = callback;
        this.settings = IdeaHelpers.load_settings(profile);

        setTitle(settings.getJdbc().getUrl());

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

// call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

// call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        if (dto || fk) {
            this.radioPanel.setVisible(false);
            chk_add_fk_access.setVisible(false);
        }

        if (fk) {
            this.chk_Skip.setVisible(false);
            this.chk_including_views.setVisible(false);
        }

        refresh();
    }

    private int[] getSelection() throws InternalException {

        int rc = table.getModel().getRowCount();

        if (rc == 1) {
            return new int[]{0};
        }

        int[] selectedRows = table.getSelectedRows();

        if (selectedRows.length == 0) {

            throw new InternalException("Selection is empty.");
        }

        return selectedRows;
    }

    private void onOK() {

        try {

            String selected_schema = null;

            if (table.getRowCount() == 1) {

                selected_schema = (String) table.getValueAt(0, 0);

            } else {

                if (table.getRowCount() != 0) {

                    int[] indexes = getSelection();

                    if (indexes.length == 1) {

                        selected_schema = (String) table.getValueAt(indexes[0], 0);

                    } else {

                        throw new InternalException("Selection is empty.");
                    }
                }
            }

            callback.process_ok(selected_schema, chk_Skip.isSelected(), chk_including_views.isSelected(), chk_DeleteS.isSelected(), crudAutoRadioButton.isSelected(), chk_add_fk_access.isSelected());

            dispose();

        } catch (InternalException e) {
            IdeaMessageHelpers.show_error_in_ui_thread(e);
        }
    }

    private void onCancel() {
// add your code here if necessary
        dispose();
    }

    private ISelectDbSchemaCallback callback;

    static void open(Project project,
                     VirtualFile profile, ISelectDbSchemaCallback callback, boolean dto, boolean fk) throws Exception {

        UIDialogSelectDbSchema dlg = new UIDialogSelectDbSchema(project, profile, callback, dto, fk);
        // dlg.setPreferredSize(new Dimension(520, 280));
        dlg.pack(); // after setPreferredSize
        dlg.setLocationRelativeTo(null);  // after pack!!!
        dlg.setVisible(true);
    }

    private void refresh() {

        try {
            final ArrayList<String> items = new ArrayList<String>();

            Connection con = IdeaHelpers.get_connection(project, settings);

            DbUtils.get_schema_names(con, items);

            // buttonOK.setEnabled(items.size() <= 1);

            table.setModel(new AbstractTableModel() {

                public Object getValueAt(int rowIndex, int columnIndex) {
                    return items.get(rowIndex);
                }

                public int getColumnCount() {
                    return 1;
                }

                public String getColumnName(int column) {
                    return "DB-Schema";
                }

                public int getRowCount() {
                    return items.size();
                }
            });

            table.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        onOK();
                    }
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            IdeaMessageHelpers.show_error_in_ui_thread(e);
        }
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        contentPane = new JPanel();
        contentPane.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 2, new Insets(10, 10, 10, 10), -1, -1));
        contentPane.add(panel1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1, true, false));
        panel1.add(panel2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        buttonOK = new JButton();
        buttonOK.setText("OK");
        panel2.add(buttonOK, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        buttonCancel = new JButton();
        buttonCancel.setText("Cancel");
        panel2.add(buttonCancel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(4, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(2, 1, new Insets(10, 10, 10, 10), -1, -1));
        panel3.add(panel4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("(just click OK if the list is empty)");
        panel4.add(label1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        Font label2Font = this.$$$getFont$$$(null, Font.BOLD, 14, label2.getFont());
        if (label2Font != null) label2.setFont(label2Font);
        label2.setText("Select DB-Schema");
        panel4.add(label2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(6, 1, new Insets(10, 10, 10, 10), -1, -1));
        panel3.add(panel5, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        panel5.add(scrollPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        table = new JTable();
        scrollPane1.setViewportView(table);
        chk_Skip = new JCheckBox();
        chk_Skip.setSelected(true);
        chk_Skip.setText("Skip tables/views used in existing declarations");
        panel5.add(chk_Skip, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        chk_DeleteS = new JCheckBox();
        chk_DeleteS.setSelected(true);
        chk_DeleteS.setText("English plural to singular for DTO class names");
        panel5.add(chk_DeleteS, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        radioPanel = new JPanel();
        radioPanel.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel5.add(radioPanel, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        crudRadioButton = new JRadioButton();
        crudRadioButton.setSelected(true);
        crudRadioButton.setText("crud");
        radioPanel.add(crudRadioButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        radioPanel.add(spacer2, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        crudAutoRadioButton = new JRadioButton();
        crudAutoRadioButton.setText("crud-auto");
        radioPanel.add(crudAutoRadioButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        chk_add_fk_access = new JCheckBox();
        chk_add_fk_access.setSelected(true);
        chk_add_fk_access.setText("Including FK access code");
        panel5.add(chk_add_fk_access, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        chk_including_views = new JCheckBox();
        chk_including_views.setSelected(true);
        chk_including_views.setText("Including Views");
        panel5.add(chk_including_views, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JSeparator separator1 = new JSeparator();
        panel3.add(separator1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        ButtonGroup buttonGroup;
        buttonGroup = new ButtonGroup();
        buttonGroup.add(crudAutoRadioButton);
        buttonGroup.add(crudRadioButton);
    }

    /**
     */
    private Font $$$getFont$$$(String fontName, int style, int size, Font currentFont) {
        if (currentFont == null) return null;
        String resultName;
        if (fontName == null) {
            resultName = currentFont.getName();
        } else {
            Font testFont = new Font(fontName, Font.PLAIN, 10);
            if (testFont.canDisplay('a') && testFont.canDisplay('1')) {
                resultName = fontName;
            } else {
                resultName = currentFont.getName();
            }
        }
        return new Font(resultName, style >= 0 ? style : currentFont.getStyle(), size >= 0 ? size : currentFont.getSize());
    }

    /**
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }
}