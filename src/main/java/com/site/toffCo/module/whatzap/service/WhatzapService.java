package com.site.toffCo.module.whatzap.service;

import com.site.toffCo.module.whatzap.dto.SendMessageRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
public class WhatzapService {

    private final RestClient restClient;
    private final String instanceName;

   public WhatzapService(
           @Value("${evolution.api.url}") String baseUrl,
           @Value("${evolution.api.key}") String apiKey,
           @Value("${evolution.api.instance}") String instanceName
   ) {
       log.info("Debug: Whatzap service start: {}", baseUrl);
       log.info("Debug: Whatzap service instance: {}", instanceName);
       this.instanceName = instanceName;
       this.restClient = RestClient.builder()
               .baseUrl(baseUrl)
               .defaultHeader("apikey", apiKey)
               .requestFactory(new org.springframework.http.client.SimpleClientHttpRequestFactory() {{
                   setConnectTimeout(5000);
                   setReadTimeout(5000);
               }})
               .build();
   }

   public void sendMessage(SendMessageRequest request) {
       String url = "/message/sendText/" + this.instanceName;

       log.info("Enviando POST para -> {} ", url);
       log.info("Request -> {}", request.toString());

       restClient.post()
               .uri(url)
               .body(request)
               .retrieve()
               .toBodilessEntity(); // executa o post e descarta a resposta
   }
}
