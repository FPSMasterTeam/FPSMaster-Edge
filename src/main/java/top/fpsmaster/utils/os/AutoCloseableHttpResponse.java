package top.fpsmaster.utils.os;

import org.apache.http.HttpResponse;

import java.io.IOException;

public class AutoCloseableHttpResponse implements AutoCloseable {
    private final HttpResponse httpResponse;

    public AutoCloseableHttpResponse(HttpResponse httpResponse) {
        this.httpResponse = httpResponse;
    }

    public HttpResponse getHttpResponse() {
        return httpResponse;
    }

    @Override
    public void close() throws IOException {
        // 这里实现关闭操作，释放HttpResponse的资源
        if (httpResponse != null) {
            httpResponse.getEntity().getContent().close();
        }
    }
}
