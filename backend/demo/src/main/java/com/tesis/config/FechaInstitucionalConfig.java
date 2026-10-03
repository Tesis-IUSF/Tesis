package com.tesis.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class FechaInstitucionalConfig {

    public static final ZoneId ZONA_INSTITUCIONAL = ZoneId.of("America/Caracas");

    @Bean
    public Clock clockInstitucional() {
        return Clock.system(ZONA_INSTITUCIONAL);
    }
}