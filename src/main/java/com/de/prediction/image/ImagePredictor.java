/* Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.*/
package com.de.prediction.image;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class ImagePredictor {
    public static void flyBy(double latitude, double longitude, String beginDate, String apiKey) throws Exception {
        try {
            if( apiKey == null) {
                throw new Exception("ERROR: Api Key cannot be null or empty. ");
            }
            String queryString = "?lon=" + longitude +"&lat=" + latitude + "&api_key=" + apiKey;
            if(beginDate != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                Date ret = sdf.parse(beginDate.trim());
                //builder.setParameter("begin", sdf.format(ret));
                queryString = queryString + "&begin=" + sdf.format(ret);
            }
            String url = "https://api.nasa.gov/planetary/earth/assets" + queryString;
            URL nasaURL = new URL(url);

            // Create a trust manager that does not validate certificate chains
            TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return new java.security.cert.X509Certificate[] {};
                }

                public void checkClientTrusted(X509Certificate[] chain,
                                               String authType) throws CertificateException
                {
                }

                public void checkServerTrusted(X509Certificate[] chain,
                                               String authType) throws CertificateException {
                }
            } };

            // Install the all-trusting trust manager
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            
            HttpURLConnection con = (HttpURLConnection) nasaURL.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Accept", "application/json");
            int responseCode = con.getResponseCode();
            if( responseCode != 200) {
                throw new Exception("ERROR: HTTP request returned incorrect status code of " + responseCode);
            }
            //HttpEntity entity = httpResponse.getEntity();
            InputStream instream = con.getInputStream();
            if(instream != null) {
                JSONParser jsonParser = new JSONParser();
                JSONObject jsonObject = (JSONObject)jsonParser.parse(
                      new InputStreamReader(instream, "UTF-8"));
                long count = (long) jsonObject.get("count");
                if(count < 2) {
                    throw new Exception("ERROR: NASA does not have enough data for specified location to make a prediction");
                }
                JSONArray resultsArray = (JSONArray) jsonObject.get("results");
                Date[] dateList= new Date[resultsArray.size()];
                for(int i=0; i<resultsArray.size(); i++) {
                    JSONObject resultObj = (JSONObject) resultsArray.get(i);
                    String dateStr = (String) resultObj.get("date");
                    dateStr = dateStr.replace("T", " ");
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    Date date = dateFormat.parse(dateStr);
                    dateList[i] = date;
                }
                
                Arrays.sort(dateList);
                Long[] deltas = new Long[dateList.length-1];
                for(int i=0; i<dateList.length-1; i++) {
                    long diffInMillies = Math.abs(dateList[i+1].getTime() - dateList[i].getTime());
                    deltas[i] = diffInMillies;
                }
                
                double totalDelta = 0;
                for(int i=0; i<deltas.length; i++) {
                    totalDelta += deltas[i];
                }
                double avgDelta = totalDelta/deltas.length;
                int seconds = (int) (avgDelta/1000);
                
                Calendar nextDay = Calendar.getInstance();
                nextDay.setTimeInMillis((dateList[dateList.length-1]).getTime());
                nextDay.add(Calendar.SECOND, seconds);
                System.out.println("Next time: " + nextDay.getTime());
            }           
        } catch (URISyntaxException | IOException | KeyManagementException | NoSuchAlgorithmException
                | KeyStoreException | ParseException | java.text.ParseException e) {
            System.out.println(e.getMessage());
        }
    }

    public static void main(String args[]) {
        try {
            flyBy(1.5, 100.75, "2017-01-01", "9Jz6tLIeJ0yY9vjbEUWaH9fsXA930J9hspPchute");
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }
}
