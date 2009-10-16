/*
 *    Geotoolkit.org - An Open Source Java GIS Toolkit
 *    http://www.geotoolkit.org
 *
 *    (C) 2009, Open Source Geospatial Foundation (OSGeo)
 *    (C) 2009, Geomatys
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
package org.geotoolkit.gui.swing.image;

import java.awt.Component;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.ListSelectionModel;
import javax.imageio.metadata.IIOMetadataFormat;

import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.table.TableColumnExt;
import org.jdesktop.swingx.renderer.StringValue;
import org.jdesktop.swingx.renderer.DefaultTableRenderer;
import org.jdesktop.swingx.decorator.HighlighterFactory;

import org.geotoolkit.resources.Vocabulary;
import org.geotoolkit.gui.swing.tree.TreeTableNode;
import org.geotoolkit.image.io.metadata.MetadataTreeTable;
import org.geotoolkit.internal.swing.table.BooleanRenderer;

import static org.geotoolkit.image.io.metadata.MetadataTreeTable.COLUMN_COUNT;
import static org.geotoolkit.image.io.metadata.MetadataTreeTable.VALUE_COLUMN;


/**
 * The {@code TreeTable} implementation for {@link IIOMetadataPanel}.
 *
 * @author Martin Desruisseaux (Geomatys)
 * @version 3.05
 *
 * @see MetadataTreeTable
 *
 * @since 3.05
 * @module
 */
@SuppressWarnings("serial")
final class IIOMetadataTreeTable extends JXTreeTable implements StringValue {
    /**
     * The identifier for this table. Each table in a {@link IIOMetadataPanel}
     * shall have an unique identifier.
     */
    final String identifier;

    /**
     * The renderer for boolean values.
     */
    private final TableCellRenderer booleanRenderer;

    /**
     * Creates a new table for the given table. The given root <strong>must</strong> be
     * the value returned by {@link MetadataTreeTable#getRootNode()}, or something having
     * the same structure.
     *
     * @param identifier An identifier, which must be unique for a given {@link IIOMetadataPanel}.
     * @param root The output of {@link MetadataTreeTable#getRootNode()}.
     */
    IIOMetadataTreeTable(final String identifier, final TreeTableNode root) {
        super(new Model(root));
        final Model model = (Model) getTreeTableModel();
        model.owner = this;
        this.identifier = identifier;
        setRootVisible(false);
        setColumnControlVisible(true);
        setHighlighters(HighlighterFactory.createSimpleStriping());
        setDefaultRenderer(Class.class, new DefaultTableRenderer(this));
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        booleanRenderer = new BooleanRenderer();
        /*
         * Hide the "default value" column. This column is empty most of the time.
         * The only non-null values are usually "false" for the boolean type. The
         * user can still show this column if he ask explicitly.
         */
        final TableColumnModel columns = getColumnModel();
        final int c = columns.getColumnCount();
        ((TableColumnExt) columns.getColumn(c-1)).setVisible(false); // "Default value" column.
        ((TableColumnExt) columns.getColumn(c-4)).setVisible(false); // "Value type" column.
    }

    /**
     * The table model for the {@link IIOMetadata} or {@link IIOMetadataFormat}.
     * The columns are documented in the {@link MetadataTreeTable} javadoc.
     *
     * @author Martin Desruisseaux (Geomatys)
     * @version 3.05
     *
     * @since 3.05
     * @module
     */
    private static final class Model extends org.geotoolkit.gui.swing.TreeTableModelAdapter {
        /**
         * The component which own this model.
         * This is used only for fetching the locale.
         */
        Component owner;

        /**
         * Creates a model for the given root. The given root <strong>must</strong> be
         * the value returned by {@link MetadataTreeTable#getRootNode()}, or something
         * having the same structure.
         */
        Model(final TreeTableNode root) {
            super(root);
        }

        /**
         * Returns the name of the given column. The columns shall
         * matches the ones documented in {@link MetadataTreeTable}.
         */
        @Override
        public String getColumnName(int column) {
            final int key;
            if (column >= VALUE_COLUMN) {
                column += COLUMN_COUNT - getColumnCount();
                // Skip the "values" column if it doesn't exist.
            }
            switch (column) {
                case 0:  key = Vocabulary.Keys.METADATA;     break;
                case 1:  key = Vocabulary.Keys.DESCRIPTION;  break;
                case 2:  key = Vocabulary.Keys.VALUE;        break;
                case 3:  key = Vocabulary.Keys.TYPE;         break;
                case 4:  key = Vocabulary.Keys.OCCURENCE;    break;
                case 5:  key = Vocabulary.Keys.VALID_VALUES; break;
                case 6:  key = Vocabulary.Keys.DEFAULT;      break;
                case COLUMN_COUNT:
                // The later is added only for making sure at compile-time that
                // we are not declaring more columns than the expected number.
                default: return super.getColumnName(column);
            }
            final Component owner = this.owner;
            return Vocabulary.getResources(owner != null ? owner.getLocale() : null).getString(key);
        }
    }

    /**
     * Returns the string representation of a few types to be handled especially. This
     * is used only when the whole column has the same type, otherwise we need to use
     * {@link #getCellRenderer(int, int)}.
     */
    @Override
    public String getString(final Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Class<?>) {
            return ((Class<?>) value).getSimpleName();
        }
        return value.toString();
    }

    /**
     * Returns the renderer for the given cell. This method returns a special
     * renderer for the {@link Boolean} type.
     */
    @Override
    public TableCellRenderer getCellRenderer(final int row, final int column) {
        final Object value = getValueAt(row, column);
        if (value != null) {
            final Class<?> type = value.getClass();
            if (Boolean.class.equals(type)) {
                return booleanRenderer;
            }
        }
        return super.getCellRenderer(row, column);
    }
}
