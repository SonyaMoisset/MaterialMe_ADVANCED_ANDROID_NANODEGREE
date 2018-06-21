package com.example.xyzreader.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

public class SelectionBuilder {
    private String table = null;
    private HashMap<String, String> projectionMap;
    private StringBuilder selection;
    private ArrayList<String> selectionArgs;

    public SelectionBuilder where(String selection, String... selectionArgs) {
        if (TextUtils.isEmpty(selection)) {
            if (selectionArgs != null && selectionArgs.length > 0) {
                throw new IllegalArgumentException(
                        "Valid selection required when including arguments=");
            }

            return this;
        }

        ensureSelection(selection.length());
        if (this.selection.length() > 0) {
            this.selection.append(" AND ");
        }

        this.selection.append("(").append(selection).append(")");
        if (selectionArgs != null) {
        	ensureSelectionArgs();
            Collections.addAll(this.selectionArgs, selectionArgs);
        }

        return this;
    }

    public SelectionBuilder table(String table) {
        this.table = table;
        return this;
    }

    private void assertTable() {
        if (table == null) {
            throw new IllegalStateException("Table not specified");
        }
    }

    private void ensureSelection(int lengthHint) {
    	if (selection == null) {
    		selection = new StringBuilder(lengthHint + 8);
    	}
    }

    private void ensureSelectionArgs() {
    	if (selectionArgs == null) {
    		selectionArgs = new ArrayList<>();
    	}
    }

    private String getSelection() {
    	if (selection != null) {
            return selection.toString();
    	} else {
    		return null;
    	}
    }

    private String[] getSelectionArgs() {
    	if (selectionArgs != null) {
            return selectionArgs.toArray(new String[selectionArgs.size()]);
    	} else {
    		return null;
    	}
    }

    private void mapColumns(String[] columns) {
    	if (projectionMap == null) return;
        for (int i = 0; i < columns.length; i++) {
            final String target = projectionMap.get(columns[i]);
            if (target != null) {
                columns[i] = target;
            }
        }
    }

    @Override
    public String toString() {
        return "SelectionBuilder[table=" + table + ", selection=" + getSelection()
                + ", selectionArgs=" + Arrays.toString(getSelectionArgs()) + "]";
    }

    public Cursor query(SQLiteDatabase db, String[] columns, String orderBy) {
        return query(db, columns, orderBy, null);
    }

    private Cursor query(SQLiteDatabase db, String[] columns,
                         String orderBy, String limit) {
        assertTable();
        if (columns != null) mapColumns(columns);
        return db.query(table, columns, getSelection(), getSelectionArgs(), null, null,
                orderBy, limit);
    }

    public int update(SQLiteDatabase db, ContentValues values) {
        assertTable();
        return db.update(table, values, getSelection(), getSelectionArgs());
    }

    public int delete(SQLiteDatabase db) {
        assertTable();
        return db.delete(table, getSelection(), getSelectionArgs());
    }
}
