package com.spring.springwebsockets.integration;

import com.spring.springwebsockets.model.ChatMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.FluxMessageChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.messaging.MessageChannel;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Sinks.Many;

@Configuration
public class IntegrationConfigs {

    @Bean
    Many<ChatMessage> chatMessageStream() {
        return Sinks.many().multicast().<ChatMessage>onBackpressureBuffer();
    }

    @Bean
    @Qualifier("inboundfmc")
    FluxMessageChannel fmcBeanIN() {

        FluxMessageChannel fmc = new FluxMessageChannel();
        //fmc.setManagedName("chatMessageStreamChannel");
        return fmc;
    }


    @Bean
    @Qualifier("outboundfmc")
    FluxMessageChannel fmcBeanOut() {

        FluxMessageChannel fmc = new FluxMessageChannel();
        //fmc.setManagedName("chatMessageStreamChannel");
        return fmc;
    }

    @Bean
    @Qualifier("pubsubfmc")
    FluxMessageChannel pubsubBean() {

        FluxMessageChannel fmc = new FluxMessageChannel();
        //fmc.setManagedName("chatMessageStreamChannel");
        return fmc;
    }

    @Bean
    @Qualifier("rabbitpubfmc")
    FluxMessageChannel rabbitpubfmcBean() {

        FluxMessageChannel fmc = new FluxMessageChannel();
        return fmc;
    }

    @Bean
    @Qualifier("rabbitsubfmc")
    FluxMessageChannel rabbitsubfmcBean() {

        FluxMessageChannel fmc = new FluxMessageChannel();
        return fmc;
    }

    @Bean
    IntegrationFlow fluxItegration(@Qualifier("inboundfmc") FluxMessageChannel fmcin, @Qualifier("outboundfmc") FluxMessageChannel fmcout) {
        return IntegrationFlows.from(((MessageChannel) fmcin))
                .channel(fmcout).get();


    }

    @Bean
    IntegrationFlow fluxItegration(@Qualifier("rabbitpubfmc") FluxMessageChannel rabbitpubfmc) {
        return IntegrationFlows.from(((MessageChannel) rabbitpubfmc)).bridge().nullChannel();
    }


    @Bean
    IntegrationFlow fluxItegration2(@Qualifier("rabbitsubfmc") FluxMessageChannel rabbitsubfmc) {
        return IntegrationFlows.from(((MessageChannel) rabbitsubfmc)).bridge().nullChannel();
    }
}
