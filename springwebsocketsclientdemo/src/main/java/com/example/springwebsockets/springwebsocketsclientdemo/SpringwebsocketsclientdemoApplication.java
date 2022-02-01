package com.example.springwebsockets.springwebsocketsclientdemo;

import java.net.URI;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;

import reactor.core.publisher.Mono;


@SpringBootApplication
@RestController
public class SpringwebsocketsclientdemoApplication {

	public static void main(String[] args) throws InterruptedException {
		System.setProperty("spring.profiles.active", "wsclient");
		SpringApplication.run(SpringwebsocketsclientdemoApplication.class, args);
		Thread.sleep(20_000);
	}
	

	@Bean
	WebSocketClient webSocketClient() {
		return new ReactorNettyWebSocketClient();
	}

	/*@GetMapping(value="/wsc/connect")
	public String wsconnect() {
		wsc.execute(URI.create("ws://localhost:8080/ws/echo2"), wsh);
		return "Connected";
	}*/

	@Bean
	CommandLineRunner clr(WebSocketClient wsc , WebSocketHandler wsh)
	{
		return args -> {

			wsc.execute(URI.create("ws://localhost:8080/ws/echo"), wsh).subscribe();

		};

	}


	@Bean
	WebSocketHandler webSocketHandler()
	{
		return session -> {
			WebSocketMessage world = session.textMessage("from another ws client");
			return session.send(Mono.just(world))
			.thenMany(
				session.receive()
				.map(WebSocketMessage::getPayloadAsText)
				.log()).then();


		};
	}
	




}
