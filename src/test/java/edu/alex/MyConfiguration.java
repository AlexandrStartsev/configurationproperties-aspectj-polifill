package edu.alex;

import java.util.Properties;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:application.properties")
public class MyConfiguration {
	
	@Bean
	@ConfigurationProperties(prefix = "some")
	Properties getSomeProperties() {
		return new Properties();
	}
	
	String verify() {
		return getSomeProperties().getProperty("property");
	}	
}