package com.calmio.calm.integration.Helpers;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.net.ssl.*;
import javax.xml.bind.DatatypeConverter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import hudson.model.BuildListener;


public class Calm {

    private static final String USER_AGENT = "Mozilla/5.0";
    private static final String POST_PARAMS = "{\"length\":250}";
    private final String PCVM_IP, USERNAME, PASSWORD, BP_NAME, APP_NAME, APP_PROFILE_NAME ;

    public Calm(String pcvmip, String username, String pwd, String bpname, String appname, String profname) {
        PCVM_IP = pcvmip;
        USERNAME = username;
        PASSWORD = pwd;
        BP_NAME = bpname;
        APP_NAME = appname;
        APP_PROFILE_NAME = profname;
    }

    public void doTrustToCertificates() throws Exception {
        Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkServerTrusted(X509Certificate[] certs, String authType) throws CertificateException {
                        return;
                    }

                    public void checkClientTrusted(X509Certificate[] certs, String authType) throws CertificateException {
                        return;
                    }
                }
        };

        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        HostnameVerifier hv = new HostnameVerifier() {

            public boolean verify(String urlHostName, SSLSession session) {
                if (!urlHostName.equals(session.getPeerHost())) {
                    System.out.println("Warning: URL host '" + urlHostName + "' is different to SSLSession host '" + session.getPeerHost() + "'.");
                }
                return true;
            }
        };
        HttpsURLConnection.setDefaultHostnameVerifier(hv);
    }

    // connecting to URL
    public void launchBlueprint() throws Exception {
        doTrustToCertificates();//
        URL url = new URL("https://"+this.PCVM_IP+":9440/api/nutanix/v3/blueprints/list");
        String encoding = DatatypeConverter.printBase64Binary((this.USERNAME+":"+this.PASSWORD).getBytes());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Authorization", "Basic " + encoding);
        conn.setRequestProperty("User-Agent", USER_AGENT);
        conn.setRequestProperty("Content-Type", "application/json");


        // For POST only - START
        conn.setDoOutput(true);
        OutputStream os = conn.getOutputStream();
        os.write(POST_PARAMS.getBytes());
        os.flush();
        os.close();
        // For POST only - END

        int responseCode = conn.getResponseCode();
        BufferedReader in =
                new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inputLine;
        String bpId = null;
        StringBuffer response = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }

        JSONObject outerObject = new JSONObject(response.toString());
        JSONArray entities = outerObject.getJSONArray("entities");
        for (int i = 0; i < entities.length(); i++) {
            JSONObject entity = entities.getJSONObject(i);
            String entityName = entity.getJSONObject("status").getString("name");
            if (entityName.equals(this.BP_NAME)) {
                bpId = entity.getJSONObject("status").getString("uuid");
                break;
            }
        }

        if (bpId == null) {
            System.out.println("Blueprint with name " + this.BP_NAME + " not found in list");
            throw new Exception("Blueprint with name " + this.BP_NAME + " not found in list");
        }

        url = new URL("https://"+this.PCVM_IP+":9440/api/nutanix/v3/blueprints/" + bpId);
        encoding = DatatypeConverter.printBase64Binary((this.USERNAME+":"+this.PASSWORD).getBytes());
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Authorization", "Basic " + encoding);
        con.setRequestProperty("User-Agent", USER_AGENT);
        con.setRequestProperty("Content-Type", "application/json");
        int responseCod = con.getResponseCode();
        in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        response = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        JSONObject launchBp = new JSONObject(response.toString());
        long time = (System.currentTimeMillis() / 1000L);
        String appname = this.APP_NAME + "_" + time;
        launchBp.remove("status");
        launchBp.getJSONObject("spec").remove("name");
        launchBp.getJSONObject("spec").put("application_name", appname);
        String appProfileUuid = null;
        // get app profile refrence from name
        JSONArray appProfileList = launchBp.getJSONObject("spec").getJSONObject("resources").getJSONArray("app_profile_list");
        for (int i = 0; i < appProfileList.length(); i++) {
            JSONObject appProfile = appProfileList.getJSONObject(i);
            if (appProfile.getString("name").equals(this.APP_PROFILE_NAME)) {
                appProfileUuid = appProfile.getString("uuid");
                break;
            }
        }

        if (appProfileUuid == null) {
            throw new Exception("App profile with name " + this.APP_PROFILE_NAME + " not found in list");
        }
        JSONObject appProfileReference = new JSONObject();
        appProfileReference.put("kind", "app_profile");
        appProfileReference.put("uuid", appProfileUuid);
        launchBp.getJSONObject("spec").put("app_profile_reference", appProfileReference);

        url = new URL("https://"+this.PCVM_IP+":9440/api/nutanix/v3/blueprints/" + bpId + "/launch");
        encoding = DatatypeConverter.printBase64Binary((this.USERNAME+":"+this.PASSWORD).getBytes());
        conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Authorization", "Basic " + encoding);
        conn.setRequestProperty("User-Agent", USER_AGENT);
        conn.setRequestProperty("Content-Type", "application/json");
        // For POST only - START
        conn.setDoOutput(true);
        os = conn.getOutputStream();
        os.write(launchBp.toString().getBytes());
        os.flush();
        os.close();
        in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        response = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }

        if (responseCode == 200) {
            System.out.println("Blueprint " + this.BP_NAME + "has been lanched sucessfully with application name " + appname + "with profile" + this.APP_PROFILE_NAME);
		

        }
        else{
            throw new Exception("Blueprint launch failed" + response.toString());
        }
    }
}
