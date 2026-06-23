package com.site.toffCo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.site.toffCo") // O JPA vai varrer TUDO
@EnableRedisRepositories(basePackages = "com.site.toffCo.module.whatzap.entity") // O Redis vai olhar SÓ a pasta da entidade do Zap
public class ToffCoApplication {

	static void main(String[] args) {
		SpringApplication.run(ToffCoApplication.class, args);
	}

}
