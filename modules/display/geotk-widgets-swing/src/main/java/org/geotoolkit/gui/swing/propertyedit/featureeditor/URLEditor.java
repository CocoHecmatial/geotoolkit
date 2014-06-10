/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2012, Johann Sorel
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotoolkit.gui.swing.propertyedit.featureeditor;

import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;

import static org.geotoolkit.gui.swing.propertyedit.featureeditor.FileEditor.getPreviousPath;
import static org.geotoolkit.gui.swing.propertyedit.featureeditor.FileEditor.setPreviousPath;
import org.geotoolkit.gui.swing.resource.MessageBundle;
import org.geotoolkit.feature.type.PropertyType;

/**
 * Throw PropertyChange event when TextField text change.
 *
 * @author Johann Sorel (Puzzle-GIS)
 */
public class URLEditor extends PropertyValueEditor implements ActionListener, DocumentListener {

    private final JTextField component = new JTextField();
    private final JButton chooseButton = new JButton("...");
    private final FileFilter filter;

    public URLEditor() {
        this(null);
    }

    public URLEditor(FileFilter filter) {
        super(new BorderLayout());
        add(BorderLayout.CENTER, component);
        add(BorderLayout.EAST, chooseButton);
        component.getDocument().addDocumentListener(this);
        chooseButton.addActionListener(this);
        chooseButton.addFocusListener(this);
        chooseButton.setMargin(new Insets(0, 0, 0, 0));
        this.filter = filter;
    }

    @Override
    public boolean canHandle(PropertyType candidate) {
        return URL.class.equals(candidate.getBinding());
    }

    @Override
    public void setValue(PropertyType type, Object value) {
        if (value instanceof URL) {
            try {
                component.setText( ((URL) value).toURI().toString() );
            } catch (URISyntaxException ex) {
                Logger.getLogger(URLEditor.class.getName()).log(Level.WARNING, null, ex);
            }
        }else{
            component.setText("");
        }
    }

    @Override
    public Object getValue() {
        final String str = component.getText();
            try {
                return new URL(str);
            } catch (MalformedURLException ex) {
                Logger.getLogger(URLEditor.class.getName()).log(Level.FINER, null, ex);
                return null;
            }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        chooser.setMultiSelectionEnabled(false);
        if (filter != null) {
            chooser.setFileFilter(filter);
        }
        final String prevPath = getPreviousPath();
        if (prevPath != null) {
            chooser.setCurrentDirectory(new File(prevPath));
        }

        final int response = chooser.showDialog(chooseButton, MessageBundle.getString("ok"));
        if(response == JFileChooser.APPROVE_OPTION){
            final File f = chooser.getSelectedFile();
            if(f!=null){
                setPreviousPath(f.getAbsolutePath());
                component.setText(f.toURI().toString());
            }
        }
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        valueChanged();
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        valueChanged();
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        valueChanged();
    }

    @Override
    public void setEnabled(boolean enabled) {
        component.setEnabled(enabled);
        chooseButton.setEnabled(enabled);
    }

    @Override
    public boolean isEnabled() {
        return component.isEnabled() && chooseButton.isEnabled();
    }

}
