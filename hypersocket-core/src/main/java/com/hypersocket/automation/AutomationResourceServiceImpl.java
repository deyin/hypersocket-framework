package com.hypersocket.automation;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.stereotype.Service;

import com.hypersocket.automation.events.AutomationResourceCreatedEvent;
import com.hypersocket.automation.events.AutomationResourceDeletedEvent;
import com.hypersocket.automation.events.AutomationResourceEvent;
import com.hypersocket.automation.events.AutomationResourceUpdatedEvent;
import com.hypersocket.events.EventService;
import com.hypersocket.i18n.I18NService;
import com.hypersocket.permissions.AccessDeniedException;
import com.hypersocket.permissions.PermissionCategory;
import com.hypersocket.permissions.PermissionService;
import com.hypersocket.properties.EntityResourcePropertyStore;
import com.hypersocket.properties.PropertyCategory;
import com.hypersocket.realm.Realm;
import com.hypersocket.resource.AbstractResourceRepository;
import com.hypersocket.resource.AbstractResourceServiceImpl;
import com.hypersocket.resource.ResourceChangeException;
import com.hypersocket.resource.ResourceCreationException;
import com.hypersocket.scheduler.PermissionsAwareJobData;
import com.hypersocket.scheduler.SchedulerService;
import com.hypersocket.tasks.TaskProvider;
import com.hypersocket.tasks.TaskProviderService;

