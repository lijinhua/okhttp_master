package okhttp3.test.intercept;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

public class MyCallServerInterceptor implements Interceptor {
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();

        // request.setUrl("<MyCallServerInterceptor before request> "+request.url());

       // 最后一个拦截器，不调用chain.procedd的，不然没有办法结束

        //  创建结果
//       Response response = new Response(request.getUrl());

       // 再结结果处理
//       response.setUrl(response.getUrl()+"<MyCallServerInterceptor> after request>");
//       return response;
        return null;
    }
}
