package com.spring.springwebsockets.sock;

import com.spring.springwebsockets.model.ChatProps;
import com.spring.springwebsockets.security.ChatUserRepository;
import com.spring.springwebsockets.utils.ChatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.file.dsl.Files;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;

import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Sinks.Many;

import java.io.File;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import com.spring.springwebsockets.model.ChatMessage;
import com.spring.springwebsockets.model.MessageTypes;
import com.spring.springwebsockets.security.CustomUserDetailService;
import com.spring.springwebsockets.security.CustomUserDetails;
import com.spring.springwebsockets.security.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class SocksConfig {
    Logger logger = LoggerFactory.getLogger(this.getClass().getName());
    @Autowired
    private ChatProps props;

    private Map<String, FileEventMessageHandler> sessions;

    {
        sessions = new ConcurrentHashMap<>();
    }

    @Bean(name = "wshbean")
    WebSocketHandler wshbean() {

        return session -> {

            Flux<String> payloadFlux = Flux.generate(
                    sink -> sink.next(UUID.randomUUID().toString()));
            Flux<Long> interval = Flux.interval(Duration.ofSeconds(2));
            Flux<String> repeatPayloadFlux = interval.zipWith(payloadFlux,
                    (elapsed, payload) -> payload + " @ " + LocalDateTime.now().toString());

            return session.send(repeatPayloadFlux.map(session::textMessage))
                    .and(session.receive().map(WebSocketMessage::getPayloadAsText).log());

            /*
             * Flux<WebSocketMessage> out = session.receive()
             * .map(WebSocketMessage::getPayloadAsText)
             * .flatMap(textmessage -> Flux.just("Echo :: " + textmessage))
             * .map(session::textMessage);
             * return session.send(out);
             */

        };

    }

    @Bean(name = "wshbean2")
    WebSocketHandler wshbean2(Many<String> globalMessageStream) {

        return session -> {

            session.receive()
                    .map(WebSocketMessage::getPayloadAsText)
                    .subscribe(payload -> globalMessageStream.tryEmitNext(payload));

            Flux<String> globalMessageStreamFlux = globalMessageStream.asFlux();
            return session.send(globalMessageStreamFlux.map(session::textMessage));

        };
    }

    @Bean(name = "wshbean3")
    WebSocketHandler wshbean3(PublishSubscribeChannel psc) {

        return session -> {

            logger.info("-------  session from WS Session : " + session.getId());

            Flux<WebSocketMessage> publisher = Flux.create((Consumer<FluxSink<WebSocketMessage>>) emitter -> {
                sessions.put(session.getId(), new FileEventMessageHandler(session, emitter));
                psc.subscribe(sessions.get(session.getId()));

            }).doFinally(b -> psc.unsubscribe(sessions.get(session.getId())));

            return session.send(publisher);
        };

    }

    @Bean(name = "wshbean4Chat")
    WebSocketHandler wshbean4Chat(Many<ChatMessage> chatSessionStream, ChatUtils utils, ObjectMapper mapper,
                                  ChatUserRepository repo)
            throws JsonProcessingException {

        return session -> {
            logger.info("New Chat Session Initiated : " + session.getId());

            session.receive()
                    .map(wsm -> wsm.getPayloadAsText())
                    .onErrorResume(t-> {
                        session.close();
                        return Mono.error(t);
                    })
                    .map(s -> {
                        ChatMessage cmparsed = null;
                        try {
                            cmparsed = mapper.readValue(s, ChatMessage.class);
                            cmparsed.setTimestamp(utils.getCurrentTimeSamp());
                            logger.info("Received :" + cmparsed);
                            return cmparsed;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return cmparsed;
                    })
                    .map(cmsg -> updateSessionRepo(repo, cmsg))
                    .subscribe(chatmessage -> chatSessionStream.tryEmitNext(chatmessage));

            Flux<WebSocketMessage> sessionOutboundFlux = chatSessionStream.asFlux()
                    .map(cmo -> {
                        try {
                            return mapper.writeValueAsString(cmo);
                        } catch (JsonProcessingException e) {

                            e.printStackTrace();
                        }
                        return "";
                    })
                    .map(session::textMessage)
                    .onErrorResume(t-> {
                        session.close();
                        return Mono.error(t);
                    });

            return session.send(sessionOutboundFlux);

        };

    }

    private ChatMessage updateSessionRepo(ChatUserRepository repo, ChatMessage cmsg) {
        if (cmsg.getType().equals(MessageTypes.LEAVE)) {
            repo.leftChatSession(cmsg.getUsername());
        }

        return cmsg;
    }

    @Bean
    Many<String> globalMessageStream() {
        return Sinks.many().multicast().<String>onBackpressureBuffer();
    }

    @Bean
    Many<ChatMessage> chatMessageStream() {
        return Sinks.many().multicast().<ChatMessage>onBackpressureBuffer();
    }

    @Bean
    PublishSubscribeChannel psc() {
        return new PublishSubscribeChannel();

    }

    @Bean
    IntegrationFlow incomingFileFlow(@Value("file:///${user.home}/temp") File f, PublishSubscribeChannel psc) {

        return IntegrationFlows.from(
                Files.inboundAdapter(f)
                        .autoCreateDirectory(true),
                pspec -> pspec.poller(pm -> pm.fixedDelay(1000)))
                .channel(psc)
                .get();

    }

    @Bean
    ChatUtils chatUtilsBean() {
        return new ChatUtils(props);
    }

    @Bean
    public ReactiveUserDetailsService userDetailsService() {
        return new CustomUserDetailService(createSyntheticUsers(passwordEncoder()),passwordEncoder());
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        PasswordEncoderFactories.createDelegatingPasswordEncoder();
        return new BCryptPasswordEncoder();
    }

    @Bean
    public Map<String, UserDetails> createSyntheticUsers(PasswordEncoder passwordEncoder) {

        User u1 = new User("jhon", passwordEncoder.encode("password"), List.of("USER", "DEVELOPER"));
        User u2 = new User("jane", passwordEncoder.encode("password"), List.of("USER", "DBA"));
        User u3 = new User("jim", passwordEncoder.encode("password"), List.of("USER", "ADMIN"));
        Map<String, UserDetails> users = new HashMap<String, UserDetails>();
        users.put(u1.getUsername(), new CustomUserDetails(u1));
        users.put(u2.getUsername(), new CustomUserDetails(u2));
        users.put(u3.getUsername(), new CustomUserDetails(u3));
        return users;

    }

    @Bean
    public ObjectMapper getObjectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public ChatUserRepository repo() {
        return new ChatUserRepository();
    }

}
