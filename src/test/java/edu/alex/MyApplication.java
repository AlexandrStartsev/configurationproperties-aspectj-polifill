package edu.alex;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.PropertySource;

@Configuration
@Aspect
@EnableAspectJAutoProxy(proxyTargetClass=true)
@ComponentScan
@PropertySource("classpath:application.properties")
public class MyApplication {
	
	// Just some random test stuff 
	
	@Around("execution(* edu.alex.MyComponent.getRef(..))")
	public Object anyName(ProceedingJoinPoint point) throws Throwable {
		/*
		 * Object retVal = point.proceed();
		 */
		return 1;
	}
}