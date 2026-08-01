package top.fpsmaster.features.settings.impl;

import top.fpsmaster.features.settings.Setting;

import java.util.ArrayList;
import java.util.List;

/**
 * A setting that stores an ordered list of {@link AutoTextEntry}s exposed to the ClickGUI.
 *
 * <p>Each entry bundles a key code and a chat message. The list is capped at 20, and duplicate
 * key codes (other than 0) are rejected at the editor level. This class enforces the cap and
 * provides shallow-copy snapshots for the reset-to-default lifecycle.
 */
public class AutoTextSetting extends Setting<ArrayList<AutoTextEntry>> {
    public static final int MAX_CAPACITY = 20;

    public AutoTextSetting(String name, ArrayList<AutoTextEntry> defaultValue) {
        super(name, clamp(defaultValue));
    }

    public AutoTextSetting(String name, ArrayList<AutoTextEntry> defaultValue, VisibleCondition visible) {
        super(name, clamp(defaultValue), visible);
    }

    private static ArrayList<AutoTextEntry> clamp(ArrayList<AutoTextEntry> list) {
        if (list == null) {
            list = new ArrayList<>();
        }
        if (list.size() > MAX_CAPACITY) {
            list = new ArrayList<>(list.subList(0, MAX_CAPACITY));
        }
        return list;
    }

    /** Returns true if the entry was added at the end. */
    public boolean addEntry(AutoTextEntry entry) {
        ArrayList<AutoTextEntry> current = getValue();
        if (current.size() >= MAX_CAPACITY) {
            return false;
        }
        ArrayList<AutoTextEntry> oldSnapshot = new ArrayList<>(current);
        ArrayList<AutoTextEntry> newSnapshot = new ArrayList<>(current);
        newSnapshot.add(new AutoTextEntry(entry));
        if (!fireValueChangeEvent(oldSnapshot, newSnapshot)) {
            return false;
        }
        current.add(new AutoTextEntry(entry));
        notifyChangeListeners(oldSnapshot, newSnapshot);
        return true;
    }

    /** Removes the entry at {@code index}, no-op if out of bounds. */
    public void removeEntry(int index) {
        ArrayList<AutoTextEntry> current = getValue();
        if (index < 0 || index >= current.size()) {
            return;
        }
        ArrayList<AutoTextEntry> oldSnapshot = new ArrayList<>(current);
        ArrayList<AutoTextEntry> newSnapshot = new ArrayList<>(current);
        newSnapshot.remove(index);
        if (!fireValueChangeEvent(oldSnapshot, newSnapshot)) {
            return;
        }
        current.remove(index);
        notifyChangeListeners(oldSnapshot, newSnapshot);
    }

    /** Replaces the entry at {@code index}. Returns false if the edit was rejected. */
    public boolean editEntry(int index, AutoTextEntry entry) {
        ArrayList<AutoTextEntry> current = getValue();
        if (index < 0 || index >= current.size()) {
            return false;
        }
        ArrayList<AutoTextEntry> oldSnapshot = new ArrayList<>(current);
        ArrayList<AutoTextEntry> newSnapshot = new ArrayList<>(current);
        newSnapshot.set(index, new AutoTextEntry(entry));
        if (!fireValueChangeEvent(oldSnapshot, newSnapshot)) {
            return false;
        }
        current.set(index, new AutoTextEntry(entry));
        notifyChangeListeners(oldSnapshot, newSnapshot);
        return true;
    }
}