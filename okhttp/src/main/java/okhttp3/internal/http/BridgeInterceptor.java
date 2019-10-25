/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package okhttp3.internal.http;

import java.io.IOException;
import java.util.List;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.internal.Version;
import okio.GzipSource;
import okio.Okio;

import static okhttp3.internal.Util.hostHeader;

/**
 * Bridges from application code to network code. First it builds a network request from a user
 * request. Then it proceeds to call the network. Finally it builds a user response from the network
 * response.
 *
 * 该拦截器用来处理网络请求前和网络请求后的一些处理的
 *
 * 该拦截器的主要工作就是创建一些请求头，包括：请求体类型，请求体长度，主机，还有最重要的Cookie也是在这里添加的。
 *
 * 同时该请求会处理响应的头，包括保存相应的Cookie信息，以及处理GZIP压缩过的响应体，下面我们就来看看看详细的源码
 */
public final class BridgeInterceptor implements Interceptor {
  // 用来处理cookie的保存和读取
  private final CookieJar cookieJar;

  public BridgeInterceptor(CookieJar cookieJar) {
    this.cookieJar = cookieJar;
  }

  @Override public Response intercept(Chain chain) throws IOException {
    Request userRequest = chain.request();
    // 根据用户创建的request，在创建一个builder，这么做是因为还需要通过builder添加一些信息
    Request.Builder requestBuilder = userRequest.newBuilder();

    // 获取请求体
    RequestBody body = userRequest.body();
    if (body != null) {
      MediaType contentType = body.contentType();
      if (contentType != null) {
        // 增加内容类型
        requestBuilder.header("Content-Type", contentType.toString());
      }

      long contentLength = body.contentLength();
      if (contentLength != -1) {
        // 添加内容长度
        requestBuilder.header("Content-Length", Long.toString(contentLength));
        requestBuilder.removeHeader("Transfer-Encoding");
      } else {
        requestBuilder.header("Transfer-Encoding", "chunked");
        requestBuilder.removeHeader("Content-Length");
      }
    }

    if (userRequest.header("Host") == null) {
      // 如果没有主机，就添加主机头
      requestBuilder.header("Host", hostHeader(userRequest.url(), false));
    }
    // 连接类型
    if (userRequest.header("Connection") == null) {
      requestBuilder.header("Connection", "Keep-Alive");
    }

    // 如果用户没有指定接受的编码的编码，就添加gzip头
    // If we add an "Accept-Encoding: gzip" header field we're responsible for also decompressing
    // the transfer stream.
    boolean transparentGzip = false;
    if (userRequest.header("Accept-Encoding") == null && userRequest.header("Range") == null) {
      transparentGzip = true;
      requestBuilder.header("Accept-Encoding", "gzip");
    }

    // 从url里面获取cookie
    List<Cookie> cookies = cookieJar.loadForRequest(userRequest.url());
    if (!cookies.isEmpty()) {
      // 将所有的cookie添加到cookie头
      requestBuilder.header("Cookie", cookieHeader(cookies));
    }

    if (userRequest.header("User-Agent") == null) {
      requestBuilder.header("User-Agent", Version.userAgent());
    }
    // 调用下一个拦截器
    Response networkResponse = chain.proceed(requestBuilder.build());

    // 从网络的响应中获取cookie，这个方法里面
    HttpHeaders.receiveHeaders(cookieJar, userRequest.url(), networkResponse.headers());

    // 根据网络的响应创建一个builder，这和前面从请求体创建一个builder类似
    Response.Builder responseBuilder = networkResponse.newBuilder()
        .request(userRequest);

    if (transparentGzip
        && "gzip".equalsIgnoreCase(networkResponse.header("Content-Encoding"))
        && HttpHeaders.hasBody(networkResponse)) {
      // 在这里面响应体是gzip类型的工作
      GzipSource responseBody = new GzipSource(networkResponse.body().source());
      Headers strippedHeaders = networkResponse.headers().newBuilder()
          .removeAll("Content-Encoding")
          .removeAll("Content-Length")
          .build();
      responseBuilder.headers(strippedHeaders);
      String contentType = networkResponse.header("Content-Type");
      // 这里用了一个包装类realResponseBody,用来转换gzip响应体，这个后面分析
      responseBuilder.body(new RealResponseBody(contentType, -1L, Okio.buffer(responseBody)));
    }
    // 根据响应体builder创建一个response
    return responseBuilder.build();
  }
  // 用来将所有的cookie转换成 key1=value1;
  /** Returns a 'Cookie' HTTP request header with all cookies, like {@code a=b; c=d}. */
  private String cookieHeader(List<Cookie> cookies) {
    StringBuilder cookieHeader = new StringBuilder();
    for (int i = 0, size = cookies.size(); i < size; i++) {
      if (i > 0) {
        cookieHeader.append("; ");
      }
      Cookie cookie = cookies.get(i);
      cookieHeader.append(cookie.name()).append('=').append(cookie.value());
    }
    return cookieHeader.toString();
  }
}

//可以看到当Okhttp将一个特定的功能抽象到单独一个拦截器中时，会发现每个拦截器的功能是非常简单的。
// 虽然这个拦截器看似简单，但其实还有几个点我们没弄清楚，比如：Cookie保存，以及如何将Gzip的响应体转换成正常的流
