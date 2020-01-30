package com.teachassist.teachassist;


import android.text.Html;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

import org.apache.commons.lang.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class SendRequest {



    public String[] send(String URL, HashMap<String, String> headers,
                         LinkedHashMap<String, String> Parameters, final HashMap<String, String> Cookies,
                         final String path, boolean shouldFollowRedirects) throws IOException {

        try {
            final List<String> TAcookies = new ArrayList();
            OkHttpClient client = new OkHttpClient().newBuilder()
                    .cookieJar(new CookieJar() {
                        @Override
                        public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                            for(Cookie temp:cookies) {
                                TAcookies.add(temp.toString());

                            }
                        }

                        @Override
                        public List<Cookie> loadForRequest(HttpUrl url) {
                            final ArrayList<Cookie> oneCookie = new ArrayList<>(1);
                            for (Map.Entry<String, String> entry : Cookies.entrySet()) {
                                oneCookie.add(new Cookie.Builder()
                                        .domain("ta.yrdsb.ca")
                                        .path("/")
                                        .name(entry.getKey())
                                        .value(entry.getValue())
                                        .httpOnly()
                                        .secure()
                                        .build());
                            }

                            return oneCookie;
                        }
                    })
                    .followRedirects(shouldFollowRedirects)
                    .build();






            //add headers to map object
            headers.put("Host", "ta.yrdsb.ca");
            headers.put("Connection", "keep-alive");
            headers.put("Upgrade-Insecure-Requests", "1");
            headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/69.0.3497.100 Safari/537.36");
            headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8");
            headers.put("Referer", "https://ta.yrdsb.ca/live/index.php");
            headers.put("Accept-Encoding", "gzip, deflate, br");
            headers.put("Accept-Language", "en-US,en;q=0.9,vi-VN;q=0.8,vi;q=0.7");

            // add headers
            Headers.Builder builder = new Headers.Builder();
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                builder.add(entry.getKey(), entry.getValue());
            }
            builder.build();


            // add parameters
            FormBody.Builder requestBodyForm = new FormBody.Builder();
                    //.setType(MultipartBody.FORM);
            for (Map.Entry<String, String> entry : Parameters.entrySet()) {
                requestBodyForm.add(entry.getKey(), entry.getValue());
            }

            FormBody requestBody = requestBodyForm.build();


            //build request
            Request request = new Request.Builder()
                    .url(URL)
                    .post(requestBody)
                    .build();

            //response
            Response response = client.newCall(request).execute();
            //System.out.println(response.code());



            // generate return list and add cookies
            String returnList[] = new String[TAcookies.size()+1];
            returnList[0] = StringEscapeUtils.unescapeHtml(response.body().string());
            for (String i : TAcookies) {
                returnList[TAcookies.indexOf(i)+1] = i;
            }
            return returnList;


        }
        catch (IOException e) {
            e.printStackTrace();
            System.out.println("POST Request failed");
            Crashlytics.log(Log.ERROR, "network request failed", "line 131 SR");
            return null;
        }

    }





    public JSONArray sendJson(String URL, String json) throws IOException {
        JSONArray jsonObjectResp = null;

        try {

            MediaType JSON = MediaType.parse("application/json; charset=utf-8");
            OkHttpClient client = new OkHttpClient();

            okhttp3.RequestBody body = RequestBody.create(JSON, json.toString());
            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(URL)
                    .post(body)
                    .build();

                okhttp3.Response response = client.newCall(request).execute();

            String networkResp = response.body().string().replaceAll("&quot;", "'"); // raises exception if first 2 escape chars arent present
            networkResp = StringEscapeUtils.unescapeHtml(networkResp);
            if (!networkResp.isEmpty()) {
                jsonObjectResp = new JSONArray(networkResp);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Crashlytics.log(Log.ERROR, "network request failed", "line 160 SR");
            return null;
        }

        return jsonObjectResp;
    }



    public JSONObject sendJsonNotifications(String URL, String json) throws IOException {
        JSONObject jsonObjectResp = null;

        try {

            MediaType JSON = MediaType.parse("application/json; charset=utf-8");
            OkHttpClient client = new OkHttpClient();

            okhttp3.RequestBody body = RequestBody.create(JSON, json.toString());
            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(URL)
                    .post(body)
                    .build();

            okhttp3.Response response = client.newCall(request).execute();

            String networkResp = response.body().string().replaceAll("&quot;", "'"); // raises exception if first 2 escape chars arent present
            networkResp = StringEscapeUtils.unescapeHtml(networkResp);
            if (!networkResp.isEmpty()) {
                jsonObjectResp = new JSONObject(networkResp);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Crashlytics.log(Log.ERROR, "network request failed", "line 160 SR");
            return null;
        }

        return jsonObjectResp;
    }

}