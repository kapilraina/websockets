package com.example.springstompwebsockstsdemo.controller;

import java.time.Instant;
import java.util.Collection;

import com.example.springstompwebsockstsdemo.model.ChatMessage;
import com.example.springstompwebsockstsdemo.model.LoginEvent;
import com.example.springstompwebsockstsdemo.repository.ActiveSessionsRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

@Controller
public class SocketController {
    Logger logger = LoggerFactory.getLogger(SocketController.class);

    @Autowired
    ActiveSessionsRepository participantRepository;

    @MessageMapping("/sendchat")
    @SendTo("/topic/publicchat")
    public ChatMessage chatSend(ChatMessage message) {
        message.setTimestamp(Instant.now().toString());
        logger.info("broadcasting : " + message);
        return message;
    }

    @SubscribeMapping("/chat.participants")
    public Collection<LoginEvent> retrieveParticipants() {

        Collection<LoginEvent> logins = participantRepository.getActiveSessions().values();
        logger.info("chat.participants : " + logins);
        return logins;
    }

}
