package edu.alex;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

import edu.alex.MyComponent.InnerBean;
import edu.alex.MyConfiguration.Int;

public class ConfigPropTest {

	@Test
	public void test() {
		try(AbstractApplicationContext context = new AnnotationConfigApplicationContext(MyApplication.class)) {
			MyComponent component = context.getBean(MyComponent.class);
			MyConfiguration configuration = context.getBean(MyConfiguration.class);
			InnerBean innerBean = context.getBean(InnerBean.class);
			Int t3 = context.getBean(Int.class);
			
			assertEquals("some-value-dev", configuration.verify());
			assertEquals(10, innerBean.getValue());
			assertEquals(10, component.ib().value);
			assertEquals(2, t3.getTwo());
			assertNotEquals(0, configuration.getSomeProperties().size());
			assertEquals(0, configuration.getSomeOtherProperties().size());
			
			//assertEquals(10, component.shouldBe11);
		}
	}

}
