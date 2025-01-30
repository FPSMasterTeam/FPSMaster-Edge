package top.fpsmaster.utils.os;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.entity.StringEntity;
import top.fpsmaster.modules.logger.ClientLogger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class HttpRequest {
    private static final HttpClient client = HttpClients.createDefault();
    private static final int TIMEOUT = 15000;

    public static String get(String url) {
        return getWithCookie(url, "");
    }

    public static String getWithCookie(String url, String cookie) {
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("URL cannot be null or empty");
        }

        HttpGet request = new HttpGet(url);
        request.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36");

        if (!cookie.isEmpty()) {
            request.setHeader("Cookie", cookie.replace("\n", ""));
        }

        try (AutoCloseableHttpResponse response = new AutoCloseableHttpResponse(client.execute(request));
             BufferedReader reader = new BufferedReader(new InputStreamReader(response.getHttpResponse().getEntity().getContent(), StandardCharsets.UTF_8))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            return builder.toString();
        } catch (IOException e) {
            ClientLogger.error("Error during HTTP GET request to " + url + ": " + e.getMessage());
        }
        return "";
    }

    public static void downloadFile(String url, String filepath) {
        try {
            HttpGet request = new HttpGet(url);
            try (AutoCloseableHttpResponse response = new AutoCloseableHttpResponse(client.execute(request));
                 InputStream is = response.getHttpResponse().getEntity().getContent();
                 FileOutputStream fileout = new FileOutputStream(filepath)) {

                long totalLen = response.getHttpResponse().getEntity().getContentLength();
                long unit = totalLen / 100;
                byte[] buffer = new byte[10 * 1024];
                long progress = 0;
                int ch;
                while ((ch = is.read(buffer)) != -1) {
                    fileout.write(buffer, 0, ch);
                    progress += ch;
                    if (progress % 10 == 0) {
                        ClientLogger.info("Downloaded " + progress / unit + "%");
                    }
                }
                fileout.flush();
            }
        } catch (Exception e) {
            ClientLogger.error("Failed to download file from " + url + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static String[] sendPostRequest(String targetUrl, String body, Map<String, String> headers) throws IOException {
        String[] response = new String[2];
        HttpPost request = new HttpPost(targetUrl);

        // Add headers
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            request.setHeader(entry.getKey(), entry.getValue().trim());
        }

        // Add body
        StringEntity entity = new StringEntity(body, StandardCharsets.UTF_8);
        request.setEntity(entity);

        try (AutoCloseableHttpResponse httpResponse = new AutoCloseableHttpResponse(client.execute(request));
             BufferedReader reader = new BufferedReader(new InputStreamReader(
                     httpResponse.getHttpResponse().getEntity().getContent(), StandardCharsets.UTF_8))) {

            // Get the response code
            response[0] = String.valueOf(httpResponse.getHttpResponse().getStatusLine().getStatusCode());

            StringBuilder content = new StringBuilder();
            String inputLine;
            while ((inputLine = reader.readLine()) != null) {
                content.append(inputLine);
            }
            response[1] = content.toString();
        }
        return response;
    }

    public static void downloadAsync(String url, String filepath, Runnable callback) {
        new Thread(() -> {
            try {
                downloadFile(url, filepath);
                callback.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
