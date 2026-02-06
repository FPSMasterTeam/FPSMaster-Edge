package top.fpsmaster.modules.music.netease;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import top.fpsmaster.utils.crypto.CryptUtils;
import top.fpsmaster.utils.io.HttpRequest;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Random;

public class NeteaseApi {
    private static final String BASE_URL = "https://music.skidder.top/";
    public static JsonParser parser = new JsonParser();

    public static String cookies = "";

    public static String encryptRequest(String text) {
        String secKey = CryptUtils.createSecretKey(16);
        // Key
        String nonce = "0CoJUm6Qyw8W8jud";
        String encText = CryptUtils.aesEncrypt(CryptUtils.aesEncrypt(text, nonce), secKey);
        String modulus = "00e0b509f6259df8642dbc35662901477df22677ec152b5ff68ace615bb7"
                + "b725152b3ab17a876aea8a5aa76d2e417629ec4ee341f56135fccf695280"
                + "104e0312ecbda92557c93870114af6c9d05c4f7f0c3685b7a46bee255932"
                + "575cce10b424d813cfe4875d3e82047b97ddef52741d546b8e289dc6935b" + "3ece0462db0a22b8e7";
        String pubKey = "010001";
        String encSecKey = CryptUtils.rsaEncrypt(secKey, pubKey, modulus);
        try {
            return "params=" + URLEncoder.encode(encText, "UTF-8") + "&encSecKey="
                    + URLEncoder.encode(encSecKey, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }


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

    public static String getUniKeyNew() {
        JsonObject obj = new JsonObject();
        obj.addProperty("type", 1);

        String data = encryptRequest(obj.toString());

        HttpRequest.HttpResponseResult post = null;
        try {
            post = HttpRequest.post("https://music.163.com/weapi/login/qrcode/unikey?"+data,null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (post.isSuccess()) {
            return ((JsonObject) parser.parse(post.getBody())).get("unikey").getAsString();
        }else{
            return null;
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



