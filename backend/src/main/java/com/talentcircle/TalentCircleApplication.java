package com.talentcircle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class TalentCircleApplication {

    public static void main(String[] args) {
        SpringApplication.run(TalentCircleApplication.class, args);
    }

}
