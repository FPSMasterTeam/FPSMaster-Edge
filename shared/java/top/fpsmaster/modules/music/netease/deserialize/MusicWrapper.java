package top.fpsmaster.modules.music.netease.deserialize;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import top.fpsmaster.modules.logger.ClientLogger;
import top.fpsmaster.modules.music.Line;
import top.fpsmaster.modules.music.Lyrics;
import top.fpsmaster.modules.music.PlayList;
import top.fpsmaster.modules.music.Word;
import top.fpsmaster.modules.music.netease.Music;
import top.fpsmaster.modules.music.netease.NeteaseApi;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Iterator;

public class MusicWrapper {
    private static Gson gson = new GsonBuilder().create();

    public static String getSongUrl(String id) {
        JsonObject jsonObject = gson.fromJson(NeteaseApi.getPlayURL(id), JsonObject.class);
        return jsonObject.getAsJsonArray("data").get(0).getAsJsonObject().get("url").getAsString();
    }

    public static String getQRKey() {
        JsonObject jsonObject = gson.fromJson(NeteaseApi.getUniKey(), JsonObject.class);
        return jsonObject == null ? null : jsonObject.getAsJsonObject("data").get("unikey").getAsString();
    }

    public static String getQRCodeImg(String key) {
        JsonObject jsonObject = gson.fromJson(NeteaseApi.generateQRCode(key), JsonObject.class);
        String data = jsonObject.getAsJsonObject("data").get("qrimg").getAsString();
        return data.split(",")[1];
    }

    public static void loginAnonymous() {
        JsonObject jsonObject = gson.fromJson(NeteaseApi.getAnonymous(), JsonObject.class);
        NeteaseApi.cookies = jsonObject.get("cookie").getAsString();
    }

    private static PlayList getSongsFromList(String id) {
        PlayList playList = new PlayList();
        try {
            JsonObject jsonObject = gson.fromJson(NeteaseApi.getPlayList(id), JsonObject.class);
            JsonElement songs = jsonObject.get("songs");
            for (JsonElement song : songs.getAsJsonArray()) {
                JsonObject songObject = song.getAsJsonObject();
                long id1 = songObject.get("id").getAsLong();
                String name = songObject.get("name").getAsString();
                StringBuilder artists = new StringBuilder();
                Iterator<JsonElement> artistIterator = songObject.getAsJsonArray("ar").iterator();
                while (artistIterator.hasNext()) {
                    artists.append(artistIterator.next().getAsJsonObject().get("name").getAsString());
                }
                String picUrl = songObject.getAsJsonObject("al").get("picUrl").getAsString();
                playList.add(new Music(id1, name, artists.toString(), picUrl));
            }
        } catch (Exception e) {
            // ignored
        }
        return playList;
    }

    public static PlayList getSongsFromDaily() {
        PlayList playList = new PlayList();
        try {
            JsonObject jsonObject = gson.fromJson(NeteaseApi.getDailyList(), JsonObject.class);
            JsonElement songs = jsonObject.getAsJsonObject("data").getAsJsonArray("dailySongs");
            for (JsonElement song : songs.getAsJsonArray()) {
                JsonObject songObject = song.getAsJsonObject();
                long id1 = songObject.get("id").getAsLong();
                String name = songObject.get("name").getAsString();
                StringBuilder artists = new StringBuilder();
                Iterator<JsonElement> artistIterator = songObject.getAsJsonArray("ar").iterator();
                while (artistIterator.hasNext()) {
                    artists.append(artistIterator.next().getAsJsonObject().get("name").getAsString());
                }
                String picUrl = songObject.getAsJsonObject("al").get("picUrl").getAsString();
                playList.add(new Music(id1, name, artists.toString(), picUrl));
            }
        } catch (Exception e) {
            // ignored
        }
        return playList;
    }

