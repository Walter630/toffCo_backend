package com.site.toffCo;

import com.site.toffCo.infra.config.EvolutionApiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.site.toffCo")
@EnableConfigurationProperties(EvolutionApiProperties.class) // registra o record de config da Evolution API
public class ToffCoApplication {

	static void main(String[] args) {
		SpringApplication.run(ToffCoApplication.class, args);
	}

}
