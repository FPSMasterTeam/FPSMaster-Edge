package top.fpsmaster.modules.client.api.model;

import com.google.gson.JsonObject;

public class ApiResponse<T> {
    /** 没有携带状态码时的取值（成功响应、或调用方没给状态码）。 */
    public static final int NO_CODE = 0;

    /** 请求根本没发出去/没拿到响应（网络层失败）。 */
    public static final int NETWORK_ERROR = -1;

    private final boolean success;
    private final String message;
    private final T data;

    /**
     * HTTP 状态码（或 {@link #NETWORK_ERROR}）。以前 {@link #error(int, String, String)} 把它丢掉了，
     * 结果 UI 只能拿裸英文 message 显示，没法按 401 / 网络失败分流到本地化文案。
     */
    private final int code;

    /**
     * {@link #message} 是不是后端按契约给的。
     *
     * <p>解析失败、空正文这些兜底路径也会填一个 message（"Parse error" 之类），直接贴到
     * 界面上就是一句英文技术黑话。UI 想「优先显示后端原话」时必须先问这个，否则
     * Cloudflare 挡下来的 403 会被当成封禁原因显示出去。
     */
    private final boolean serverMessage;

    private ApiResponse(boolean success, String message, T data) {
        this(success, message, data, NO_CODE, false);
    }

    private ApiResponse(boolean success, String message, T data, int code) {
        this(success, message, data, code, false);
    }

    private ApiResponse(boolean success, String message, T data, int code, boolean serverMessage) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.code = code;
        this.serverMessage = serverMessage;
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    public static <T> ApiResponse<T> error(int code, String message, String error) {
        return new ApiResponse<>(false, message, null, code, false);
    }

    /** 转发一个后端原话时用这个重载，别让 {@link #hasServerMessage()} 在转手时丢掉。 */
    public static <T> ApiResponse<T> error(int code, String message, String error, boolean serverMessage) {
        return new ApiResponse<>(false, message, null, code, serverMessage);
    }

    public static <T> ApiResponse<T> fromJson(JsonObject json, Class<T> dataClass) {
        return fromJson(json, dataClass, NO_CODE);
    }

    /**
     * 带上真实 HTTP 状态码的解析。调用方手上一般都有 {@code response.getStatusCode()}，
     * 传进来 UI 才能把「200 但 success:false」和「网络断了」分流到不同文案——不传的话
     * 两者都会落到 {@link #NO_CODE}，被本地化逻辑当成未知错误、直接贴后端裸英文。
     */
    public static <T> ApiResponse<T> fromJson(JsonObject json, Class<T> dataClass, int statusCode) {
        if (json == null) {
            // 「拿到了响应但正文不是 JSON 对象」不是断网。标成 NETWORK_ERROR 的话界面会
            // 显示「无法连接服务器」，把排障方向指到网络上去。
            return error(statusCode, "Invalid response", "Response is null");
        }

        boolean success = json.has("success") && json.get("success").getAsBoolean();
        String message = json.has("message") && !json.get("message").isJsonNull()
                ? json.get("message").getAsString() : "";
        // 「后端按契约给的原话」得连信封一起认：`success` 字段在才算契约响应。Cloudflare
        // 之类挡在前面时正文可能是一段带 message 的 JSON，只看 message 非空就会把它当成
        // 封禁原因贴到界面上。
        boolean serverMessage = !message.isEmpty() && json.has("success");

        T data = null;
        if (json.has("data") && !json.get("data").isJsonNull()) {
            try {
                if (dataClass != null && dataClass != Void.class) {
                    data = FPSMasterGson.getInstance().fromJson(json.get("data"), dataClass);
                }
            } catch (Exception e) {
                // 这是「响应到了但 data 结构不对」，不是网络失败——以前它返回 NETWORK_ERROR，
                // 界面会显示「无法连接服务器」，把排障方向指到网络上去。
                return new ApiResponse<T>(false, message, null, statusCode, serverMessage);
            }
        }

        return new ApiResponse<>(success, message, data, statusCode, serverMessage);
    }

    public static ApiResponse<Void> fromJson(JsonObject json) {
        return fromJson(json, Void.class, NO_CODE);
    }

    public static ApiResponse<Void> fromJson(JsonObject json, int statusCode) {
        return fromJson(json, Void.class, statusCode);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public int getCode() {
        return code;
    }

    /** {@link #getMessage()} 是不是后端按契约给的原话；兜底路径造的技术文案不算。 */
    public boolean hasServerMessage() {
        return serverMessage;
    }

    @Override
    public String toString() {
        return "ApiResponse{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", code=" + code +
                ", data=" + data +
                '}';
    }
}
