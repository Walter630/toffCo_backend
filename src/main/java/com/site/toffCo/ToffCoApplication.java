package com.site.toffCo;

import com.site.toffCo.infra.config.EvolutionApiProperties;
import com.site.toffCo.infra.config.WhatsappProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.validation.annotation.Validated;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.site.toffCo")
@EnableConfigurationProperties({EvolutionApiProperties.class, WhatsappProperties.class})
@Validated
public class ToffCoApplication {

	static void main(String[] args) {
		SpringApplication.run(ToffCoApplication.class, args);
	}

}
