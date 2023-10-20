package no.einnsyn.apiv3.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Configuration
public class EmailConfiguration {

  @Value("${spring.mail.host:localhost}")
  private String mailServerHost;

  @Value("${spring.mail.port:25}")
  private Integer mailServerPort;

  @Value("${spring.mail.username}")
  private String mailServerUsername;

  @Value("${spring.mail.password}")
  private String mailServerPassword;

  @Value("${spring.mail.properties.mail.smtp.protocol}")
  private String mailServerProtocol;

  @Value("${spring.mail.properties.mail.smtp.auth:false}")
  private String mailServerAuth;

  @Value("${spring.mail.properties.mail.smtp.starttls.enable:false}")
  private String mailServerStartTls;

  @Value("${spring.mail.properties.mail.debug:false}")
  private String debug;


  @Bean
  JavaMailSender getJavaMailSender() {
    var mailSender = new JavaMailSenderImpl();

    mailSender.setHost(mailServerHost);
    mailSender.setPort(mailServerPort);
    mailSender.setUsername(mailServerUsername);
    mailSender.setPassword(mailServerPassword);

    var props = mailSender.getJavaMailProperties();
    props.put("mail.transport.protocol", mailServerProtocol);
    props.put("mail.smtp.auth", mailServerAuth);
    props.put("mail.smtp.starttls.enable", mailServerStartTls);
    props.put("mail.debug", debug);

    return mailSender;
  }

}
