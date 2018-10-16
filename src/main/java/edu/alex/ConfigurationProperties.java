package edu.alex;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigurationProperties {

	String prefix() default "";
	
	enum Mode {
		MethodsThenFields,
		MethodsOnly
	}
	
	Mode mode() default Mode.MethodsThenFields;
}
