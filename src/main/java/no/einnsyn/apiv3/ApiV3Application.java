package no.einnsyn.apiv3;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAutoConfiguration
@SpringBootApplication
@EnableJpaRepositories
@EnableScheduling
@ComponentScan("no.einnsyn")
public class ApiV3Application {

	public static void main(String[] args) {
		SpringApplication.run(ApiV3Application.class, args);
	}

}
