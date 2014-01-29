package com.hypersocket.properties;

import java.util.Collection;

import com.hypersocket.permissions.AccessDeniedException;

public interface PropertyTemplateService {

	String getValue(String resourceKey);

	Integer getIntValue(String name) throws NumberFormatException;

	Boolean getBooleanValue(String name);

	void setValue(String resourceKey, String value)
			throws AccessDeniedException;

	void setValue(String resourceKey, Integer value)
			throws AccessDeniedException;

	void setValue(String name, Boolean value) throws AccessDeniedException;

	Collection<PropertyCategory> getPropertyCategories()
			throws AccessDeniedException;

	String[] getValues(String name);

}
