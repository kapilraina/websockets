package com.spring.springwebsockets;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.socket.WebSocketSession;

import reactor.core.publisher.Sinks;
import reactor.core.publisher.Sinks.Many;

@SpringBootApplication
@RestController
public class SpringwebsocketsdemoApplication {
	Logger logger = LoggerFactory.getLogger(this.getClass().getName());
	private Map<String, WebSocketSession> savedSessions = new HashMap<String, WebSocketSession>();

	public static void main(String[] args) {
		System.setProperty("spring.profiles.active", "wsserver");
		SpringApplication.run(SpringwebsocketsdemoApplication.class, args);
	}

	/**
	 * low latency, high frequency, and high volume that make the best case for the
	 * use of WebSocket.
	 * 
	 * @return
	 */

	@Bean
	CommandLineRunner a__run() {
		return args -> {
			Many<String> manys = Sinks.many().multicast().<String>onBackpressureBuffer();

			manys.asFlux().map(s -> "Proc 1 : " + s).subscribe(logger::info);
			manys.asFlux().map(s -> "Proc 2 : " + s).subscribe(logger::info);
			manys.asFlux().map(s -> "Proc 3 : " + s).subscribe(logger::info);

			Arrays.asList("1").stream()
					.forEach(n -> logger.info("Subscribers = :" + manys.currentSubscriberCount()));

			manys.tryEmitNext("brown");
			manys.tryEmitNext("fox");
			manys.tryEmitNext("sleeps");
			manys.tryEmitComplete();

		};
	}

}
