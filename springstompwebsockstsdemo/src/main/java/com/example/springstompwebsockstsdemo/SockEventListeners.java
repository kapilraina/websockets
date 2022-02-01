package com.example.springstompwebsockstsdemo;

import java.util.Date;
import java.util.Optional;

import com.example.springstompwebsockstsdemo.model.LoginEvent;
import com.example.springstompwebsockstsdemo.model.LogoutEvent;
import com.example.springstompwebsockstsdemo.repository.ActiveSessionsRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class SockEventListeners {

    Logger logger = LoggerFactory.getLogger(SockEventListeners.class);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ActiveSessionsRepository participantRepository;

    @EventListener
    private void handleSessionConnected(SessionConnectEvent event) {
        logger.info("Connection Event " + event);
        logger.info("Connection Event Headers : ");

        event.getMessage().getHeaders().entrySet()
                .stream()
                .forEach(entry -> System.out.println(entry.getKey() + ":" + entry.getValue()));

        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(event.getMessage());
        // String username = headers.getUser().getName();
        String username = (String) event.getMessage().getHeaders().get("simpSessionId");

        LoginEvent loginEvent = new LoginEvent(username, new Date());
        logger.info("New Session : " + loginEvent);
        messagingTemplate.convertAndSend("/topic/chat.login", loginEvent);

        // We store the session as we need to be idempotent in the disconnect event
        // processing
        participantRepository.add(headers.getSessionId(), loginEvent);
    }

    @EventListener
    private void handleSessionDisconnect(SessionDisconnectEvent event) {

        Optional.ofNullable(participantRepository.getParticipant(event.getSessionId()))
                .ifPresent(login -> {
                    logger.info("Session Disconnected ::" + login.getUsername());
                    messagingTemplate.convertAndSend("/topic/chat.logout", new LogoutEvent(login.getUsername()));
                    participantRepository.removeParticipant(event.getSessionId());
                });
    }

}
