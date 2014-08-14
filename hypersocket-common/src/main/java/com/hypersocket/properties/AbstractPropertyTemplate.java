package com.hypersocket.properties;

import org.codehaus.jackson.annotate.JsonIgnore;

public abstract class AbstractPropertyTemplate {

	String resourceKey;
	String defaultValue;
	String metaData;
	int weight;
	boolean hidden;
	boolean readOnly;
	PropertyCategory category;
	String mapping;
	
	public String getResourceKey() {
		return resourceKey;
	}

	public int getId() {
		return resourceKey.hashCode();
	}
	
	public void setResourceKey(String resourceKey) {
		this.resourceKey = resourceKey;
	}

	public abstract String getValue();
	
	public String getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public String getMetaData() {
		return metaData;
	}

	public void setMetaData(String metaData) {
		this.metaData = metaData;
	}

	public Integer getWeight() {
		return weight;
	}

	public void setWeight(int weight) {
		this.weight = weight;
	}
	
	public boolean isHidden() {
		return hidden;
	}

	public void setHidden(boolean hidden) {
		this.hidden = hidden;
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}
	
	@JsonIgnore
	public PropertyCategory getCategory() {
		return category;
	}
	
	public void setCategory(PropertyCategory category) {
		this.category = category;
	}

	public String getMapping() {
		return mapping;
	}

	public void setMapping(String mapping) {
		this.mapping = mapping;
	}
	
	
}