    public static void loadLyrics(Music music) {
        try {
            String verbatimLyrics = NeteaseApi.getVerbatimLyrics(music.id);
            if (verbatimLyrics.isEmpty()) {
                return;
            }
            JsonObject jsonObject = gson.fromJson(verbatimLyrics, JsonObject.class);
            JsonObject yrc = jsonObject.getAsJsonObject("yrc");
            if (yrc != null) {
                String lyrics = yrc.getAsJsonPrimitive("lyric").getAsString();
                if (lyrics != null) {
                    music.lyrics = parseLyrics(lyrics);
                }
            } else {
                JsonObject lrc = jsonObject.getAsJsonObject("lrc");
                String lrcs = lrc.getAsJsonPrimitive("lyric").getAsString();
                if (lrcs != null) {
                    music.lyrics = parseLyrics2(lrcs);
                } else {
                    Lyrics lyrics = new Lyrics();
                    Line line = new Line();
                    line.addWord(new Word("暂无歌词", 0, Long.MAX_VALUE));
                    line.time = 0;
                    line.duration = Long.MAX_VALUE;
                    lyrics.addLine(line);
                    music.lyrics = lyrics;
                }
            }
        } catch (Exception e) {
            ClientLogger.error("music", "An error occurred when loading lyrics of " + music.name);
            e.printStackTrace();
        }
    }

    private static Lyrics parseLyrics2(String str) {
        Lyrics lyrics = new Lyrics();
        str = str.replace("\n", System.lineSeparator());
        String[] split1 = str.split(System.lineSeparator());
        for (String s : split1) {
            if (s.startsWith("[")) {
                Line line = new Line();
                String[] split = s.split("]");
                if (split.length == 2) {
                    String time = split[0].substring(1);
                    line.timeTick = time;
                    line.type = 1;
                    String content = split[1];
                    line.addWord(new Word(content, 0, 0));
                    lyrics.addLine(line);
                }
            }
        }
        return lyrics;
    }

    private static Lyrics parseLyrics(String str) {
        Lyrics lyrics = new Lyrics();
        str = str.replace("\n", System.lineSeparator()).replace("\"", "\"");
        for (String s : str.split(System.lineSeparator())) {
            if (s.startsWith("[")) {
                Line line = new Line();
                String[] split = s.split("]");
                String time = split[0].split(",")[0].substring(1);
                String lineduration = split[0].split(",")[1];
                line.time = Long.parseLong(time);
                line.duration = Long.parseLong(lineduration);
                String content = split[1];
                String[] words = content.split("\\(");
                for (String word : words) {
                    if (!word.isEmpty()) {
                        String[] split1 = word.split("\\)");
                        String[] split2 = split1[0].split(",");
                        String wordContent = split1[1];
                        int start = Integer.parseInt(split2[0]);
                        int duration = Integer.parseInt(split2[1]);
                        line.addWord(new Word(wordContent, start, duration));
                    }
                }
                lyrics.addLine(line);
            }
        }
        return lyrics;
    }

    public static PlayList searchSongs(String keywords) {
        try {
            PlayList playList = new PlayList();
            String json = NeteaseApi.searchSongs(URLEncoder.encode(keywords, "UTF-8"));
            JsonObject jsonObject = gson.fromJson(json, JsonObject.class);
            JsonObject result = jsonObject.getAsJsonObject("result");
            JsonElement songs = result.getAsJsonArray("songs");
            for (JsonElement song : songs.getAsJsonArray()) {
                JsonObject songObject = song.getAsJsonObject();
                long id1 = songObject.get("id").getAsLong();
                String name = songObject.get("name").getAsString();
                StringBuilder artists = new StringBuilder();
                Iterator<JsonElement> artistIterator = songObject.getAsJsonArray("ar").iterator();
                while (artistIterator.hasNext()) {
                    artists.append(artistIterator.next().getAsJsonObject().get("name").getAsString()).append(" ");
                }
                String picUrl = songObject.getAsJsonObject("al").get("picUrl").getAsString();
                playList.add(new Music(id1, name, artists.toString(), picUrl));
            }
            return playList;
        } catch (Exception e) {
            e.printStackTrace();
            return new PlayList();
        }
    }

    public static PlayList searchList(String list) {
        return getSongsFromList(list);
    }

    public static JsonObject getLoginStatus(String key) {
        String json = NeteaseApi.checkLoginStatus(key);
        return gson.fromJson(json, JsonObject.class);
    }
}
