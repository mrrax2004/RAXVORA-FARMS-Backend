package com.dairyfarm.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeConfig {
    @Bean
    Clock farmClock(@Value("${app.farm.time-zone}") String timeZone) {
        return Clock.system(ZoneId.of(timeZone));
    }
}
