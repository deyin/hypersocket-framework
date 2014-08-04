/*******************************************************************************
 * Copyright (c) 2013 Hypersocket Limited.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package com.hypersocket.config;

import java.util.Collection;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.hypersocket.auth.AuthenticatedServiceImpl;
import com.hypersocket.events.EventService;
import com.hypersocket.permissions.AccessDeniedException;
import com.hypersocket.permissions.PermissionService;
import com.hypersocket.properties.PropertyCategory;
import com.hypersocket.properties.PropertyTemplate;
import com.hypersocket.resource.ResourceChangeException;

@Transactional
public class SystemConfigurationServiceImpl extends AuthenticatedServiceImpl
		implements SystemConfigurationService {

	static Logger log = LoggerFactory
			.getLogger(SystemConfigurationServiceImpl.class);

	@Autowired
	SystemConfigurationRepository repository;

	@Autowired
	PermissionService permissionService;

	@Autowired
	EventService eventService;

	@PostConstruct
	private void postConstruct() {

		eventService.registerEvent(ConfigurationChangedEvent.class,
				ConfigurationServiceImpl.RESOURCE_BUNDLE);
		
		repository.loadPropertyTemplates("systemTemplates.xml");
	}

	protected void onValueChanged(PropertyTemplate template, String oldValue,
			String value) {
		eventService.publishEvent(new ConfigurationChangedEvent(this, true,
				getCurrentSession(), template, oldValue, value));
	}

	@Override
	public String getValue(String resourceKey) {
		return repository.getValue(resourceKey);
	}

	@Override
	public Integer getIntValue(String name) throws NumberFormatException {
		return repository.getIntValue(name);
	}

	@Override
	public Boolean getBooleanValue(String name) {
		return repository.getBooleanValue(name);
	}

	@Override
	public void setValue(String resourceKey, String value)
			throws AccessDeniedException {
		assertPermission(ConfigurationPermission.UPDATE);
		repository.setValue(resourceKey, value);
	}

	@Override
	public void setValue(String resourceKey, Integer value)
			throws AccessDeniedException {
		assertPermission(ConfigurationPermission.UPDATE);
		repository.setValue(resourceKey, value);
	}

	@Override
	public void setValue(String name, Boolean value)
			throws AccessDeniedException {
		assertPermission(ConfigurationPermission.UPDATE);
		repository.setValue(name, value);
	}

	@Override
	public Collection<PropertyCategory> getPropertyCategories()
			throws AccessDeniedException {
		assertPermission(ConfigurationPermission.READ);
		return repository.getPropertyCategories();
	}

	@Override
	public String[] getValues(String name) {
		return repository.getValues(name);
	}

	@Override
	public void setValues(Map<String, String> values)
			throws AccessDeniedException, ResourceChangeException {

		assertPermission(ConfigurationPermission.UPDATE);
		repository.setValues(values);

	}

	@Override
	public Collection<PropertyCategory> getPropertyCategories(String group)
			throws AccessDeniedException {
		assertPermission(ConfigurationPermission.READ);
		return repository.getPropertyCategories(group);
	}
}