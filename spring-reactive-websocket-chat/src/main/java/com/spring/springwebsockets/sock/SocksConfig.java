package com.spring.springwebsockets.sock;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring.springwebsockets.model.ChatMessage;
import com.spring.springwebsockets.model.ChatProps;
import com.spring.springwebsockets.model.MessageTypes;
import com.spring.springwebsockets.security.ChatUserRepository;
import com.spring.springwebsockets.security.CustomUserDetailService;
import com.spring.springwebsockets.security.CustomUserDetails;
import com.spring.springwebsockets.security.User;
import com.spring.springwebsockets.utils.ChatUtils;
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

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.FluxMessageChannel;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlowBuilder;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.StandardIntegrationFlow;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Sinks.Many;

@Configuration
public class SocksConfig {

  Logger logger = LoggerFactory.getLogger(this.getClass().getName());

  @Autowired
  private ChatProps props;
    @Autowired
    private IntegrationFlowContext integrationFlowContext;

  @Bean(name = "wshbean5Chat")
  WebSocketHandler wshbean5Chat(
     ChatUtils utils,
     @Qualifier("inboundfmc") FluxMessageChannel fmcBeanIN,
          ObjectMapper mapper,
          ChatUserRepository repo
  )
    throws JsonProcessingException {
    return session -> {
       logger.info("New Chat Session Initiated : " + session.getId());

        Flux<Message<ChatMessage>> incomingFlux =
                session.receive()
                .map(wsm -> wsm.getPayloadAsText())
                .onErrorResume(
                        t -> {
                            logger.info("Chat Session Closed on Error: " + t);
                            session.close();
                            return Mono.error(t);
                        }
                )
                .map(
                        s -> {
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
                        }
                )
                .map(cmsg -> updateSessionRepo(repo, cmsg))
                .map(cmsg -> MessageBuilder.withPayload(cmsg).setHeader("webSocketSession", session).build())

                .doOnComplete(
                        () -> {
                            logger.info("Chat Session completed " + session.getId());
                            // session.close(); // No need?
                        }
                );

        StandardIntegrationFlow standardIntegrationFlow = IntegrationFlows.from(incomingFlux).channel(fmcBeanIN).get();
        if(integrationFlowContext.getRegistrationById("globalChatIntegration") == null)
        {
             integrationFlowContext.registration(standardIntegrationFlow).register();
        }


        Flux<WebSocketMessage> sessionOutboundFlux = Flux.from(fmcBeanIN)
              .map(cmsg -> (ChatMessage)cmsg.getPayload())
        .map(
          cmo1 -> {
            if (cmo1.getType().equals(MessageTypes.LEAVE)) {
              cmo1.setMessage("Left");
            }
            return cmo1;
          }
        )
        .map(
          cmo -> {
            try {
              return mapper.writeValueAsString(cmo);
            } catch (JsonProcessingException e) {
              e.printStackTrace();
            }
            return "";
          }
        )
        .map(session::textMessage)
        .onErrorResume(
          t -> {
            logger.info(
              t.getMessage() + "::Chat Session Closed : " + session.getId()
            );
            session.close();
            return Mono.error(t);
          }
        );

      Mono<Void> outbound = session.send(sessionOutboundFlux);
      return outbound;
    };
  }


  private ChatMessage updateSessionRepo(
    ChatUserRepository repo,
    ChatMessage cmsg
  ) {
    if (cmsg.getType().equals(MessageTypes.LEAVE)) {
      repo.leftChatSession(cmsg.getUsername());
    }
      System.out.println("Session Repo Updated for "+cmsg);
    return cmsg;
  }


  @Bean
  ChatUtils chatUtilsBean() {
    return new ChatUtils(props);
  }
}
