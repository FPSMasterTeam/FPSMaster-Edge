package top.fpsmaster.utils.os;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
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

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class HttpRequest {
    // 共享线程安全的HTTP客户端
    private static final CloseableHttpClient HTTP_CLIENT = HttpClients.createDefault();
    // 默认超时设置(15秒)
    private static final int DEFAULT_TIMEOUT = 15000;

    private HttpRequest() {} // 防止实例化

    public static Gson gson() {
        return new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    }

    // ================== GET 请求 ================== //
    public static String get(String url) {
        return executeRequest(new HttpGet(url), null);
    }

    public static String getWithCookie(String url, String cookie) {
        HttpGet request = new HttpGet(url);
        request.setHeader("Cookie", cookie.replace("\n", ""));
        return executeRequest(request, null);
    }

    public static String get(String url, Map<String, String> headers) {
        return executeRequest(new HttpGet(url), headers);
    }

    // ================== POST 请求 ================== //
    public static String post(String url, String body) {
        return post(url, body, "application/json");
    }

    public static String postJson(String url, JsonObject json) {
        return post(url, json.toString(), "application/json");
    }

    public static String postJson(String url, JsonObject json, Map<String, String> headers) {
        return post(url, json.toString(), "application/json", headers);
    }

    public static String postForm(String url, Map<String, String> params) {
        HttpPost request = new HttpPost(url);
        if (params != null && !params.isEmpty()) {
            List<NameValuePair> formData = new ArrayList<>();
            params.forEach((k, v) -> formData.add(new BasicNameValuePair(k, v)));
            request.setEntity(new UrlEncodedFormEntity(formData, StandardCharsets.UTF_8));
        }
        return executeRequest(request, null);
    }

    private static String post(String url, String body, String contentType) {
        return post(url, body, contentType, null);
    }
    private static String post(String url, String body, String contentType, Map<String, String> headers) {
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

    // ================== 文件下载 ================== //
    public static void downloadFile(String url, String filepath) {
        try (InputStream is = HTTP_CLIENT.execute(new HttpGet(url)).getEntity().getContent();
             FileOutputStream fos = new FileOutputStream(filepath)) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        } catch (Exception e) {
            ClientLogger.error("Download failed: " + e.getMessage());
        }
    }

    public static void downloadAsync(String url, String filepath, Runnable callback) {
        new Thread(() -> {
            downloadFile(url, filepath);
            callback.run();
        }).start();
    }

    // ================== 核心执行方法 ================== //
    private static String executeRequest(HttpRequestBase request, Map<String, String> headers) {
        try {
            // 设置请求配置和默认头
            request.setConfig(buildRequestConfig());
            addDefaultHeaders(request);

            // 添加自定义头部
            if (headers != null) {
                headers.forEach(request::addHeader);
            }

            // 执行请求并处理响应
            try (CloseableHttpResponse response = HTTP_CLIENT.execute(request)) {
                return handleResponse(response, request.getURI().toString());
            }
        } catch (Exception e) {
            ClientLogger.error("Request failed: " + e.getMessage());
            return "";
        }
    }

    // ================== 工具方法 ================== //
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

    private static String handleResponse(HttpResponse response, String url) throws IOException {
        int statusCode = response.getStatusLine().getStatusCode();
        HttpEntity entity = response.getEntity();

        if (statusCode < 200 || statusCode >= 300) {
            throw new IOException("HTTP Error: " + statusCode + " for URL: " + url);
        }

        return entity != null ? EntityUtils.toString(entity, StandardCharsets.UTF_8) : "";
    }
}