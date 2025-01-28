package top.fpsmaster.modules.lua;

public class RawLua {
    public String code;
    public String filename;

    public RawLua(String code, String filename) {
        this.filename = filename;
        this.code = code;
    }

    public boolean compare(RawLua other) {
        return this.code.equals(other.code);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }

        if (this.filename.equals(((RawLua) obj).filename)) {
            return false;
        }
        return compare((RawLua) obj);
    }

    @Override
    public int hashCode() {
        return (this.filename + this.code).hashCode();
    }
}
