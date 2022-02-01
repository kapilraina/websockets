package com.spring.springwebsockets.model;

import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import reactor.core.publisher.Flux;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class WssessionBean {

    private WebSocketSession wssession;
    private Flux<WebSocketMessage> wsmessage;

    
}
