package okhttp3.test.intercept;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

public class AInterceptor implements Interceptor {
    @Override
    public Response intercept(Chain chain) throws IOException {
       Request request = chain.request();
        // 在调用下一个请求前，可以更改request的信息
        // 从而可以控制请求关，链接，传递的参数等
        // request.setUrl("<AInterceptor before request> "+request.url());

        // 调用了下一个拦截器
       Response proceed = chain.proceed(request);
       // 还可以对结果加工，加工完成返回
      //  proceed.setUrl(proceed.request().url()+"<AInterceptor after request>");
        return proceed;
    }
}
