package top.fpsmaster.modules.music;

public abstract class AbstractMusic {
    // stuff to display
    public String name = "";
    public String author = "";
    public Lyrics lyrics = null;
    public Lyrics tlyrics = null;
    public Lyrics rlyrics = null;

    public abstract void play();

    public void seek(float percent) {
        if (JLayerHelper.clip != null) {
            JLayerHelper.seek(percent);
        }
    }
}



