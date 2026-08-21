package top.fpsmaster.replay.director;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayDeque;

/**
 * Snapshot undo/redo for an {@link EditProject}. Checkpoints are taken <em>before</em> a mutation.
 */
public final class EditHistory {
    private static final int LIMIT = 40;
    private static final Gson GSON = new GsonBuilder().create();

    private final ArrayDeque<String> undo = new ArrayDeque<String>();
    private final ArrayDeque<String> redo = new ArrayDeque<String>();

    public void clear() {
        undo.clear();
        redo.clear();
    }

    public boolean canUndo() {
        return !undo.isEmpty();
    }

    public boolean canRedo() {
        return !redo.isEmpty();
    }

    public void checkpoint(EditProject project) {
        if (project == null) {
            return;
        }
        String json = GSON.toJson(project);
        if (!undo.isEmpty() && json.equals(undo.peek())) {
            return;
        }
        undo.push(json);
        redo.clear();
        while (undo.size() > LIMIT) {
            undo.removeLast();
        }
    }

    public EditProject undo(EditProject current) {
        if (undo.isEmpty() || current == null) {
            return null;
        }
        redo.push(GSON.toJson(current));
        return GSON.fromJson(undo.pop(), EditProject.class);
    }

    public EditProject redo(EditProject current) {
        if (redo.isEmpty() || current == null) {
            return null;
        }
        undo.push(GSON.toJson(current));
        return GSON.fromJson(redo.pop(), EditProject.class);
    }
}
