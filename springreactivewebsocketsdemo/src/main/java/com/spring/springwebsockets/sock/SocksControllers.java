package com.spring.springwebsockets.sock;

import java.security.Principal;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import com.spring.springwebsockets.model.ChatMessage;
import com.spring.springwebsockets.model.InitalChatData;
import com.spring.springwebsockets.model.MessageTypes;

import com.spring.springwebsockets.security.ChatUserRepository;
import com.spring.springwebsockets.utils.ChatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;

import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks.Many;

@Controller
@RestController
public class SocksControllers {

    @Autowired
    @Qualifier("wshbean")
    WebSocketHandler wshbean;

    @Autowired
    @Qualifier("wshbean2")
    WebSocketHandler wshbean2;

    @Autowired
    @Qualifier("wshbean3")
    WebSocketHandler wshbean3;

    @Autowired
    @Qualifier("wshbean4Chat")
    WebSocketHandler wshbean4Chat;

    @Autowired
    Many<String> globalMessageStream;

    @Autowired
    Many<ChatMessage> chatMessageStream;

    @Autowired
    ChatUtils chatutils;

    @Autowired
    ChatUserRepository repo;

    Logger logger = LoggerFactory.getLogger(this.getClass().getName());

    @GetMapping("/ws/send/{message}")
    public String sendwsh(@PathVariable String message) {

        // Flux.fromStream(savedSessions.values().stream())
        // .map(wssession -> new WssessionBean(wssession,
        // Flux.just(wssession.textMessage(message))))
        // .flatMap(wsb -> wsb.getWssession().send(wsb.getWsmessage())).subscribe();

        return "done";
    }

    @GetMapping("/ws/send2/{message}")
    public String sendwsh2(@PathVariable String message) {

        globalMessageStream.tryEmitNext(message);

        return "done";
    }

    @GetMapping("/chat/broadcast/{message}")
    public ResponseEntity<String> chatBroadcast(@PathVariable String message) {
        ChatMessage broadcast = new ChatMessage("Admin", message, chatutils.getCurrentTimeSamp(),
                MessageTypes.BROADCAST);
        chatMessageStream.tryEmitNext(broadcast);
        return ResponseEntity.ok().body("Broadcasted Successfully");
    }

    @GetMapping("/rebound")
    public Mono<String> rebound(Mono<Principal> principal) {
        return ReactiveSecurityContextHolder.getContext()
                .map(sc -> sc.getAuthentication())
                .map(auth -> auth.getPrincipal())
                .map(p -> String.format("What is it %s ", ((User) p).getUsername()));
        // return principal.map(p -> String.format("What is is %s ",p.getName()));
    }

    @GetMapping("/chat/initialdata")
    public Mono<InitalChatData> fetchInitialChatData(Mono<Principal> principal) {
        return ReactiveSecurityContextHolder.getContext()
                .map(sc -> sc.getAuthentication())
                .map(auth -> (UserDetails) auth.getPrincipal())
                .flatMap(ud -> repo.newChatSession(ud))
                .map(p -> p.getUsername())
                .map(username -> new InitalChatData(username, repo.getActiveUsers(), UUID.randomUUID().toString()));

    }

    @Value("classpath:/chat.html")
    Resource html;

    @PostMapping("/login")
    public Mono<ResponseEntity<Resource>> login(ServerWebExchange exchange, Authentication authentication) {
        UserDetails principal = (UserDetails) authentication.getPrincipal();
        logger.info(principal.getUsername() + " logged in at " + new Date());

        return Mono
                .just(principal)
                .flatMap(p -> repo.newChatSession(principal))
                .map(p -> p.getUsername())
                .map(
                        username -> ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, "chat_user_cookie=" + username)
                                .header("chat_user_header", username)
                                .contentType(MediaType.TEXT_HTML)
                                .body(html));
    }

    @GetMapping("/chat.html")
    public Mono<ResponseEntity<Resource>> chatpage(ServerWebExchange exchange, Authentication authentication) {
        return login(exchange, authentication);
    }
    @GetMapping("/")
    public Mono<ResponseEntity<Resource>> defaultroot(ServerWebExchange exchange, Authentication authentication) {
        return login(exchange, authentication);
    }

    @Bean
    SimpleUrlHandlerMapping getDefwsh1() {
        return new SimpleUrlHandlerMapping(Map.of("/ws/echo", wshbean), 10);
    }

    @Bean
    SimpleUrlHandlerMapping getDefwsh2() {

        return new SimpleUrlHandlerMapping(Map.of("/ws/echo2", wshbean2), 10);
    }

    @Bean
    SimpleUrlHandlerMapping getDefwsh3() {

        return new SimpleUrlHandlerMapping(Map.of("/ws/echo3", wshbean3), 10);
    }

    @Bean
    SimpleUrlHandlerMapping getDefwsh4() {

        return new SimpleUrlHandlerMapping(Map.of("/ws/chat", wshbean4Chat), 10);
    }

}
