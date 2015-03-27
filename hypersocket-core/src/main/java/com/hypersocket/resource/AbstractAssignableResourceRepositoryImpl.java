/*******************************************************************************
 * Copyright (c) 2013 Hypersocket Limited.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package com.hypersocket.resource;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.hypersocket.properties.EntityResourcePropertyStore;
import com.hypersocket.properties.ResourcePropertyStore;
import com.hypersocket.properties.ResourceTemplateRepositoryImpl;
import com.hypersocket.realm.Principal;
import com.hypersocket.realm.Realm;
import com.hypersocket.realm.RealmRestriction;
import com.hypersocket.repository.CriteriaConfiguration;
import com.hypersocket.repository.DeletedCriteria;
import com.hypersocket.repository.DeletedDetachedCriteria;
import com.hypersocket.session.Session;
import com.hypersocket.tables.ColumnSort;
import com.hypersocket.tables.Sort;

@Repository
public abstract class AbstractAssignableResourceRepositoryImpl<T extends AssignableResource>
		extends ResourceTemplateRepositoryImpl implements
		AbstractAssignableResourceRepository<T> {

	@Autowired
	EntityResourcePropertyStore entityPropertyStore;

	CacheManager cacheManager;
	Cache queryCache;
	
	@PostConstruct
	private void postConstruct() {
		cacheManager = CacheManager.newInstance();
		queryCache = new Cache("assignable" + getResourceClass().getName() + "QueryCache", 5000, false, false,
				60 * 60, 60 * 60);
		cacheManager.addCache(queryCache);
	}
	
	@Override
	@Transactional(readOnly=true)
	public Collection<T> getAssignedResources(List<Principal> principals) {
		return getAssignedResources(principals.toArray(new Principal[0]));
	}

	@Override
	protected ResourcePropertyStore getPropertyStore() {
		return entityPropertyStore;
	}

	@SuppressWarnings("unchecked")
	@Override
	@Transactional(readOnly=true)
	public Collection<T> getAssignedResources(Principal... principals) {

		StringBuffer keydata = new StringBuffer("getAssignedResources;");
		keydata.append(getResourceClass().getCanonicalName());
		keydata.append(";");
		for(Principal p : principals) {
			keydata.append(p.getId());
			keydata.append(";");
		}
		
		String key = keydata.toString();
		
		if (!queryCache.isElementInMemory(key)
				|| (queryCache.get(key) == null 
					|| queryCache.isExpired(queryCache.get(key)))) {
			
		
			Criteria criteria = createCriteria(getResourceClass());
			criteria.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
			
			criteria.add(Restrictions.eq("realm", principals[0].getRealm()));
			
			criteria = criteria.createCriteria("roles");
			criteria.add(Restrictions.eq("allUsers", true));
			
			Set<T> everyone = new HashSet<T>(criteria.list());
			
			Set<Long> ids = new HashSet<Long>();
			for (Principal p : principals) {
				ids.add(p.getId());
			}
	
			criteria = createCriteria(getResourceClass());
			
			criteria.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
			
			criteria.add(Restrictions.eq("realm", principals[0].getRealm()));
	
			criteria = criteria.createCriteria("roles");
			criteria.add(Restrictions.eq("allUsers", false));
			criteria = criteria.createCriteria("principals");
			criteria.add(Restrictions.in("id", ids));
			
			everyone.addAll((List<T>) criteria.list());
			
			queryCache.put(new Element(key, everyone));
		
		}
		
		return (Collection<T>) queryCache.get(key).getObjectValue();
	}

	@SuppressWarnings("unchecked")
	@Override
	@Transactional(readOnly=true)
	public Collection<T> searchAssignedResources(Principal principal,
			final String searchPattern, final int start, final int length,
			final ColumnSort[] sorting, CriteriaConfiguration... configs) {

		StringBuffer keydata = new StringBuffer("searchAssignedResources;");
		keydata.append(getResourceClass().getCanonicalName());
		keydata.append(";");
		keydata.append(principal.getId());
		keydata.append(";");
		keydata.append(searchPattern);
		keydata.append(";");
		keydata.append(start);
		keydata.append(";");
		keydata.append(length);
		for(ColumnSort s : sorting) {
			keydata.append(s.getColumn().getColumnName());
			keydata.append(";");
		}
		
		String key = keydata.toString();
		
		if (!queryCache.isElementInMemory(key)
				|| (queryCache.get(key) == null 
					|| queryCache.isExpired(queryCache.get(key)))) {
			
			Criteria criteria = createCriteria(getResourceClass());
			criteria.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
			
			if (StringUtils.isNotBlank(searchPattern)) {
				criteria.add(Restrictions.like("name", searchPattern));
			}
	
			for (CriteriaConfiguration c : configs) {
				c.configure(criteria);
			}
	
			criteria.add(Restrictions.eq("realm", principal.getRealm()));
			criteria = criteria.createCriteria("roles");
			criteria.add(Restrictions.eq("allUsers", true));
			
			Set<T> everyone = new HashSet<T>(criteria.list());
	
			criteria = createCriteria(getResourceClass());
			
			ProjectionList projList = Projections.projectionList();
			projList.add(Projections.property("id"));
			projList.add(Projections.property("name"));
			
			criteria.setProjection(Projections.distinct(projList));
			criteria.setFirstResult(start);
			criteria.setMaxResults(length);
			
			if (StringUtils.isNotBlank(searchPattern)) {
				criteria.add(Restrictions.like("name", searchPattern));
			}
	
			for (CriteriaConfiguration c : configs) {
				c.configure(criteria);
			}
			
			for (ColumnSort sort : sorting) {
				criteria.addOrder(sort.getSort() == Sort.ASC ? Order.asc(sort
						.getColumn().getColumnName()) : Order.desc(sort.getColumn()
						.getColumnName()));
			}
			
			criteria.add(Restrictions.eq("realm", principal.getRealm()));
	
			criteria = criteria.createCriteria("roles");
			criteria.add(Restrictions.eq("allUsers", false));
			criteria = criteria.createCriteria("principals");
			criteria.add(Restrictions.in("id", new Long[] { principal.getId() }));
			
			List<Object[]> results = (List<Object[]>)criteria.list();
			
			if(results.size() > 0) {
				Long[] entityIds = new Long[results.size()];
				int idx = 0;
				for(Object[] obj : results) {
					entityIds[idx++] = (Long) obj[0];
				}
				
				criteria = createCriteria(getResourceClass());
				criteria.add(Restrictions.in("id", entityIds));
		
				everyone.addAll((List<T>) criteria.list());
			}
			
			queryCache.put(new Element(key, everyone));
		}
		
		return (Collection<T>) queryCache.get(key).getObjectValue();
	}

	@Override
	@Transactional(readOnly=true)
	public Long getAssignedResourceCount(Principal principal,
			final String searchPattern, CriteriaConfiguration... configs) {

		
		StringBuffer keydata = new StringBuffer("searchAssignedResources;");
		keydata.append(getResourceClass().getCanonicalName());
		keydata.append(";");
		keydata.append(principal.getId());
		keydata.append(";");
		keydata.append(searchPattern);
		keydata.append(";");
		
		String key = keydata.toString();
		
		if (!queryCache.isElementInMemory(key)
				|| (queryCache.get(key) == null 
					|| queryCache.isExpired(queryCache.get(key)))) {
			Criteria criteria = createCriteria(getResourceClass());
			criteria.setProjection(Projections.property("id"));
			criteria.setResultTransformer(CriteriaSpecification.PROJECTION);
			if (StringUtils.isNotBlank(searchPattern)) {
				criteria.add(Restrictions.like("name", searchPattern));
			}
	
			for (CriteriaConfiguration c : configs) {
				c.configure(criteria);
			}
	
			criteria.add(Restrictions.eq("realm", principal.getRealm()));
			criteria = criteria.createCriteria("roles");
			criteria.add(Restrictions.eq("allUsers", true));
			
			List<?> ids = criteria.list();
			
			criteria = createCriteria(getResourceClass());
			criteria.setProjection(Projections.countDistinct("name"));
			criteria.setResultTransformer(CriteriaSpecification.PROJECTION);
			if (StringUtils.isNotBlank(searchPattern)) {
				criteria.add(Restrictions.like("name", searchPattern));
			}
	
			for (CriteriaConfiguration c : configs) {
				c.configure(criteria);
			}
			
			criteria.add(Restrictions.eq("realm", principal.getRealm()));
			if(ids.size() > 0) {
				criteria.add(Restrictions.not(Restrictions.in("id", ids)));
			}
			criteria = criteria.createCriteria("roles");
			criteria.add(Restrictions.eq("allUsers", false));
			criteria = criteria.createCriteria("principals");
			criteria.add(Restrictions.in("id", new Long[] { principal.getId() }));
			
			Long count = (Long) criteria.uniqueResult();
			
			queryCache.put(new Element(key, new Long(count + ids.size())));
		
		}

		return (Long) queryCache.get(key).getObjectValue();
	}

	@Override
	@Transactional(readOnly=true)
	public Long getAssignableResourceCount(Principal principal) {
		return getAssignedResourceCount(principal, "");
	}

	@SuppressWarnings("unchecked")
	@Override
	@Transactional(readOnly=true)
	public List<AssignableResource> getAllAssignableResources(
			List<Principal> principals) {

		StringBuffer keydata = new StringBuffer("getResourceByIdAndPrincipals;");
		keydata.append(getResourceClass().getCanonicalName());
		keydata.append(";");
		
		for(Principal p : principals) {
			keydata.append(p.getId());
			keydata.append(";");
		}
		
		String key = keydata.toString();
		
		if (!queryCache.isElementInMemory(key)
				|| (queryCache.get(key) == null 
					|| queryCache.isExpired(queryCache.get(key)))) {

			Set<Long> ids = new HashSet<Long>();
			for (Principal p : principals) {
				ids.add(p.getId());
			}
			Criteria crit = createCriteria(AssignableResource.class);
			crit.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
			crit = crit.createCriteria("roles");
			crit = crit.createCriteria("principals");
			crit.add(Restrictions.in("id", ids));
	
			queryCache.put(new Element(key, crit.list()));
		
		}
		
		return (List<AssignableResource>) queryCache.get(key).getObjectValue();
	}

	@SuppressWarnings("unchecked")
	@Override
	@Transactional(readOnly=true)
	public T getResourceByIdAndPrincipals(Long resourceId,
			List<Principal> principals) {

		StringBuffer keydata = new StringBuffer("getResourceByIdAndPrincipals;");
		keydata.append(getResourceClass().getCanonicalName());
		keydata.append(";");
		keydata.append(resourceId);
		keydata.append(";");
		
		for(Principal p : principals) {
			keydata.append(p.getId());
			keydata.append(";");
		}
		
		String key = keydata.toString();
		
		if (!queryCache.isElementInMemory(key)
				|| (queryCache.get(key) == null 
					|| queryCache.isExpired(queryCache.get(key)))) {
			
			Set<Long> ids = new HashSet<Long>();
			for (Principal p : principals) {
				ids.add(p.getId());
			}
			Criteria crit = createCriteria(getResourceClass());
			crit.add(Restrictions.eq("id", resourceId));
			crit.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
			crit = crit.createCriteria("roles");
			crit = crit.createCriteria("principals");
			crit.add(Restrictions.in("id", ids));
	
			queryCache.put(new Element(key, crit.uniqueResult()));
		}
		
		return (T) queryCache.get(key).getObjectValue();
	}

	@Transactional
	protected <K extends AssignableResourceSession<T>> K createResourceSession(
			T resource, Session session, K newSession) {

		queryCache.removeAll();
		
		newSession.setSession(session);
		newSession.setResource(resource);

		save(newSession);

		return newSession;
	}

	@Override
	@Transactional(readOnly=true)
	public T getResourceByName(String name, Realm realm) {
		return get("name", name, getResourceClass(), new DeletedDetachedCriteria(false), new RealmRestriction(realm));
	}

	@Override
	@Transactional(readOnly=true)
	public T getResourceByName(String name, Realm realm, boolean deleted) {
		return get("name", name, getResourceClass(), new DeletedDetachedCriteria(
				deleted), new RealmRestriction(realm));
	}

	@Override
	@Transactional(readOnly=true)
	public T getResourceById(Long id) {
		return get("id", id, getResourceClass());
	}

	@Override
	@Transactional
	public void deleteResource(T resource) throws ResourceChangeException {
		
		queryCache.removeAll();
		beforeDelete(resource);
		delete(resource);
		afterDelete(resource);
	}

	protected void beforeDelete(T resource) throws ResourceChangeException {
		
	}
	
	protected void afterDelete(T resource) throws ResourceChangeException {
		
	}
	
	protected void beforeSave(T resource, Map<String,String> properties) throws ResourceChangeException {
		
	}
	
	protected void afterSave(T resource, Map<String,String> properties) throws ResourceChangeException {
		
	}
	
	@Override
	@SafeVarargs
	@Transactional
	public final void saveResource(T resource, Map<String, String> properties, TransactionOperation<T>... ops) throws ResourceChangeException {

		queryCache.removeAll();
		
		beforeSave(resource, properties);
		for(TransactionOperation<T> op : ops) {
			op.beforeOperation(resource, properties);
		}
		for (Map.Entry<String, String> e : properties.entrySet()) {
			if (hasPropertyTemplate(e.getKey())) {
				setValue(resource, e.getKey(), e.getValue());
			}
		}
		save(resource);
		
		afterSave(resource, properties);
		for(TransactionOperation<T> op : ops) {
			op.afterOperation(resource, properties);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	@Transactional(readOnly=true)
	public List<T> getResources(Realm realm) {

		Criteria crit = createCriteria(getResourceClass());
		crit.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
		crit.setFetchMode("roles", FetchMode.SELECT);
		crit.add(Restrictions.eq("deleted", false));
		crit.add(Restrictions.eq("realm", realm));

		return (List<T>) crit.list();
	}

	@Override
	@SuppressWarnings("unchecked")
	@Transactional(readOnly=true)
	public List<T> allResources() {

		Criteria crit = createCriteria(getResourceClass());
		crit.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
		crit.setFetchMode("roles", FetchMode.SELECT);
		crit.add(Restrictions.eq("deleted", false));

		return (List<T>) crit.list();
	}

	@Override
	@Transactional(readOnly=true)
	public List<T> search(Realm realm, String searchPattern, int start,
			int length, ColumnSort[] sorting, CriteriaConfiguration... configs) {
		return super.search(getResourceClass(), "name", searchPattern, start,
				length, sorting, ArrayUtils.addAll(configs,
						new RoleSelectMode(), new RealmAndDefaultRealmCriteria(
								realm)));
	}
	
	
	@Override
	@Transactional(readOnly=true)
	public long allRealmsResourcesCount() {
		return getCount(getResourceClass(), new DeletedCriteria(false));
	}

	@Override
	@Transactional(readOnly=true)
	public long getResourceCount(Realm realm, String searchPattern,
			CriteriaConfiguration... configs) {
		return getCount(getResourceClass(), "name", searchPattern,
				ArrayUtils.addAll(configs, new RoleSelectMode(),
						new RealmAndDefaultRealmCriteria(realm)));
	}

	protected abstract Class<T> getResourceClass();

	class RoleSelectMode implements CriteriaConfiguration {

		@Override
		public void configure(Criteria criteria) {
			criteria.setFetchMode("roles", FetchMode.SELECT);
		}
	}

}
