package com.cocotalk.push.service;

import com.cocotalk.push.dto.push.FCMMessage;
import com.cocotalk.push.support.PushException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.common.net.HttpHeaders;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.List;

import static com.cocotalk.push.dto.common.response.ResponseStatus.BAD_REQUEST;
import static com.cocotalk.push.dto.common.response.ResponseStatus.SUBSCRIBE_ERROR;

@Service
@RequiredArgsConstructor
@Slf4j
public class FCMService {

    @Value("${fcm.api-url}")
    private String apiUrl;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;


    // 파라미터를 FCM이 요구하는 body 형태로 만들어준다.
    private String makeMessageStr(String targetToken, String title, String body) throws JsonProcessingException {
        FCMMessage fcmMessage = FCMMessage.builder()
                .message(FCMMessage.Message.builder()
                        .token(targetToken)
                        .notification(FCMMessage.Notification.builder()
                                .title(title)
                                .body(body)
                                .image(null)
                                .build()
                        )
                        .build()
                )
                .validate_only(false)
                .build();
        return objectMapper.writeValueAsString(fcmMessage);
    }

    private FCMMessage makeMessage(String targetToken, String title, String body) {
        FCMMessage fcmMessage = FCMMessage.builder()
                .message(FCMMessage.Message.builder()
                        .token(targetToken)
                        .notification(FCMMessage.Notification.builder()
                                .title(title)
                                .body(body)
                                .image(null)
                                .build()
                        )
                        .build()
                )
                .validate_only(false)
                .build();
        return fcmMessage;
    }

    public void sendByToken(String targetToken, String title, String body) throws IOException {
        String message = makeMessageStr(targetToken, title, body);

        OkHttpClient client = new OkHttpClient();
        RequestBody requestBody = RequestBody.create(message, MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(apiUrl)
                .post(requestBody)
                .addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + getAccessToken())
                .addHeader(HttpHeaders.CONTENT_TYPE, "application/json; UTF-8")
                .build();

        Response response = client.newCall(request).execute();

        log.info(response.body().string());
    }


    public void sendByTokenList(List<String> targetTokens, String title, String body) throws IOException {
        String authorization = "Bearer " + getAccessToken();
        long startTime = System.currentTimeMillis();

        Flux<FCMMessage> fcmMessageFlux = Flux.fromIterable(targetTokens)
                .map(targetToken -> makeMessage(targetToken, title, body))
                .doOnError((e) ->{
                    System.err.println("Error : " + e.getMessage());
                    throw new PushException(BAD_REQUEST, e);
                });

        fcmMessageFlux.subscribe(fcmTokenMessage -> webClient
                        .post()
                        .uri(apiUrl)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .accept(org.springframework.http.MediaType.APPLICATION_JSON)
                        .header(org.springframework.http.HttpHeaders.AUTHORIZATION, authorization)
                        .bodyValue(fcmTokenMessage)
                        .retrieve()
                        .bodyToFlux(String.class)
                        .subscribe(
                                res -> log.info("{}", res),
                                (e) -> {throw new PushException(SUBSCRIBE_ERROR,e);}
                        )
        );

        log.info("end: " + (System.currentTimeMillis() - startTime) + "sec");
    }


    private String getAccessToken() throws IOException {
        String firebaseConfigPath = "firebase/cocotalk_firebase_service_key.json";
        GoogleCredentials googleCredentials = GoogleCredentials
                .fromStream(new ClassPathResource(firebaseConfigPath).getInputStream())
                .createScoped(List.of("https://www.googleapis.com/auth/cloud-platform"));
        googleCredentials.refreshIfExpired();
        return googleCredentials.getAccessToken().getTokenValue();
    }
}
