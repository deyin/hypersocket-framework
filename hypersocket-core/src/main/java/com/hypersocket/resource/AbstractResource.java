package com.hypersocket.resource;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.OneToMany;

import org.codehaus.jackson.annotate.JsonIgnore;

import com.hypersocket.properties.DatabaseProperty;
import com.hypersocket.repository.AbstractEntity;

@Entity
@Inheritance(strategy=InheritanceType.TABLE_PER_CLASS)
public abstract class AbstractResource extends AbstractEntity<Long> {

	@Id
	@GeneratedValue(strategy = GenerationType.TABLE)
	@Column(name="resource_id")
	Long id;

	@OneToMany(mappedBy = "resource", fetch = FetchType.EAGER)
	protected Set<DatabaseProperty> properties;

	public Long getId() {
		return id;
	}
	
	public void setId(Long id) {
		this.id = id;
	}
	
	@JsonIgnore
	public Map<String, DatabaseProperty> getProperties() {
		HashMap<String, DatabaseProperty> mappedProperties = new HashMap<String, DatabaseProperty>();
		if (properties != null) {
			for (DatabaseProperty p : properties) {
				mappedProperties.put(p.getResourceKey(), p);
			}
		}

		return Collections.unmodifiableMap(mappedProperties);
	}

	public String getProperty(String resourceKey) {

		Map<String, DatabaseProperty> properties = getProperties();

		if (properties != null && properties.containsKey(resourceKey)) {
			return properties.get(resourceKey).getValue();
		} else {
			return "";
		}
	}
	
	public int getPropertyInt(String resourceKey) {
		return Integer.parseInt(getProperty(resourceKey));
	}
}