@Service
public class AutomationResourceServiceImpl extends
		AbstractResourceServiceImpl<AutomationResource> implements
		AutomationResourceService, ApplicationListener<ContextStartedEvent> {

	private static Logger log = LoggerFactory.getLogger(AutomationResourceServiceImpl.class);
	
	public static final String RESOURCE_BUNDLE = "AutomationResourceService";

	private Map<Long,String> scheduleIdsByResource = new HashMap<Long,String>();
	
	@Autowired
	AutomationResourceRepository repository;

	@Autowired
	I18NService i18nService;

	@Autowired
	PermissionService permissionService;

	@Autowired
	EventService eventService;

	@Autowired
	EntityResourcePropertyStore entityPropertyStore; 
	
	@Autowired
	SchedulerService schedulerService; 
	
	@Autowired
	TaskProviderService taskService; 
	
	@PostConstruct
	private void postConstruct() {

		i18nService.registerBundle(RESOURCE_BUNDLE);

		PermissionCategory cat = permissionService.registerPermissionCategory(
				RESOURCE_BUNDLE, "category.automation");

		for (AutomationResourcePermission p : AutomationResourcePermission.values()) {
			permissionService.registerPermission(p, cat);
		}

		repository.loadPropertyTemplates("automationTemplate.xml");


		/**
		 * Register the events. All events have to be registerd so the system
		 * knows about them.
		 */
		eventService.registerEvent(
				AutomationResourceEvent.class, RESOURCE_BUNDLE,
				this);
		eventService.registerEvent(
				AutomationResourceCreatedEvent.class, RESOURCE_BUNDLE,
				this);
		eventService.registerEvent(
				AutomationResourceUpdatedEvent.class, RESOURCE_BUNDLE,
				this);
		eventService.registerEvent(
				AutomationResourceDeletedEvent.class, RESOURCE_BUNDLE,
				this);

		entityPropertyStore.registerResourceService(AutomationResource.class, this);
	}
	
	@Override
	protected AbstractResourceRepository<AutomationResource> getRepository() {
		return repository;
	}

	@Override
	protected String getResourceBundle() {
		return RESOURCE_BUNDLE;
	}

	@Override
	public Class<AutomationResourcePermission> getPermissionType() {
		return AutomationResourcePermission.class;
	}

	@Override
	protected void fireResourceCreationEvent(AutomationResource resource) {
		eventService.publishEvent(new AutomationResourceCreatedEvent(this,
				getCurrentSession(), resource));
	}

	@Override
	protected void fireResourceCreationEvent(AutomationResource resource,
			Throwable t) {
		eventService.publishEvent(new AutomationResourceCreatedEvent(this,
				resource, t, getCurrentSession()));
	}

	@Override
	protected void fireResourceUpdateEvent(AutomationResource resource) {
		eventService.publishEvent(new AutomationResourceUpdatedEvent(this,
				getCurrentSession(), resource));
	}

	@Override
	protected void fireResourceUpdateEvent(AutomationResource resource,
			Throwable t) {
		eventService.publishEvent(new AutomationResourceUpdatedEvent(this,
				resource, t, getCurrentSession()));
	}

	@Override
	protected void fireResourceDeletionEvent(AutomationResource resource) {
		eventService.publishEvent(new AutomationResourceDeletedEvent(this,
				getCurrentSession(), resource));
	}

	@Override
	protected void fireResourceDeletionEvent(AutomationResource resource,
			Throwable t) {
		eventService.publishEvent(new AutomationResourceDeletedEvent(this,
				resource, t, getCurrentSession()));
	}

	@Override
	public AutomationResource updateResource(AutomationResource resource,
			String name, Map<String, String> properties)
			throws ResourceChangeException, AccessDeniedException {

		resource.setName(name);

		updateResource(resource, properties);

		schedule(resource);
		
		return resource;
	}
	
	@Override
	protected void beforeCreateResource(AutomationResource resource,
			Map<String, String> properties) throws ResourceCreationException {
		super.beforeCreateResource(resource, properties);
	}

	@Override
	protected void beforeUpdateResource(AutomationResource resource,
			Map<String, String> properties) throws ResourceChangeException {
		super.beforeUpdateResource(resource, properties);
	}

	@Override
	protected void afterCreateResource(AutomationResource resource, Map<String,String> properties) throws ResourceCreationException {
		TaskProvider provider = taskService.getActionProvider(resource);
		provider.getRepository().setValues(resource, properties);
	}
	
	@Override
	protected void afterUpdateResource(AutomationResource resource, Map<String,String> properties) throws ResourceChangeException {
		TaskProvider provider = taskService.getActionProvider(resource);
		provider.getRepository().setValues(resource, properties);
	}
	

	protected Date calculateDateTime(Date from, String time) {
		
		Calendar c = Calendar.getInstance();
		
		if(from!=null) {
			c.setTime(from);
		}
		
		if(!StringUtils.isEmpty(time)) {
			int idx = time.indexOf(':');				
			c.set(Calendar.HOUR_OF_DAY, Integer.parseInt(time.substring(0, idx)));
			c.set(Calendar.MINUTE, Integer.parseInt(time.substring(idx+1)));
		}
		
		return c.getTime();
	}	
	
	
	protected void schedule(AutomationResource resource) {
		
		Date start = calculateDateTime(resource.getStartDate(), resource.getStartTime());
		Date end = calculateDateTime(resource.getEndDate(), resource.getEndTime());
		
		int interval = 0;
		int repeat = -1; 
		
		if(resource.getRepeatValue() > 0) {
			
			switch(resource.getRepeatType()) {
			case DAYS:
				interval = resource.getRepeatValue() * (60000 * 60 * 24);
				break;
			case HOURS:
				interval = resource.getRepeatValue() * (60000 * 60);
				break;
			case MINUTES:
				interval = resource.getRepeatValue() * 60000;
				break;
			case SECONDS:
				interval = resource.getRepeatValue() * 1000;
				break;
			case NEVER:
			default:	
				interval = 0;
				repeat = 0;
				break;
			}
		}
		
		PermissionsAwareJobData data = new PermissionsAwareJobData(resource.getRealm());
		data.put("resourceId", resource.getId());
		
		try {
			
			String scheduleId;
			
			if(scheduleIdsByResource.containsKey(resource.getId())) {
				
				scheduleId = scheduleIdsByResource.get(resource.getId());
				
				if(start.before(new Date())) {
					schedulerService.rescheduleNow(scheduleId, interval, repeat, end);
				} else {
					schedulerService.rescheduleAt(scheduleId, start, interval, repeat, end);
				}
				
			} else {
				if(start.before(new Date())) {
					scheduleId = schedulerService.scheduleNow(AutomationJob.class, data, interval, repeat, end);
				} else {
					scheduleId = schedulerService.scheduleAt(AutomationJob.class, data, start, interval, repeat, end);
				}
				
				scheduleIdsByResource.put(resource.getId(), scheduleId);
			}
		} catch (SchedulerException e) {
			log.error("Failed to schedule automation task " + resource.getName(), e);
		}
	}

	@Override
	public AutomationResource createResource(String name, Realm realm,
			Map<String, String> properties) throws ResourceCreationException,
			AccessDeniedException {

		AutomationResource resource = new AutomationResource();
		resource.setName(name);
		resource.setRealm(realm);

		createResource(resource, properties);

		schedule(resource);
		
		return resource;
	}

	@Override
	public Collection<PropertyCategory> getPropertyTemplate(String resourceKey)
			throws AccessDeniedException {

		assertPermission(AutomationResourcePermission.READ);

		Collection<PropertyCategory> results = repository.getPropertyCategories(null);
		
		TaskProvider provider = taskService.getActionProvider(resourceKey);
		
		results.addAll(provider.getRepository().getPropertyCategories(null));
		
		return results;
	}

	
	@Override
	public Collection<PropertyCategory> getPropertyTemplate(
			AutomationResource resource) throws AccessDeniedException {
	
		assertPermission(AutomationResourcePermission.READ);

		Collection<PropertyCategory> results = repository.getPropertyCategories(resource);
		
		TaskProvider provider = taskService.getActionProvider(resource);
		
		results.addAll(provider.getRepository().getPropertyCategories(resource));
		
		return results;
	}

	@Override
	public Collection<PropertyCategory> getPropertyTemplate()
			throws AccessDeniedException {
		throw new IllegalStateException("AutomationResource needs provider resource key to return property templates");
	}

	@Override
	public Collection<String> getTasks() throws AccessDeniedException {
		
		assertPermission(AutomationResourcePermission.READ);
		
		return taskService.getActions();
	}

	@Override
	public void onApplicationEvent(ContextStartedEvent event) {
		
		if(log.isInfoEnabled()) {
			log.info("Scheduling one time only or repetitive automation resources");
		}
		
		for(AutomationResource resource: repository.getResources(null)) {
			if(!resource.isDailyJob()) {
				schedule(resource);
			}
		}
		
		scheduleDailyJobs();
	}

	@Override
	public void scheduleDailyJobs() {
		
		if(log.isInfoEnabled()) {
			log.info("Scheduling daily automation resources");
		}
		
		for(AutomationResource resource: repository.getResources(null)) {
			if(resource.isDailyJob()) {
				schedule(resource);
			}
		}
		
		Calendar c = Calendar.getInstance();
		c.set(Calendar.HOUR, 0);
		c.set(Calendar.MINUTE, 0);
		c.add(Calendar.DAY_OF_MONTH, 1);
		
		try {
			schedulerService.scheduleAt(DailySchedulerJob.class, null, c.getTime());
		} catch (SchedulerException e) {
			log.error("Failed to schedule daily automation jobs", e);
		}
		
	}
}
