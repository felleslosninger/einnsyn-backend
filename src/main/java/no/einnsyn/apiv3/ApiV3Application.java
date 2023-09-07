package no.einnsyn.apiv3;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableAutoConfiguration
public class ApiV3Application {

	public static void main(String[] args) {
		SpringApplication.run(ApiV3Application.class, args);
	}

}
