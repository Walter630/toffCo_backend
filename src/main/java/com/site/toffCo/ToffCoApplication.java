package com.site.toffCo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.site.toffCo") // O JPA vai varrer TUDO
public class ToffCoApplication {

	static void main(String[] args) {
		SpringApplication.run(ToffCoApplication.class, args);
	}

}
