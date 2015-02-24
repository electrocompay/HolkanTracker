package com.holkan.tracker.service;

import android.text.TextUtils;
import android.util.Log;

import com.holkan.tracker.BuildConfig;
import com.holkan.tracker.Utils.Utils;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.RequestExpectContinue;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.concurrent.ExecutorService;
import java.util.zip.GZIPInputStream;

public abstract class Request {

    private static final String HEADER_ACCEPT_ENCODING = "Accept-Encoding";
    private static final String ENCODING_GZIP = "gzip";
    public static final String URL_BASE = BuildConfig.DEBUG ? "http://holkantracker.ddns.net:2254/api/" : "http://www.holkantracker.com/HolkanTracking/api/";

    private class RunGetContents implements Runnable {
        private final String url;
        private HttpClient connection;
        private final RequestListener requestListener;

        public RunGetContents(String url, RequestListener listener) {
            this.url = url;
            this.requestListener = listener;
        }

        @Override
        public void run() {
            HttpRequestBase urlRequest;
            if (getMethod() == HttpGet.METHOD_NAME) {
                urlRequest = new HttpGet(url);
            } else {
                urlRequest = new HttpPost(url);
            }


            Log.d(getClass().toString(), String.format("httpRequest - %s", urlRequest.getURI().toString()));

            // connection = Request.getNewHttpClient();
            HttpParams params = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(params, 3000);
            connection = httpGZIPClient(httpGZIPClient(new DefaultHttpClient(params)));
            ((DefaultHttpClient) connection).removeRequestInterceptorByClass(RequestExpectContinue.class);

            try {
                String payLoad = getPayLoad();

                if (urlRequest instanceof HttpPost && payLoad != null) {

                    StringEntity entity = new StringEntity(payLoad, "UTF-8");
                    // StringEntity entity = new StringEntity(payLoad);

                    ((HttpPost) urlRequest).setEntity(entity);
                    Log.d(getClass().toString(), String.format("Payload - %s", payLoad));
                }

                String data = null;
                HttpResponse response = null;
                String cacheKey = url + payLoad;

                long startTime = System.currentTimeMillis();
                urlRequest.setHeader("Content-Type", "Application/Json");
                response = connection.execute(urlRequest);
                HttpEntity entity = response.getEntity();
                InputStream content = entity.getContent();

                data = Utils.readFully(content);

                long endTime = System.currentTimeMillis();
                long intervalInMilliseconds = endTime - startTime;


                Log.d(getClass().toString(), String.format("Response Payload - %s", data));

                if (requestListener != null) {

                    if (response.getStatusLine().getStatusCode() / 100 == 2) {

                        requestListener.didReceiveContent(Request.this, response, data);

                    } else {

                        if (TextUtils.isEmpty(data)){
                            data = String.valueOf(response.getStatusLine().getStatusCode());
                        }
                        handleError(null, response, data);
                    }

                }
            } catch (Exception e) {
                handleError(e, null, null);
            }
        }

        private void handleError(Exception e, HttpResponse httpResponse, String content) {
            if (e != null) {
                e.printStackTrace();
            }

            if (requestListener != null) {
                requestListener.didReceiveError(Request.this, httpResponse, content);
            }
        }

    }

    public interface RequestListener {

        public void didReceiveContent(Request request, HttpResponse httpResponse, String content);

        public void didReceiveError(Request request, HttpResponse httpResponse, String content);

    }

    public static HttpClient httpclient;

    private static final long INVALID_TIME = -1;

    private RequestListener requestListener;
    private String requestId;
    private Object reference;
    private final ExecutorService executorService;
    private Response response;
    private long responseCachingTime = INVALID_TIME;
    private String urlBase;

    public Request(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public String getMethod() {
        return HttpPost.METHOD_NAME;
    }

    public RequestListener getRequestListener() {
        return requestListener;
    }

    public void setRequestListener(RequestListener requestListener) {
        this.requestListener = requestListener;
    }

    public void run() {

        String urlString = getURLBase() + asHttpString();
        RunGetContents runGetContents = new RunGetContents(urlString, requestListener);
        if (executorService != null) {
            executorService.execute(runGetContents);
        } else {
            runGetContents.run();
        }
    }

    public String getRequestId() {
        return requestId;
    }

    public abstract Class<?> getResponseClass();

    public abstract String asHttpString();

    public Object getReference() {
        return reference;
    }

    public void setReference(Object reference) {
        this.reference = reference;
    }

    public String getURLBase() {
        return URL_BASE;
    }

    public String getPayLoad() {
        return null;
    }

    public Response getResponse() {
        return response;
    }

    public void setResponse(Response response) {
        this.response = response;
    }

    private DefaultHttpClient httpGZIPClient(DefaultHttpClient client) {
        client.addRequestInterceptor(new HttpRequestInterceptor() {

            @Override
            public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
                // Add header to accept gzip content
                if (!request.containsHeader(HEADER_ACCEPT_ENCODING)) {
                    request.addHeader(HEADER_ACCEPT_ENCODING, ENCODING_GZIP);
                }
            }

        });

        client.addResponseInterceptor(new GzipHttpResponseInterceptor());

        return client;
    }

    private class GzipHttpResponseInterceptor implements HttpResponseInterceptor {
        @Override
        public void process(final HttpResponse response, final HttpContext context) {
            final HttpEntity entity = response.getEntity();
            final Header encoding = entity.getContentEncoding();
            if (encoding != null) {
                inflateGzip(response, encoding);
            }
        }

        private void inflateGzip(final HttpResponse response, final Header encoding) {
            for (HeaderElement element : encoding.getElements()) {
                if (element.getName().equalsIgnoreCase("gzip")) {
                    response.setEntity(new GzipInflatingEntity(response.getEntity()));
                    break;
                }
            }
        }
    }

    private class GzipInflatingEntity extends HttpEntityWrapper {
        public GzipInflatingEntity(final HttpEntity wrapped) {
            super(wrapped);
        }

        @Override
        public InputStream getContent() throws IOException {
            return new GZIPInputStream(wrappedEntity.getContent());
        }

        @Override
        public long getContentLength() {
            return -1;
        }
    }
}
