package com.spring.springwebsockets.sock;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.FluxSink;

import java.io.File;

public class FileEventMessageHandler implements MessageHandler {

    private WebSocketSession wsSession;
    private FluxSink<WebSocketMessage> wsMessageFlux;

    FileEventMessageHandler(WebSocketSession wsSession, FluxSink<WebSocketMessage> wsMessageFlux)
    {
        this.wsSession = wsSession;
        this.wsMessageFlux = wsMessageFlux;
    }

    @Override
    public void handleMessage(Message<?> message) throws MessagingException {
        File fPayload = (File)message.getPayload();
        String payload = wsSession.getId() + " -> " + fPayload.getAbsolutePath();
        wsMessageFlux.next(wsSession.textMessage(payload));

    }
}
