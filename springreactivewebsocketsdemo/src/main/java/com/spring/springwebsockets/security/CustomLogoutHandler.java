package com.spring.springwebsockets.security;

import com.spring.springwebsockets.utils.ChatUtils;
import com.spring.springwebsockets.model.ChatMessage;
import com.spring.springwebsockets.model.MessageTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.logout.ServerLogoutHandler;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.Date;

@Service
public class CustomLogoutHandler implements ServerLogoutHandler {

    Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    private ChatUserRepository repo;
    private ChatUtils chatUtils;
    private Sinks.Many<ChatMessage> chatMessageStream;
    public CustomLogoutHandler(ChatUserRepository repo, ChatUtils chatUtils,
    Sinks.Many<ChatMessage> chatMessageStream)
    {
        this.repo =  repo;
        this.chatUtils = chatUtils;
        this.chatMessageStream = chatMessageStream;
    }


    @Override
    public Mono<Void> logout(WebFilterExchange exchange, Authentication authentication) {
        Object principal = authentication.getPrincipal();
        logger.info("Principal at logout "+principal);
        if(principal instanceof UserDetails) {
            String username = ((UserDetails) authentication.getPrincipal()).getUsername();
            String msg = "Logging Out : " + username;
            Mono<Void> leftVoid = repo.leftChatSession(username);
            ChatMessage broadcast = new ChatMessage(username, msg, chatUtils.getCurrentTimeSamp(),
                    MessageTypes.LEAVE);
            chatMessageStream.tryEmitNext(broadcast);
            logger.info(username + " logged off at " + new Date());

            return leftVoid;
        }

        return Mono.empty().then();


    }
}
