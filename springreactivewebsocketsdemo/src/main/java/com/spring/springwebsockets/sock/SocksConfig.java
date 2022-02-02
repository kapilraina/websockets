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
import java.util.stream.Collectors;

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

           // logger.info("New Chat Session Initiated : " + session.getId());

            Mono<Void> inbound  = session.receive()
                    .map(wsm -> wsm.getPayloadAsText())
                    .onErrorResume(t -> {
                        logger.info("Chat Session Closed on Error: " + session.getId());
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
                    /*
                     * .flatMap(chatMessage -> {
                     * // Needed?
                     * if (chatMessage.getType().equals(MessageTypes.LEAVE)) {
                     * logger.info("Chat Session Closed on LEAVE message: " + session.getId());
                     * session.close();
                     * }
                     * return Mono.just(chatMessage);
                     * })
                     */
                    .doOnComplete(() -> {
                        logger.info("Chat Session completed " + session.getId());
                        // session.close(); // No need?
                    })
                    // .log()
                    .doOnNext(chatSessionStream::tryEmitNext)
                    .then();

            Flux<WebSocketMessage> sessionOutboundFlux = chatSessionStream.asFlux()
                    .map(cmo1 -> {
                        if (cmo1.getType().equals(MessageTypes.LEAVE)) {
                            cmo1.setMessage("Left");
                        }
                        return cmo1;
                    })
                    .map(cmo -> {
                        try {
                            return mapper.writeValueAsString(cmo);
                        } catch (JsonProcessingException e) {

                            e.printStackTrace();
                        }
                        return "";
                    })
                    .map(session::textMessage)
                    .onErrorResume(t -> {
                        logger.info(t.getMessage() + "::Chat Session Closed : " + session.getId());
                        session.close();
                        return Mono.error(t);
                    });

                    Mono<Void> outbound = session.send(sessionOutboundFlux);
                    return Mono.zip(inbound, outbound).then();

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
        return new CustomUserDetailService(createSyntheticUsers(passwordEncoder()), passwordEncoder());
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        PasswordEncoderFactories.createDelegatingPasswordEncoder();
        return new BCryptPasswordEncoder();
    }

    @Bean
    public Map<String, UserDetails> createSyntheticUsers(PasswordEncoder passwordEncoder) {

        return List.of(
                new User("jim", passwordEncoder.encode("password"), List.of("USER", "SALES")),
                new User("pam", passwordEncoder.encode("password"), List.of("USER", "ADMIN")),
                new User("dwight", passwordEncoder.encode("password"), List.of("USER", "SALES", "DBA")),
                new User("michael", passwordEncoder.encode("password"), List.of("USER", "MANAGER")),
                new User("oscar", passwordEncoder.encode("password"), List.of("USER", "ACCOUNTANT")),
                new User("angela", passwordEncoder.encode("password"), List.of("USER", "ACCOUNTANT")),
                new User("kevin", passwordEncoder.encode("password"), List.of("USER", "KELVIN")),
                new User("stanley", passwordEncoder.encode("password"), List.of("USER", "SALES")),
                new User("phyllis", passwordEncoder.encode("password"), List.of("USER", "SALES")),
                new User("creed", passwordEncoder.encode("password"), List.of("USER", "QA")))
                .stream()
                .map(u -> new CustomUserDetails(u))
                .collect(Collectors.toMap(
                        CustomUserDetails::getUsername,
                        cud -> cud));
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
