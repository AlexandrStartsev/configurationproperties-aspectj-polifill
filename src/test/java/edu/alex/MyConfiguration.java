package edu.alex;

import java.util.Properties;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import lombok.Data;

@Configuration
@PropertySource("classpath:application.properties")
public class MyConfiguration {
	
	@Bean
	@ConfigurationProperties(prefix = "some")
	Properties getSomeProperties() {
		return new Properties();
	}
	
	@Bean
	Properties getSomeOtherProperties() {
		return new Properties();
	}	
	
	public static @Data class Int {
		int two;
	}

	
	@Bean
	@ConfigurationProperties(prefix = "int")
	Int getSomePropertiesWithParam(ApplicationContext ctx) {
		return new Int();
	}
	
	String verify() {
		return getSomeProperties().getProperty("property");
	}	
}