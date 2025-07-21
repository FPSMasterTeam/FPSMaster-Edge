package top.fpsmaster.modules.music.netease;

import com.google.gson.JsonElement;
import top.fpsmaster.utils.os.HttpRequest;

import java.io.IOException;

public class NeteaseApi {
    private static final String BASE_URL = "https://music.skidder.top/";

    public static String cookies = "";

    public static String getVerbatimLyrics(String id) {
        String url = BASE_URL + "lyric/new?id=" + id;
        try {
            return HttpRequest.getWithCookie(url, cookies).getBody();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String searchSongs(String keywords) {
        String url = BASE_URL + "cloudsearch?keywords=" + keywords;
        try {
            return HttpRequest.getWithCookie(url, cookies).getBody();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String checkMusic(String id) {
        String url = BASE_URL + "check/music?id=" + id;
        try {
            return HttpRequest.getWithCookie(url, cookies).getBody();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getUserData() {
        String url = BASE_URL + "user/level";
        try {
            return HttpRequest.getWithCookie(url, cookies).getBody();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getPlayURL(String id) {
        String url = BASE_URL + "song/url/v1?id=" + id + "&level=higher";
        try {
            return HttpRequest.getWithCookie(url, cookies).getBody();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getPlayList(String id) {
        String url = BASE_URL + "playlist/track/all?id=" + id + "&limit=50";
        try {
            return HttpRequest.getWithCookie(url, cookies).getBody();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getDailyList() {
        String url = BASE_URL + "recommend/songs?timestamp=" + System.currentTimeMillis();
        try {
            return HttpRequest.getWithCookie(url, cookies).getBody();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getUniKey() {
        String url = BASE_URL + "login/qr/key?timestamp=" + System.currentTimeMillis();
        try {
            return HttpRequest.getWithCookie(url, "").getBody();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String generateQRCode(String key) {
        String url = BASE_URL + "login/qr/create?key=" + key + "&qrimg=true&timestamp=" + System.currentTimeMillis();
        try {
            return HttpRequest.getWithCookie(url, "").getBody();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String checkLoginStatus(String key) {
        String url = BASE_URL + "login/qr/check?key=" + key + "&noCookie=true&timestamp=" + System.currentTimeMillis();
        try {
            return HttpRequest.getWithCookie(url, "").getBody();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getAnonymous() {
        String url = BASE_URL + "register/anonimous?timestamp=" + System.currentTimeMillis();
        try {
            return HttpRequest.getWithCookie(url, "").getBody();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getTracksDaily() {
        String url = BASE_URL + "recommend/resource?timestamp=" + System.currentTimeMillis();
        try {
            return HttpRequest.getWithCookie(url, cookies).getBody();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getProfile() {
        String url = BASE_URL + "login/status?timestamp=" + System.currentTimeMillis();
        try {
            return HttpRequest.getWithCookie(url, cookies).getBody();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getTracksLiked(long uid) {
        String url = BASE_URL + "user/playlist?uid="+uid+"&timestamp=" + System.currentTimeMillis();
        try {
            return HttpRequest.getWithCookie(url, cookies).getBody();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
