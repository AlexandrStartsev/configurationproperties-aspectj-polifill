package edu.alex;

import static org.springframework.util.ReflectionUtils.findField;
import static org.springframework.util.ReflectionUtils.findMethod;
import static org.springframework.util.ReflectionUtils.invokeMethod;
import static org.springframework.util.ReflectionUtils.setField;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import edu.alex.ConfigurationProperties.Mode;

/**
 * @author Alexandr Startsev
 * 
 * Note: seems like using this in @Configuration classes requires @EnableAspectJAutoProxy(proxyTargetClass=true)
 * */

@Component
@Aspect
public class ConfigurationPropertiesAspect {
	
	final Environment env;
	
	// Not sure how reliable is this
	ConfigurationPropertiesAspect(Environment env) {
		this.env = env;
	}
		
	/**
	 * No matter what I do, AOP won't see annotations of @Configuration class methods, 
	 * is it because it s proxy made out of artificial interface (or maybe I just don't get it, after all signature "getDeclaringType" is correct).
	 * 
	 * This is obviously overkill, in actual application it would be nice to at least narrow it down to package
	 * */
	@Pointcut("execution(* *(..))")
	public void catchAll() {}
	
	@AfterReturning(pointcut = "catchAll() && target(target)", returning = "object")
	public void doConfigurations(JoinPoint point, Object object, Object target) throws Throwable {
		final Class<?> ctype = point.getSignature().getDeclaringType();
		if(ctype.isAnnotationPresent(Configuration.class)) {
			final PropertySource propSrc = ctype.getAnnotation(PropertySource.class);
			// TODO: args
			final Method method = findMethod(ctype, point.getSignature().getName());
			if(method != null) {
				final ConfigurationProperties config = method.getAnnotation(ConfigurationProperties.class);
				if(config != null) {
					configurationProperties(point, config, propSrc, object);
				}
			}
		}
	}
	
	private final static Map<String, Map<String, Object>> resources = new HashMap<>();

	/**
	 * Modeled after @ConfigurationProperties in spring boot + can write properties directly (if there is no set*** method) 
	 * advice will look at declaring class (target's) @PropertySource and if defined will attempt 
	 * to inject property values into methods and fields  
	 *
	 * Also, I'm not sure how reliable is "Environment" here
	 * */
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@AfterReturning(pointcut = "@annotation(config) && @target(propSrc)", returning = "object")
	public void configurationProperties(JoinPoint point, ConfigurationProperties config, PropertySource propSrc, Object object)
			throws Throwable {
		final String prefix = config.prefix();
		final Mode mode = config.mode();
		final boolean root = StringUtils.isBlank(prefix);
		final Class<?> clazz = object.getClass();
		final Map<String, Object> allProps = new HashMap<>();
		final Function<String, String> trimKey = s -> s.substring(prefix.length() + 1);
		Arrays.stream(propSrc.value()).map(env::resolveRequiredPlaceholders).map(res -> {
			final boolean isYalm = res.matches(".+[.]yml$");
			final Map<String, Object> mp = resources.computeIfAbsent(res, s -> {
				try {
					InputStream is = null;
					try {
						switch (StringUtils.substringBefore(res, ":")) {
						case "classpath":
							is = new ClassPathResource(StringUtils.substringAfter(res, ":")).getInputStream();
							break;
						case "file":
							is = new FileInputStream(StringUtils.substringAfter(res, ":"));
							break;
						default:
						}
						if (is != null) {
							if (isYalm) {
								return new Yaml().load(is);
							} else if (res.matches(".+[.]properties$")) {
								Properties p = new Properties();
								p.load(is);
								return (Map) p;
							}
						} else {
							throw new RuntimeException("Unable to locate resource: " + res);
						}
					} finally {
						if (is != null) {
							is.close();
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				return new HashMap<>();
			});
			return root ? mp
					: (Map) (isYalm ? mp.get(prefix)
							: new TreeMap<>(mp).subMap(prefix + ".", prefix + "/").entrySet().stream()
									.collect(Collectors.toMap(e -> trimKey.apply(e.getKey()), Entry::getValue)));
		}).forEach(allProps::putAll);
		
		/**
		 * This also needs to be elaborated + not sure what to do about 'env'.
		 * Does it need to support @Value on properties, or more advanced type mapping?  
		 * 
		 * */
		
		final Function<Object, String> stringifyResolve = v -> v instanceof String ? env.resolvePlaceholders((String)v) : v.toString();
		final BiFunction<Class<?>, String, ?> tryConvert = (c, o) -> c == String.class ? o : invokeMethod(findMethod(ClassUtils.primitiveToWrapper(c), "valueOf", String.class), null, o);
				
		if(object instanceof Map) {
			allProps.forEach((k, v) -> ((Map) object).put(k, stringifyResolve.apply(v)));
		} else {
			allProps.forEach((k, v) -> {
				if(v != null) {
					final Predicate<String> p = ("set" + StringUtils.capitalize(k))::equals;
					final Method setter = Arrays.stream(object.getClass().getMethods()).filter(m -> p.test(m.getName())).findFirst().orElse(null);
					if(setter != null && setter.getParameterCount() == 1) {
						setter.setAccessible(true);
						invokeMethod(setter, object, tryConvert.apply(setter.getParameterTypes()[0], stringifyResolve.apply(v)));
					} else if( mode != Mode.MethodsOnly ) {
						Field fld = findField(clazz, k);
						if (fld != null ) {
							fld.setAccessible(true);
							setField(fld, object, tryConvert.apply(fld.getType(), stringifyResolve.apply(v)));
						}
					}
				}
			});
		}
	}
}
