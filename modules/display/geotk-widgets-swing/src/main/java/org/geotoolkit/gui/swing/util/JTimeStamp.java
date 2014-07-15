/*
 *    Geotoolkit - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2013, Geomatys
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
package org.geotoolkit.gui.swing.util;

import org.apache.sis.util.logging.Logging;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles a full date, with time stamp. By default, do not display seconds.
 *
 * @author Guilhem Legal (Geomatys)
 * @author Cédric Briançon (Geomatys)
 * @author Quentin Boileau (Geomatys)
 */
public class JTimeStamp extends javax.swing.JComponent {

    private static final Logger LOGGER = Logging.getLogger(JTimeStamp.class);
    private boolean displaySeconds = false;
    private TimeZone timeZone;

    /**
     * Creates new form JTimeStamp
     */
    public JTimeStamp() {
        initComponents();

        if (!displaySeconds) {
            seconds.setVisible(false);
            guiSecondColon.setVisible(false);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        datePicker = new org.jdesktop.swingx.JXDatePicker();
        hours = new javax.swing.JSpinner();
        minutes = new javax.swing.JSpinner();
        seconds = new javax.swing.JSpinner();
        guiFirstColon = new javax.swing.JLabel();
        guiSecondColon = new javax.swing.JLabel();

        hours.setModel(new javax.swing.SpinnerNumberModel(0, 0, 23, 1));

        minutes.setModel(new javax.swing.SpinnerNumberModel(0, 0, 59, 1));

        seconds.setModel(new javax.swing.SpinnerNumberModel(0, 0, 59, 1));

        org.openide.awt.Mnemonics.setLocalizedText(guiFirstColon, ":");

        org.openide.awt.Mnemonics.setLocalizedText(guiSecondColon, ":");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(1, 1, 1)
                .addComponent(datePicker, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(hours, javax.swing.GroupLayout.PREFERRED_SIZE, 49, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(3, 3, 3)
                .addComponent(guiFirstColon)
                .addGap(3, 3, 3)
                .addComponent(minutes, javax.swing.GroupLayout.PREFERRED_SIZE, 49, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(3, 3, 3)
                .addComponent(guiSecondColon)
                .addGap(3, 3, 3)
                .addComponent(seconds, javax.swing.GroupLayout.PREFERRED_SIZE, 49, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(1, 1, 1)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(datePicker, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(hours, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(minutes, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(guiFirstColon)
                    .addComponent(guiSecondColon)
                    .addComponent(seconds, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private org.jdesktop.swingx.JXDatePicker datePicker;
    private javax.swing.JLabel guiFirstColon;
    private javax.swing.JLabel guiSecondColon;
    private javax.swing.JSpinner hours;
    private javax.swing.JSpinner minutes;
    private javax.swing.JSpinner seconds;
    // End of variables declaration//GEN-END:variables

    public void setValue(final Date t) {
        if (t != null) {
            datePicker.setDate(t);

            Calendar calendar;
            if (timeZone != null) {
                calendar = new GregorianCalendar(timeZone);
            } else {
                calendar = GregorianCalendar.getInstance();
            }
            calendar.setTime(t);
            final int hour = calendar.get(Calendar.HOUR_OF_DAY);
            final int minute = calendar.get(Calendar.MINUTE);
            hours.setValue(hour);
            minutes.setValue(minute);
            if (displaySeconds) {
                final int second = calendar.get(Calendar.SECOND);
                seconds.setValue(second);
            }
        } else {
            datePicker.setDate(null);
            hours.setValue(0);
            minutes.setValue(0);
            seconds.setValue(0);
        }
    }

    public Date getValue() {
        Calendar calendar;
        if (timeZone != null) {
            calendar = new GregorianCalendar(timeZone);
        } else {
            calendar = GregorianCalendar.getInstance();
        }

        final Date d = datePicker.getDate();
        if (d != null) {
            //update models
            try {
                hours.commitEdit();
                minutes.commitEdit();
                if (displaySeconds) {
                    seconds.commitEdit();
                }

                calendar.setTime(datePicker.getDate());
                calendar.set(Calendar.HOUR_OF_DAY, (Integer) hours.getValue());
                calendar.set(Calendar.MINUTE, (Integer) minutes.getValue());
                if (displaySeconds) {
                    calendar.set(Calendar.SECOND, (Integer) seconds.getValue());
                }

                return new Date(calendar.getTimeInMillis());
            } catch (ParseException pe) {
                LOGGER.log(Level.INFO, "Error during spinners parsing : "+pe.getMessage());
                return d;
            }
        }
        return null;
    }

    /**
     * Defines if the seconds should be displayed or not.
     *
     * @param displaySeconds {@code True} to display seconds. {@code False} otherwise, and by default.
     */
    public void setDisplaySeconds(final boolean displaySeconds) {
        this.displaySeconds = displaySeconds;

        if (displaySeconds) {
            seconds.setVisible(true);
            guiSecondColon.setVisible(true);
        }
    }

    public void setTimeZone(final TimeZone timeZone) {
        this.timeZone = timeZone;
        datePicker.setTimeZone(timeZone);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        this.datePicker.setEnabled(enabled);
        this.hours.setEnabled(enabled);
        this.minutes.setEnabled(enabled);
        this.seconds.setEnabled(enabled);
    }
}
