package top.fpsmaster.modules.music.netease;

public class NeteaseProfile {
    public long id;
    public String nickname;
    public String avatarUrl;

    public NeteaseProfile(long id, String nickname, String avatarUrl) {
        this.id = id;
        this.nickname = nickname;
        this.avatarUrl = avatarUrl;
    }
}
