package edu.alex;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import edu.alex.ConfigurationProperties.Mode;
import lombok.AllArgsConstructor;
import lombok.Data;

@Component
@PropertySource({"classpath:props-${env}.yml"})
public class MyComponent {

	static @Data @AllArgsConstructor class InnerBean {
		int value;
	}
	
	@Bean
	@ConfigurationProperties(prefix = "some", mode = Mode.MethodsOnly)
	InnerBean ib() {
		return new InnerBean(Integer.MAX_VALUE);
	}
	
	//@Value("#{ T(java.lang.Math).random() * 2 }")
	public int getRef() {
		return Integer.MAX_VALUE;
	}
	
	@Value("#{ ib.value + myComponent.ref }")
	int shouldBe11;

}