package com.smartpulse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;   //JSON verilerini java nesnelerine çevirmek için kullanılır
													
													 // HTTP istekleri oluşturur
import org.apache.hc.client5.http.classic.methods.HttpPost;  
import org.apache.hc.client5.http.classic.methods.HttpGet;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;    //HTTP istemcisi
import org.apache.hc.client5.http.impl.classic.HttpClients;

import org.apache.hc.core5.http.io.entity.StringEntity;   //gönderilecek 	 HTTP body sini temsil eder

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class EpiasClient {

	private final String username;
    private final String password;
    private String cachedTGT;
    private long cacheTimestamp;
    private final ObjectMapper mapper = new ObjectMapper();

    public EpiasClient(String username, String password) {
        this.username = username;
        this.password = password;
    }

    private String getTGT() throws IOException {
        if (cachedTGT != null && (System.currentTimeMillis() - cacheTimestamp) < 2 * 60 * 60 * 1000) {
            return cachedTGT;
        }

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost("https://giris.epias.com.tr/cas/v1/tickets");
            post.setHeader("Content-Type", "application/x-www-form-urlencoded");
            String body = "username=" + username + "&password=" + password;
            post.setEntity(new StringEntity(body));

            String response = new String(client.execute(post).getEntity().getContent().readAllBytes(),
                    StandardCharsets.UTF_8);
            String tgtUrl = response.split("action=\"")[1].split("\"")[0];
            cachedTGT = tgtUrl.substring(tgtUrl.lastIndexOf("/") + 1);
            cacheTimestamp = System.currentTimeMillis();
            return cachedTGT;
        }
    }

    public List<TransactionHistoryGipDataDto> getTransactionData() throws IOException {
        String tgt = getTGT();

        String startDate = "2025-08-27T00:00:00+03:00";
        String endDate = "2025-08-28T00:00:00+03:00";

        String url = String.format(
                "https://seffaflik.epias.com.tr/electricity-service/v1/markets/idm/data/transaction-history?startDate=%s&endDate=%s",
                startDate, endDate);

        HttpGet get = new HttpGet(url);
        get.setHeader("Authorization", "Bearer " + tgt);

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            String json = new String(client.execute(get).getEntity().getContent().readAllBytes(),
                    StandardCharsets.UTF_8);
            
            System.out.println("Gelen JSON verisi:");
            System.out.println(json);
            
            JsonNode root = mapper.readTree(json);
            JsonNode dataList = root.path("body").path("transactionHistoryGipList");
            System.out.println("Gelen veri listesi: " + dataList);

            List<TransactionHistoryGipDataDto> result = new ArrayList<>();
            for (JsonNode item : dataList) {
                TransactionHistoryGipDataDto dto = mapper.treeToValue(item, TransactionHistoryGipDataDto.class);
                result.add(dto);
            }

            return result;
        }
    }

   
    public String getCachedTGT() {
        return cachedTGT;
    }

}
