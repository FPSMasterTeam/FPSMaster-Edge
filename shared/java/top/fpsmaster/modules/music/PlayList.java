package top.fpsmaster.modules.music;

import top.fpsmaster.FPSMaster;
import top.fpsmaster.ui.notification.NotificationManager;

import java.util.LinkedList;

public class PlayList {
    private LinkedList<AbstractMusic> musics = new LinkedList<>();
    private int current = 0;
    private boolean shuffled = false;

    public void add(AbstractMusic music) {
        musics.add(music);
    }

    public void play() {
        NotificationManager.addNotification(
            FPSMaster.i18n.get("notification.music"),
            FPSMaster.i18n.get("notification.music.next").replace(
                "%s",
                (MusicPlayer.playList.current() != null ? MusicPlayer.playList.current().name : "")
            ),
            2f
        );
        musics.get(current).play();
        MusicPlayer.isPlaying = true;
    }

    public AbstractMusic current() {
        return (musics.isEmpty() || current < 0) ? null : musics.get(current);
    }

    public void pause() {
        MusicPlayer.pause();
    }

    public void seek(float percent) {
        musics.get(current).seek(percent);
    }

    private void shuffleList() {
        if (MusicPlayer.mode == 0 && !shuffled) {
            AbstractMusic current1 = MusicPlayer.playList.current();
            LinkedList<AbstractMusic> shuffledMusics = new LinkedList<>(musics);
            java.util.Collections.shuffle(shuffledMusics);
            MusicPlayer.playList.setMusics(shuffledMusics);
            MusicPlayer.playList.setCurrent(shuffledMusics.indexOf(current1));
            shuffled = true;
        }
    }

    public void next() {
        MusicPlayer.stop();
        if (musics.isEmpty()) return;
        shuffleList();
        if (MusicPlayer.mode != 2) {
            current++;
            if (current >= musics.size()) {
                current = 0;
            }
        }
        NotificationManager.addNotification(
            FPSMaster.i18n.get("notification.music"),
            FPSMaster.i18n.get("notification.music.next").replace(
                "%s",
                (MusicPlayer.playList.current() != null ? MusicPlayer.playList.current().name : "")
            ),
            2f
        );
        musics.get(current).play();
        MusicPlayer.isPlaying = true;
    }

    public void previous() {
        MusicPlayer.stop();
        if (musics.isEmpty()) return;
        shuffleList();
        if (MusicPlayer.mode != 2) {
            current--;
            if (current < 0) {
                current = musics.size() - 1;
            }
        }
        NotificationManager.addNotification(
            FPSMaster.i18n.get("notification.music"),
            FPSMaster.i18n.get("notification.music.next").replace(
                "%s",
                (MusicPlayer.playList.current() != null ? MusicPlayer.playList.current().name : "")
            ),
            2f
        );
        musics.get(current).play();
    }

    public void remove(AbstractMusic music) {
        musics.remove(music);
    }

    public void remove(int index) {
        musics.remove(index);
    }

    public void clear() {
        musics.clear();
    }

    public void setMusicList(LinkedList<AbstractMusic> musics) {
        AbstractMusic element = current();
        this.musics.clear();
        this.musics.addAll(musics);
        if (element != null) {
            current = musics.indexOf(element);
        }
        shuffled = false;
    }

    public LinkedList<AbstractMusic> getMusics() {
        return musics;
    }

    public void setMusics(LinkedList<AbstractMusic> musics) {
        this.musics = musics;
    }

    public int getCurrent() {
        return current;
    }

    public void setCurrent(int current) {
        this.current = current;
    }

    public boolean isShuffled() {
        return shuffled;
    }

    public void setShuffled(boolean shuffled) {
        this.shuffled = shuffled;
    }
}
