package com.example.springstompwebsockstsdemo.config;

import com.example.springstompwebsockstsdemo.repository.ActiveSessionsRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Socketconfigs {

    @Bean
    ActiveSessionsRepository activeSessionsRepository() {
        return new ActiveSessionsRepository();
    }

}
