package com.holkan.tracker.service;

import org.apache.http.HttpResponse;

public class Response {

    private String content;
    private Object reference;
    private int resultCode = -1;
    private HttpResponse httpResponse;
    private Request request;

    public Response() {
        // TODO Auto-generated constructor stub
    }

    public void init(String content, HttpResponse httpResponse) {
        this.content = content;
        this.httpResponse = httpResponse;
    }

    public String getContent() {
        return content;
    }

    public HttpResponse getHttpResponse() {
        return httpResponse;
    }

    public Object getReference() {
        return reference;
    }

    public int getResultCode() {
        return resultCode;
    }

    public void setResultCode(int resultCode) {
        this.resultCode = resultCode;
    }

    public void setReference(Object reference) {
        this.reference = reference;
    }

    public Request getRequest() {
        return request;
    }

    public void setRequest(Request request) {
        this.request = request;
    }
}
