package no.einnsyn.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableJpaRepositories
@EnableAsync
@EnableRetry
@ComponentScan("no.einnsyn")
public class ApiV3Application {

  public static void main(String[] args) {
    SpringApplication.run(ApiV3Application.class, args);
  }
}
