package com.hypersocket.properties;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;

import com.hypersocket.encrypt.EncryptionService;
import com.hypersocket.resource.AbstractAssignableResourceService;
import com.hypersocket.resource.AbstractResource;
import com.hypersocket.resource.AbstractResourceService;
import com.hypersocket.utils.HypersocketUtils;

@Component
public class EntityResourcePropertyStore extends AbstractResourcePropertyStore {

	static Logger log = LoggerFactory.getLogger(EntityResourcePropertyStore.class);
	
	Map<Class<?>,AbstractResourceService<?>> resourceServices = new HashMap<Class<?>,AbstractResourceService<?>>();
	Map<Class<?>,AbstractAssignableResourceService<?>> assignableServices = new HashMap<Class<?>,AbstractAssignableResourceService<?>>();
	Map<Class<?>, PrimitiveParser<?>> primitiveParsers = new HashMap<Class<?>,PrimitiveParser<?>>();
	
	@Autowired
	EncryptionService encryptionService; 
	
	@PostConstruct
	private void postConstruct() {
		primitiveParsers.put(String.class, new StringValue());
		primitiveParsers.put(Boolean.class, new BooleanValue());
		primitiveParsers.put(Integer.class, new IntegerValue());
		primitiveParsers.put(Long.class, new LongValue());
		primitiveParsers.put(Double.class, new DoubleValue());
		primitiveParsers.put(Date.class, new DateValue());
		
		setEncryptionService(encryptionService);
	}
	
	public void registerResourceService(Class<?> clz, AbstractResourceService<?> service) {
		resourceServices.put(clz, service);
	}
	
	public void registerResourceService(Class<?> clz, AbstractAssignableResourceService<?> service) {
		assignableServices.put(clz, service);
	}
	
	@Override
	protected String lookupPropertyValue(PropertyTemplate template) {
		return template.getDefaultValue();
	}

	@Override
	protected void doSetProperty(PropertyTemplate template, String value) {
		throw new UnsupportedOperationException("Entity resource property store requires an entity resource to set property value");
	}

	@Override
	protected String lookupPropertyValue(AbstractPropertyTemplate template,
			AbstractResource resource) {
		
		if(resource==null) {
			return template.getDefaultValue();
		}
		Throwable t;
		String methodName = "get" + StringUtils.capitalize(template.getResourceKey());
		try {
			
			Method m = resource.getClass().getMethod(methodName, (Class<?>[])null);
			Object obj = m.invoke(resource);
			if(obj==null) {
				return "";
			}
			return obj.toString();
		} catch (NoSuchMethodException e) {
			t = e;
		} catch (SecurityException e) {
			t = e;
		} catch (IllegalAccessException e) {
			t = e;
		} catch (IllegalArgumentException e) {
			t = e;
		} catch (InvocationTargetException e) {
			t = e;
		}
		throw new IllegalStateException(methodName + " not found", t);
	}

	@Override
	protected void doSetProperty(AbstractPropertyTemplate template,
			AbstractResource resource, String value) {
		
		Method[] methods = resource.getClass().getMethods();
		
		String methodName = "set" + StringUtils.capitalize(template.getResourceKey());
		for(Method m : methods) {
			if(m.getName().equals(methodName)) {
				Class<?> clz = m.getParameterTypes()[0];
				if(primitiveParsers.containsKey(clz)) {
					try {
						m.invoke(resource, primitiveParsers.get(clz).parseValue(value));
					} catch (Exception e) {
						log.error("Could not set " + template.getResourceKey() + " primitive value " + value + " for resource " + resource.getClass().getName(), e);
						throw new IllegalStateException("Could not set " + template.getResourceKey() + " primitive value " + value + " for resource " + resource.getClass().getName(), e);
					}
					return;
				}
				if(resourceServices.containsKey(clz)) {
					try {
						m.invoke(resource, resourceServices.get(clz).getResourceById(Long.parseLong(value)));
					} catch (Exception e) {
						log.error("Could not lookup " + template.getResourceKey() + " value " + value + " for resource " + resource.getClass().getName(), e);
						throw new IllegalStateException("Could not lookup " + template.getResourceKey() + " value " + value + " for resource " + resource.getClass().getName(), e);
					}
					return;
				}
				if(assignableServices.containsKey(clz)) {
					try {
						m.invoke(resource, assignableServices.get(clz).getResourceById(Long.parseLong(value)));
					} catch (Exception e) {
						log.error("Could not lookup " + template.getResourceKey() + " value " + value + " for resource " + resource.getClass().getName(), e);
						throw new IllegalStateException("Could not lookup " + template.getResourceKey() + " value " + value + " for resource " + resource.getClass().getName(), e);
					}
					return;
				}
				
			}
		}
		
		throw new IllegalStateException("Could not set " + template.getResourceKey() + " value " + value + " for resource " + resource.getClass().getName());
	}

	
	interface PrimitiveParser<T> {
		T parseValue(String value);
	}
	
	class StringValue implements PrimitiveParser<String> {
		public String parseValue(String value) {
			return value;
		}
	}
	
	class BooleanValue implements PrimitiveParser<Boolean> {
		public Boolean parseValue(String value) {
			return Boolean.valueOf(value);
		}
	}
	
	class IntegerValue implements PrimitiveParser<Integer> {
		public Integer parseValue(String value) {
			return Integer.valueOf(value);
		}
	}
	
	class LongValue implements PrimitiveParser<Long> {
		public Long parseValue(String value) {
			return Long.valueOf(value);
		}
	}
	
	class DoubleValue implements PrimitiveParser<Double> {
		public Double parseValue(String value) {
			return Double.valueOf(value);
		}
	}
	
	class DateValue implements PrimitiveParser<Date> {

		@Override
		public Date parseValue(String value) {
			try {
				return HypersocketUtils.parseDate(value, "yyyy-MM-dd");
			} catch (ParseException e) {
				log.warn("Failed to parse date value " + value);
				return null;
			}
		}
		
	}

	@Override
	public void init(Element element) throws IOException {
		
	}

}
