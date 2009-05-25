/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2008-2009, Open Source Geospatial Foundation (OSGeo)
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
package org.geotoolkit.internal.setup;

import java.io.*;
import javax.swing.*;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.concurrent.ExecutionException;

import org.geotoolkit.resources.Vocabulary;
import org.geotoolkit.internal.io.Installation;


/**
 * The panel displaying available data and giving the opportunity to download them.
 *
 * @author Martin Desruisseaux (Geomatys)
 * @version 3.00
 *
 * @since 3.00
 * @module
 */
@SuppressWarnings("serial")
final class DataPanel extends JPanel {
    /**
     * Index of items to reports.
     */
    private static final int NADCON=0;

    /**
     * User for fetching localized words.
     */
    final Vocabulary resources;

    /**
     * The status about item to reports.
     */
    private final JProgressBar[] status = new JProgressBar[1];

    /**
     * The download buttons.
     */
    private final JButton[] downloads = new JButton[status.length];

    /**
     * Creates the panel.
     */
    DataPanel(final Vocabulary resources) {
        this.resources = resources;
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        final GridBagConstraints c = new GridBagConstraints();
        c.insets.left=3; c.insets.right=3;
        c.anchor = GridBagConstraints.WEST;
        c.fill   = GridBagConstraints.HORIZONTAL;
        for (int i=0; i<status.length; i++) {
            c.gridy = i;
            final String label;
            switch (i) {
                case NADCON: {
                    label = resources.getString(Vocabulary.Keys.DATA_$1, "NADCON");
                    break;
                }
                default: throw new AssertionError(i);
            }
            final int type = i;
            final JProgressBar state = status[i] = new JProgressBar();
            state.setStringPainted(true);
            final JButton download = downloads[i] =
                    new JButton(resources.getString(Vocabulary.Keys.DOWNLOAD));
            download.addActionListener(new ActionListener() {
                @Override public void actionPerformed(ActionEvent e) {
                    new Download(type).execute();
                }
            });
            c.gridx=0; c.weightx=c.weighty=0; add(new JLabel(label + ':'), c);
            c.insets.top=c.insets.bottom=2;
            c.gridx=1; c.weightx=c.weighty=1; add(state, c);
            c.insets.top=c.insets.bottom=0;
            c.gridx=2; c.weightx=c.weighty=0; add(download, c);
        }
        refresh();
    }

    /**
     * Refresh all data.
     */
    final void refresh() {
        for (int i=0; i<status.length; i++) {
            refresh(i);
        }
    }

    /**
     * Refresh the data. The type must be one of {@link #EPSG}, {@link #NADCON}
     * or similar constants.
     */
    private void refresh(final int item) {
        boolean found = false;
        switch (item) {
            case NADCON: {
                final File directory = Installation.NADCON.directory(true);
                if (new File(directory, "conus.las").isFile() &&
                    new File(directory, "conus.los").isFile())
                {
                    found = true;
                }
                break;
            }
        }
        final JProgressBar state = status[item];
        state.setString(resources.getString(found ?
                Vocabulary.Keys.DATA_ARE_PRESENT : Vocabulary.Keys.NODATA));
        state.setValue(found ? 100 : 0);
        downloads[item].setEnabled(!found);
    }

    /**
     * The action to be executed in a background thread when the user
     * pressed the "Download" button.
     */
    private final class Download extends SwingWorker<Object,Object> implements PropertyChangeListener {
        /**
         * The button that has been clicked.
         */
        private final int item;

        /**
         * Creates a new download action.
         */
        Download(final int item) {
            this.item = item;
            status[item].setString(resources.getMenuLabel(Vocabulary.Keys.DOWNLOADING));
            downloads[item].setEnabled(false);
            addPropertyChangeListener(this);
        }

        /**
         * Process to the download in a background thread.
         */
        @Override
        protected Object doInBackground() throws Exception {
            switch (item) {
                case NADCON: {
                    final File directory = Installation.NADCON.validDirectory(true);
                    unzip(new URL("http://www.ngs.noaa.gov/PC_PROD/NADCON/GRIDS.zip"), directory);
                    break;
                }
            }
            return null;
        }

        /**
         * Invoked when the downloading is finished.
         * displays an error message if the operation failed.
         */
        @Override
        protected void done() {
            try {
                get();
            } catch (InterruptedException e) {
                // Should not happen since we are done.
            } catch (ExecutionException e) {
                JOptionPane.showMessageDialog(DataPanel.this, e.getCause().toString(),
                        resources.getString(Vocabulary.Keys.ERROR), JOptionPane.ERROR_MESSAGE);
            }
            refresh();
        }

        /**
         * Unzip the given stream to the given target directory.
         * This convenience method does not log the progress.
         *
         * @param  in The input stream to unzip. The stream will be closed.
         * @param  target The destination directory.
         * @throws IOException If an error occured while unzipping the entries.
         */
        private void unzip(final URL url, final File target) throws IOException {
            final URLConnection connection = url.openConnection();
            final int progressDivisor = connection.getContentLength() / 100;
            final ZipInputStream in = new ZipInputStream(connection.getInputStream());
            int done = 0;
            try {
                final byte[] buffer = new byte[4096];
                ZipEntry entry;
                while ((entry = in.getNextEntry()) != null) {
                    final File file = new File(target, entry.getName());
                    final OutputStream out = new FileOutputStream(file);
                    int n;
                    while ((n = in.read(buffer)) >= 0) {
                        out.write(buffer, 0, n);
                        if (progressDivisor > 0) {
                            setProgress(Math.min(100, (done += n) / progressDivisor));
                        }
                    }
                    out.close();
                    final long time = entry.getTime();
                    if (time >= 0) {
                        file.setLastModified(time);
                    }
                    in.closeEntry();
                }
            } finally {
                in.close();
            }
        }

        /**
         * Reports progress.
         */
        @Override
        public void propertyChange(final PropertyChangeEvent event) {
            if ("progress".equals(event.getPropertyName())) {
                 status[item].setValue((Integer) event.getNewValue());
             }
        }
    }
}
