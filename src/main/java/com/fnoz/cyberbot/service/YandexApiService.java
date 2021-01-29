package com.fnoz.cyberbot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class YandexApiService {

    private final String diskFolderPath = "disk:/User memes archive/";

    private final ObjectMapper mapper = new ObjectMapper();
    private final CloseableHttpClient httpClient = HttpClients.createDefault();
    private final String token;

    public YandexApiService(String token) {
        this.token = token;
    }

    public void uploadFile(String localFilePath, String cloudFileName) throws Exception {
        Map<String, String> queryParams = new HashMap<String, String>() {{
            put("path", diskFolderPath + cloudFileName);
        }};
        String json = sendGet("https://cloud-api.yandex.net/v1/disk/resources/upload", queryParams);

        HttpPut httpPut = new HttpPut(getUrlForUpload(json));

        httpPut.setEntity(EntityBuilder.create().setFile(new File(localFilePath)).build());

        try (CloseableHttpClient httpClient = HttpClients.createDefault();
             CloseableHttpResponse response = httpClient.execute(httpPut)) {
            System.out.println(EntityUtils.toString(response.getEntity()));
        }
    }

    private String sendGet(String url, Map<String, String> params) throws Exception {

        URIBuilder builder = new URIBuilder(url);
        params.forEach(builder::setParameter);

        HttpGet request = new HttpGet(builder.build());

        request.addHeader("Authorization", "OAuth " + token);
        request.addHeader("Accept", "application/json");

        try (CloseableHttpResponse response = httpClient.execute(request)) {
            HttpEntity entity = response.getEntity();
            return EntityUtils.toString(entity);
        }
    }

    private String getUrlForUpload(String json) throws IOException {
        JsonNode jsonNode = mapper.readTree(json);
        return jsonNode.get("href").textValue();
    }

    private void close() throws IOException {
        httpClient.close();
    }
}
