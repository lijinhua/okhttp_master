package okhttp3.guide;

import java.io.IOException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 调用流程
 * 创建request
 * 使用okhttpClient根据request创建realcall
 * 调用realcall的execute方法来执行当前的call
 * 使用拦截器来执行所有的拦截器
 * 返回最后一个拦截器的reponse
 */
public class GetExample {
  OkHttpClient client = new OkHttpClient();

  String run(String url) throws IOException {
    Request request = new Request.Builder()
        .url(url)
        .build();

    try (Response response = client.newCall(request).execute()) {
      return response.body().string();
    }
  }

  public static void main(String[] args) throws IOException {
    GetExample example = new GetExample();
    String response = example.run("https://raw.github.com/square/okhttp/master/README.md");
    System.out.println(response);
  }
}
