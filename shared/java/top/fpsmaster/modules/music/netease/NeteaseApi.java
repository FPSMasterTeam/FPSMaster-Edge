package top.fpsmaster.modules.music.netease;

import top.fpsmaster.utils.os.HttpRequest;

import java.io.IOException;

public class NeteaseApi {
    private static final String BASE_URL = "https://music.skidder.top/";

    public static String cookies = "";

    public static String getVerbatimLyrics(String id) {
        String url = BASE_URL + "lyric/new?id=" + id;
        return HttpRequest.getWithCookie(url, cookies);
    }

    public static String searchSongs(String keywords) {
        String url = BASE_URL + "cloudsearch?keywords=" + keywords;
        return HttpRequest.getWithCookie(url, cookies);
    }

    public static String checkMusic(String id) {
        String url = BASE_URL + "check/music?id=" + id;
        return HttpRequest.getWithCookie(url, cookies);
    }

    public static String getUserData() {
        String url = BASE_URL + "user/level";
        return HttpRequest.getWithCookie(url, cookies);
    }

    public static String getPlayURL(String id) {
        String url = BASE_URL + "song/url/v1?id=" + id + "&level=higher";
        return HttpRequest.getWithCookie(url, cookies);
    }

    public static String getPlayList(String id) {
        String url = BASE_URL + "playlist/track/all?id=" + id + "&limit=50";
        return HttpRequest.getWithCookie(url, cookies);
    }

    public static String getDailyList() {
        String url = BASE_URL + "recommend/songs?timestamp=" + System.currentTimeMillis();
        return HttpRequest.getWithCookie(url, cookies);
    }

    public static String getUniKey() {
        String url = BASE_URL + "login/qr/key?timestamp=" + System.currentTimeMillis();
        return HttpRequest.getWithCookie(url, "");
    }

    public static String generateQRCode(String key) {
        String url = BASE_URL + "login/qr/create?key=" + key + "&qrimg=true&timestamp=" + System.currentTimeMillis();
        return HttpRequest.getWithCookie(url, "");
    }

    public static String checkLoginStatus(String key) {
        String url = BASE_URL + "login/qr/check?key=" + key + "&noCookie=true&timestamp=" + System.currentTimeMillis();
        return HttpRequest.getWithCookie(url, "");
    }

    public static String getAnonymous() {
        String url = BASE_URL + "register/anonimous?timestamp=" + System.currentTimeMillis();
        return HttpRequest.getWithCookie(url, "");
    }
}
