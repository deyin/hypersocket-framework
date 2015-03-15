package com.hypersocket.triggers;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.hypersocket.events.SystemEvent;
import com.hypersocket.properties.ResourceTemplateRepository;
import com.hypersocket.repository.CriteriaConfiguration;
import com.hypersocket.resource.AbstractResourceRepositoryImpl;
import com.hypersocket.tasks.TaskProvider;
import com.hypersocket.tasks.TaskProviderService;

@Repository
@Transactional
public class TriggerResourceRepositoryImpl extends
		AbstractResourceRepositoryImpl<TriggerResource> implements
		TriggerResourceRepository {
	
	@Autowired
	TaskProviderService taskService; 
	
	@Override
	protected Class<TriggerResource> getResourceClass() {
		return TriggerResource.class;
	}

	Map<String, ResourceTemplateRepository> registeredRepository = new HashMap<String, ResourceTemplateRepository>();

	@Override
	public void registerActionRepository(TaskProvider action) {
		for (String resourceKey : action.getResourceKeys()) {
			registeredRepository.put(resourceKey, action.getRepository());
		}
	}

	@Override
	public List<TriggerResource> getTriggersForEvent(final SystemEvent event) {

		return allEntities(TriggerResource.class,  
				new CriteriaConfiguration() {

			@Override
			public void configure(Criteria criteria) {
				switch(event.getStatus()) {
				case FAILURE:
					criteria.add(Restrictions.or(Restrictions.eq("result",
							TriggerResultType.EVENT_FAILURE), Restrictions.eq(
							"result", TriggerResultType.EVENT_ANY_RESULT)));
					break;
				case WARNING:
					criteria.add(Restrictions.or(Restrictions.eq("result",
							TriggerResultType.EVENT_WARNING), Restrictions.eq(
							"result", TriggerResultType.EVENT_ANY_RESULT)));
					break;
				default:
					criteria.add(Restrictions.or(Restrictions.eq("result",
							TriggerResultType.EVENT_SUCCESS), Restrictions.eq(
							"result", TriggerResultType.EVENT_ANY_RESULT)));
					break;
				}

				criteria.add(Restrictions.eq("realm", event.getCurrentRealm()));
				criteria.add(Restrictions.isNull("parentAction"));
				criteria.add(Restrictions.in("event", event.getResourceKeys()));
			}
		});

	}

	@Override
	public TriggerAction getActionById(Long id) {
		return get("id", id, TriggerAction.class);
	}

	@Override
	public TriggerCondition getConditionById(Long id) {
		return get("id", id, TriggerCondition.class);
	}

	@Override
	public Collection<TriggerAction> getActionsByResourceKey(final String resourceKey) {
		return list(TriggerAction.class, true, new CriteriaConfiguration() {
			
			@Override
			public void configure(Criteria criteria) {
				criteria.add(Restrictions.eq("resourceKey", resourceKey));
			}
		});
	}

	@Override
	public TriggerAction getActionByPostTriggerId(TriggerResource id) {
		return get("postExecutionTrigger", id, TriggerAction.class);
	}

}
