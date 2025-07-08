package top.fpsmaster.utils.thirdparty.openai;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.entity.StringEntity;
import com.google.gson.*;
import top.fpsmaster.exception.FileException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OpenAIClient {

    private static final String API_KEY = "sk-buhpazkvJ04hJYwp475fAb626c8c44B6AbE1D66448F5BdAd";  // 替换为你的API密钥
    private static final String API_URL = "https://api.aiskt.com/v1/chat/completions";
    private static final HttpClient client = HttpClients.createDefault();
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    // 阻塞式调用：等待全部数据返回后才返回
    public static String getChatResponse(ArrayList<Message> userMessages) throws IOException {
        StringBuilder responseBuilder = new StringBuilder();
        String jsonBody = buildRequestJson(userMessages);

        HttpPost postRequest = createPostRequest(jsonBody);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(client.execute(postRequest).getEntity().getContent()))) {
            String line;
            while ((line = reader.readLine()) != null) {
//                responseBuilder.append(line);
                if (processResponseLine(line, responseBuilder)) {
                    break;  // 如果检测到回复已完成，则退出
                }
            }
        }

        return responseBuilder.toString();  // 返回拼接后的完整响应
    }

    // 非阻塞式调用：响应流将通过回调处理
    public static void getChatResponseAsync(ArrayList<Message> userMessages, ResponseCallback callback) {
        executor.submit(() -> {
            try {
                String jsonBody = buildRequestJson(userMessages);
                HttpPost postRequest = createPostRequest(jsonBody);
                StringBuilder responseBuilder = new StringBuilder();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(client.execute(postRequest).getEntity().getContent(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
//                        responseBuilder.append(line);
                        if (processResponseLine(line, responseBuilder)) {
                            callback.onFinish(responseBuilder.toString());  // 非阻塞调用，处理数据
                            break;  // 如果检测到回复已完成，则退出
                        }
                        callback.onResponse(responseBuilder.toString());  // 非阻塞调用，处理数据
                    }
                }
            } catch (IOException e) {
                callback.onError(e);
            }
        });
    }

    // 创建POST请求
    private static HttpPost createPostRequest(String jsonBody) {
        HttpPost postRequest = new HttpPost(API_URL);
        postRequest.setHeader("Authorization", "Bearer " + API_KEY);
        postRequest.setHeader("Content-Type", "application/json; charset=UTF-8\"");

        try {
            StringEntity entity = new StringEntity(jsonBody, "UTF-8");
            postRequest.setEntity(entity);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return postRequest;
    }

    // 构建请求体JSON
    private static String buildRequestJson(ArrayList<Message> messages) {
        StringBuilder messagesJson = new StringBuilder();

        for (Message message : messages) {
            messagesJson.append("{\"role\": \"").append(message.role).append("\", \"content\": \"").append(StringEscapeUtils.escapeJson(message.content)).append("\"},");
        }

        // 移除最后一个逗号
        if (messagesJson.length() > 0) {
            messagesJson.deleteCharAt(messagesJson.length() - 1);
        }

        return "{\n" +
                "  \"model\": \"gpt-3.5-turbo\",\n" +
                "  \"messages\": [" + messagesJson + "],\n" +
                "  \"stream\": true\n" +
                "}";
    }

    // 处理每一行的响应数据并解析回复文本
    private static boolean processResponseLine(String line, StringBuilder responseBuilder) {
        // OpenAI 的流式响应数据通常会以 "data: " 开头，我们需要忽略它
        if (line.startsWith("data: ")) {
            // 去掉前缀"data: "，并解析剩余的 JSON 部分
            String jsonData = line.substring(6).trim();

            try {
                JsonElement jsonElement = new JsonParser().parse(jsonData);
                if (jsonElement.isJsonObject()) {
                    JsonObject jsonObject = jsonElement.getAsJsonObject();
                    if (jsonObject.has("choices")) {
                        JsonArray choices = jsonObject.getAsJsonArray("choices");
                        for (JsonElement choice : choices) {
                            JsonObject choiceObj = choice.getAsJsonObject();
                            if (choiceObj.has("delta")) {
                                JsonObject messageObj = choiceObj.getAsJsonObject("delta");
                                if (messageObj.has("content")) {
                                    String content = messageObj.get("content").getAsString();
                                    responseBuilder.append(content);  // 拼接回复文本
                                }
                            }
                            // 检查finish_reason字段，如果是stop，表示消息已完成
                            if (choiceObj.has("finish_reason") && choiceObj.get("finish_reason").getAsString().equals("stop")) {
                                return true;  // 发现回复完成，返回true
                            }
                        }
                    }
                }
            } catch (JsonParseException e) {
                e.printStackTrace();
            }
        }
        return false;  // 如果没有完成，继续读取
    }

    // 回调接口：非阻塞方式
    public interface ResponseCallback {
        void onResponse(String response);  // 当成功获取到响应时调用

        void onError(Exception e);  // 当发生错误时调用

        void onFinish(String string) throws FileException;
    }

    public static class Message {
        String content;
        String role;

        public Message(String role, String content) {
            this.content = content;
            this.role = role;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }
    }
}