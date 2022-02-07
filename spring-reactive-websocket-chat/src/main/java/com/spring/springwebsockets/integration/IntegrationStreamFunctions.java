package com.spring.springwebsockets.integration;

import com.spring.springwebsockets.model.ChatMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.FluxMessageChannel;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;
import java.util.function.Supplier;

@Configuration
public class IntegrationStreamFunctions {
    @Autowired
    @Qualifier("rabbitpubfmc")
    FluxMessageChannel rabbitpubfmc;

    @Autowired
    @Qualifier("rabbitsubfmc")
    FluxMessageChannel rabbitsubfmc;

    @Bean
    public Supplier<Flux<Message<?>>> globalchatpubchannel() {
        return () -> {
/*

            Flux.from(rabbitpubfmc)
                    .subscribe(v -> System.out.println("\n\n TMP PUB \n\n"+v));
*/

            return Flux.from(rabbitpubfmc);
        };
    }

    @Bean
    public Consumer<Flux<Message<?>>> globalchatsubchannel() {
        return cFlux -> {

            //cFlux.subscribe(v -> System.out.println("\n\nTMP SUB \n\n"+v));
            cFlux.log().subscribe(m -> rabbitsubfmc.send(m));
            // rabbitsubfmc.subscribeTo(cFlux);
        };
    }
}
