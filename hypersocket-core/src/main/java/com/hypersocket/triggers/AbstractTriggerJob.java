package com.hypersocket.triggers;

import java.util.Locale;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.hypersocket.auth.AuthenticationService;
import com.hypersocket.events.EventService;
import com.hypersocket.events.SystemEvent;
import com.hypersocket.i18n.I18NService;
import com.hypersocket.realm.Principal;
import com.hypersocket.realm.Realm;
import com.hypersocket.tasks.TaskProvider;
import com.hypersocket.tasks.TaskProviderService;
import com.hypersocket.triggers.events.TriggerExecutedEvent;

public class AbstractTriggerJob implements Job {

	@Autowired
	TriggerResourceService triggerService;

	@Autowired
	AuthenticationService authenticationService; 
	
	@Autowired
	I18NService i18nService; 
	
	@Autowired
	EventService eventService; 
	
	@Autowired
	TaskProviderService taskService; 
	
	static Logger log = LoggerFactory.getLogger(AbstractTriggerJob.class);

	@Override
	public void execute(JobExecutionContext context)
			throws JobExecutionException {

		
		SystemEvent event = (SystemEvent) context.getTrigger().getJobDataMap().get("event");
		Principal principal = (Principal) context.getTrigger().getJobDataMap().get("principal");
		Locale locale = (Locale) context.getTrigger().getJobDataMap().get("locale");
		Realm realm = (Realm) context.getTrigger().getJobDataMap().get("realm");
		
		if(log.isInfoEnabled()) {
			log.info("Starting trigger job for event " + event.getResourceKey());
		}
		
		authenticationService.setCurrentPrincipal(principal, locale, realm);
		
		try {
			
			TriggerResource trigger = (TriggerResource) context.getTrigger()
					.getJobDataMap().get("trigger");

			try {
				processEventTrigger(trigger, event);

				if(trigger.getFireEvent()!=null && trigger.getFireEvent()) {
					eventService.publishEvent(new TriggerExecutedEvent(this, trigger));
				}
				
			} catch (Throwable e) {
				eventService.publishEvent(new TriggerExecutedEvent(this, trigger, e));
			} 
		} finally {
			authenticationService.clearPrincipalContext();
		}

	}

	protected void processEventTrigger(TriggerResource trigger,
			SystemEvent event) throws ValidationException {
		if (log.isInfoEnabled()) {
			log.info("Processing trigger " + trigger.getName());
		}

		if (checkConditions(trigger, event)) {
			for (TriggerAction action : trigger.getActions()) {
				executeAction(action, event);
			}
		}
		if (log.isInfoEnabled()) {
			log.info("Finished processing trigger " + trigger.getName());
		}

	}

	protected boolean checkConditions(TriggerResource trigger, SystemEvent event)
			throws ValidationException {

		for (TriggerCondition condition : trigger.getAllConditions()) {
			if (!checkCondition(condition, trigger, event)) {
				if (log.isDebugEnabled()) {
					log.debug("Trigger " + trigger.getName()
							+ " failed processing all conditions due to "
							+ condition.getConditionKey() + " attributeValue="
							+ condition.getAttributeKey() + " conditionValue="
							+ condition.getConditionValue());
				}
				return false;
			}
		}

		if (trigger.getAnyConditions().size() > 0) {
			boolean conditionPassed = false;
			for (TriggerCondition condition : trigger.getAnyConditions()) {
				if (checkCondition(condition, trigger, event)) {
					conditionPassed = true;
					break;
				}
				if (log.isDebugEnabled()) {
					log.debug("Trigger " + trigger.getName()
							+ " failed processing any conditions due to "
							+ condition.getConditionKey() + " attributeValue="
							+ condition.getAttributeKey() + " conditionValue="
							+ condition.getConditionValue());
				}
			}
			return conditionPassed;
		}

		return true;
	}

	private boolean checkCondition(TriggerCondition condition,
			TriggerResource trigger, SystemEvent event)
			throws ValidationException {

		TriggerConditionProvider provider = triggerService
				.getConditionProvider(condition);

		if (provider == null) {
			throw new ValidationException(
					"Failed to check condition because provider "
							+ condition.getConditionKey() + " is not available");
		}
		return provider.checkCondition(condition, trigger, event);

	}

	protected void executeAction(TriggerAction action, SystemEvent event)
			throws ValidationException {

		TaskProvider provider = taskService
				.getActionProvider(action.getResourceKey());
		if (provider == null) {
			throw new ValidationException(
					"Failed to execute action because provider "
							+ action.getResourceKey() + " is not available");
		}

		TaskResult outputEvent = provider.execute(action, event);

		if(outputEvent!=null) {
			if(outputEvent.isPublishable()) {
				eventService.publishEvent(outputEvent);
			}
			
			if (action.getPostExecutionTrigger() != null
					&& checkConditions(action.getTrigger(), outputEvent)) {
				processEventTrigger(action.getPostExecutionTrigger(), outputEvent);
			} 
		}
		
	}
}
