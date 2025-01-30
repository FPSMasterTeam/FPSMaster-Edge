package top.fpsmaster.utils.thirdparty.openai;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import top.fpsmaster.FPSMaster;
import top.fpsmaster.modules.logger.ClientLogger;
import top.fpsmaster.utils.os.HttpRequest;

import java.util.HashMap;
import java.util.Map;

public class OpenAI {

    String baseUrl;
    String openAiKey;
    String model;
    String prompt;

    public OpenAI(String baseUrl, String openAiKey, String model, String prompt) {
        this.baseUrl = baseUrl;
        this.openAiKey = openAiKey;
        this.model = model;
        this.prompt = prompt;
    }

    public String requestNewAnswer(String question, JsonArray msgs) {
        JsonObject systemRole = new JsonObject();
        systemRole.addProperty("role", "system");
        systemRole.addProperty("content", prompt);

        JsonArray messages = new JsonArray();
        messages.add(systemRole);
        messages.addAll(msgs);

        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.add("messages", messages);
        String json = body.toString();

        Map<String, String> hashMap = new HashMap<>();
        hashMap.put("Content-Type", "application/json");
        hashMap.put("Authorization", "Bearer " + openAiKey);

        String[] text;
        try {
            text = HttpRequest.sendPostRequest(baseUrl + "/chat/completions", json, hashMap);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return getString(text);
    }

    public String requestNewAnswer(String question) {
        JsonObject systemRole = new JsonObject();
        systemRole.addProperty("role", "system");
        systemRole.addProperty("content", prompt);

        JsonObject userRole = new JsonObject();
        userRole.addProperty("role", "user");
        userRole.addProperty("content", question);

        JsonArray messages = new JsonArray();
        messages.add(systemRole);
        messages.add(userRole);

        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.add("messages", messages);
        String json = body.toString();

        Map<String, String> hashMap = new HashMap<>();
        hashMap.put("Content-Type", "application/json");
        hashMap.put("Authorization", "Bearer " + openAiKey);

        String[] text;
        try {
            text = HttpRequest.sendPostRequest(baseUrl + "/chat/completions", json, hashMap);
        } catch (Exception e) {
            ClientLogger.error("Translator", e.toString());
            return "";
        }

        return getString(text);
    }

    private String getString(String[] response) {
        if (response.length != 2) return "failed";

        JsonObject responseJson = new JsonParser().parse(response[1]).getAsJsonObject();
        if ("200".equals(response[0])) {
            return responseJson.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .getAsJsonObject("content")
                    .getAsString();
        } else {
            JsonObject errorJson = responseJson.getAsJsonObject("error");
            throw new RuntimeException("OpenAI returned an error: " + errorJson.getAsJsonPrimitive("message").getAsString());
        }
    }

    public static String[] requestClientAI(String prompt, String model, JsonArray messages) {
        try {
            String sendPostRequest = HttpRequest.sendPostRequest(
                    FPSMaster.SERVICE_API + "/chat?timestamp=" + System.currentTimeMillis(),
                    new Gson().toJson(messages),
                    new HashMap<String, String>() {{
                        put("Content-Type", "application/json");
                        put("username", FPSMaster.accountManager.getUsername());
                        put("token", FPSMaster.accountManager.getToken());
                        put("model", model);
                        put("prompt", prompt);
                    }}
            )[1];

            if (!sendPostRequest.isEmpty()) {
                JsonObject json = new JsonParser().parse(sendPostRequest).getAsJsonObject();
                if ("200".equals(sendPostRequest)) {
                    return new String[]{
                            json.get("code").getAsString(),
                            json.get("data").getAsString()
                    };
                } else {
                    return new String[]{"501", "Server went wrong"};
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new String[]{"500", e.toString()};
        }
        return new String[]{"500", "Request failed"};
    }
}
