package top.fpsmaster.utils.os;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import top.fpsmaster.modules.logger.ClientLogger;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class HttpRequest {
    // Shared thread-safe HTTP client
    private static final CloseableHttpClient HTTP_CLIENT = HttpClients.createDefault();
    // Default timeout settings (15 seconds)
    private static final int DEFAULT_TIMEOUT = 15000;

    // Response wrapper class
    public static class HttpResponseResult {
        private final int statusCode;
        private final String body;

        public HttpResponseResult(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getBody() {
            return body;
        }

        public boolean isSuccess() {
            return statusCode >= 200 && statusCode < 300;
        }
    }

    private HttpRequest() {
    } // Prevent instantiation

    public static Gson gson() {
        return new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    }

    // ================== GET Requests ================== //
    public static HttpResponseResult get(String url) throws IOException {
        return executeRequest(new HttpGet(url), null);
    }

    public static HttpResponseResult getWithCookie(String url, String cookie) throws IOException {
        HttpGet request = new HttpGet(url);
        request.setHeader("Cookie", cookie.replace("\n", ""));
        return executeRequest(request, null);
    }

    public static HttpResponseResult get(String url, Map<String, String> headers) throws IOException {
        return executeRequest(new HttpGet(url), headers);
    }

    // ================== POST Requests ================== //
    public static HttpResponseResult post(String url, String body) throws IOException {
        return post(url, body, "application/json");
    }

    public static HttpResponseResult postJson(String url, JsonObject json) throws IOException {
        return post(url, json.toString(), "application/json");
    }

    public static HttpResponseResult postJson(String url, JsonObject json, Map<String, String> headers) throws IOException {
        return post(url, json.toString(), "application/json", headers);
    }

    public static HttpResponseResult postForm(String url, Map<String, String> params) throws IOException {
        HttpPost request = new HttpPost(url);
        if (params != null && !params.isEmpty()) {
            List<NameValuePair> formData = new ArrayList<>();
            params.forEach((k, v) -> formData.add(new BasicNameValuePair(k, v)));
            request.setEntity(new UrlEncodedFormEntity(formData, StandardCharsets.UTF_8));
        }
        return executeRequest(request, null);
    }

    public static HttpResponseResult post(String url, String body, HashMap<String, String> headers) throws IOException {
        return post(url, body, "application/json", headers);
    }

    private static HttpResponseResult post(String url, String body, String contentType) throws IOException {
        return post(url, body, contentType, null);
    }

    private static HttpResponseResult post(String url, String body, String contentType, Map<String, String> headers) throws IOException {
        HttpPost request = new HttpPost(url);
        if (headers != null) {
            headers.forEach(request::setHeader);
        }
        if (body != null) {
            StringEntity entity = new StringEntity(body, StandardCharsets.UTF_8);
            entity.setContentType(contentType);
            request.setEntity(entity);
        }
        return executeRequest(request, null);
    }

    // ================== File Download ================== //
    public static boolean downloadFile(String url, String filepath) {
        try (InputStream is = HTTP_CLIENT.execute(new HttpGet(url)).getEntity().getContent();
             FileOutputStream fos = new FileOutputStream(filepath)) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
            return true;
        } catch (Exception e) {
            ClientLogger.error("Download failed: " + e.getMessage());
            return false;
        }
    }

    public static boolean downloadFile(String url, String filepath, ProgressCallback callback) {
        try {
            HttpResponse response = HTTP_CLIENT.execute(new HttpGet(url));
            HttpEntity entity = response.getEntity();
            long totalSize = entity.getContentLength();

            try (InputStream is = entity.getContent();
                 FileOutputStream fos = new FileOutputStream(filepath)) {

                byte[] buffer = new byte[8192];
                int bytesRead;
                long downloadedSize = 0;

                while ((bytesRead = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                    downloadedSize += bytesRead;

                    // Invoke progress callback if provided
                    if (callback != null) {
                        callback.onProgress(downloadedSize, totalSize);
                    }
                }
                return true;
            }
        } catch (Exception e) {
            ClientLogger.error("Download failed: " + e.getMessage());
            return false;
        }
    }

    public interface ProgressCallback {
        void onProgress(long downloadedBytes, long totalBytes);
    }

    // download file to buffer
    public static InputStream downloadFile(String url) {
        try {
            return HTTP_CLIENT.execute(new HttpGet(url)).getEntity().getContent();
        } catch (Exception e) {
            ClientLogger.error("Download failed: " + e.getMessage());
            return null;
        }
    }

    public static void downloadAsync(String url, String filepath, Runnable callback) {
        new Thread(() -> {
            boolean success = downloadFile(url, filepath);
            if (success && callback != null) {
                callback.run();
            }
        }).start();
    }


    public static BufferedImage downloadImage(String imageUrl) throws IOException {
        // 验证URL是否为空
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("图片URL不能为空");
        }

        // 创建URL对象
        URL url = new URL(imageUrl);

        // 使用ImageIO读取URL中的图片并转换为BufferedImage
        BufferedImage image = ImageIO.read(url);

        // 检查是否成功读取图片
        if (image == null) {
            throw new IOException("Error when reading image from URL: " + imageUrl);
        }

        return image;
    }


    // ================== Core Execution Method ================== //
    private static HttpResponseResult executeRequest(HttpRequestBase request, Map<String, String> headers) throws IOException {
        // Set request configuration and default headers
        request.setConfig(buildRequestConfig());
        addDefaultHeaders(request);

        // Add custom headers
        if (headers != null) {
            headers.forEach(request::addHeader);
        }
        CloseableHttpResponse response = HTTP_CLIENT.execute(request);
        return handleResponse(response, request.getURI().toString());
    }

    // ================== Utility Methods ================== //
    private static RequestConfig buildRequestConfig() {
        return RequestConfig.custom()
                .setConnectTimeout(DEFAULT_TIMEOUT)
                .setConnectionRequestTimeout(DEFAULT_TIMEOUT)
                .setSocketTimeout(DEFAULT_TIMEOUT)
                .build();
    }

    private static void addDefaultHeaders(HttpRequestBase request) {
        request.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36");
        request.setHeader("Accept", "application/json, text/html, */*");
        request.setHeader("Accept-Language", "en-US,en;q=0.9");
    }

    private static HttpResponseResult handleResponse(HttpResponse response, String url) throws IOException {
        int statusCode = response.getStatusLine().getStatusCode();
        HttpEntity entity = response.getEntity();

        String body = entity != null ? EntityUtils.toString(entity, StandardCharsets.UTF_8) : "";
        return new HttpResponseResult(statusCode, body);
    }
}