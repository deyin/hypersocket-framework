/*******************************************************************************
 * Copyright (c) 2013 Hypersocket Limited.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package com.hypersocket.resource;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.Order;
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
import com.hypersocket.repository.CriteriaConfiguration;
import com.hypersocket.repository.DeletedCriteria;
import com.hypersocket.session.Session;
import com.hypersocket.tables.ColumnSort;
import com.hypersocket.tables.Sort;

@Repository
@Transactional
public abstract class AbstractAssignableResourceRepositoryImpl<T extends AssignableResource>
		extends ResourceTemplateRepositoryImpl implements
		AbstractAssignableResourceRepository<T> {

	@Autowired
	EntityResourcePropertyStore entityPropertyStore;
	
	@Override
	public List<T> getAssigedResources(List<Principal> principals) {
		return getAssignedResources(principals.toArray(new Principal[0]));
	}

	@Override
	protected ResourcePropertyStore getPropertyStore() {
		return entityPropertyStore;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public List<T> getAssignedResources(Principal... principals) {

		Set<Long> ids = new HashSet<Long>();
		for (Principal p : principals) {
			ids.add(p.getId());
		}
		Criteria crit = sessionFactory.getCurrentSession().createCriteria(
				getResourceClass());
		crit.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);

		crit = crit.createCriteria("roles");
		crit = crit.createCriteria("principals");
		crit.add(Restrictions.in("id", ids));

		return (List<T>) crit.list();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<T> searchAssignedResources(Principal principal,
			final String searchPattern, final int start, final int length,
			final ColumnSort[] sorting, CriteriaConfiguration... configs) {

		Criteria criteria = createCriteria(getResourceClass());
		if (!StringUtils.isEmpty(searchPattern)) {
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

		criteria.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
		criteria.setFirstResult(start);
		criteria.setMaxResults(length);

		criteria = criteria.createCriteria("roles");
		criteria = criteria.createCriteria("principals");
		criteria.add(Restrictions.in("id", new Long[] { principal.getId() }));

		List<T> res = (List<T>) criteria.list();
		return res;
	};

	@Override
	public Long getAssignedResourceCount(Principal principal,
			final String searchPattern, CriteriaConfiguration... configs) {

		Criteria criteria = createCriteria(getResourceClass());
		if (!StringUtils.isEmpty(searchPattern)) {
			criteria.add(Restrictions.like("name", searchPattern));
		}

		for (CriteriaConfiguration c : configs) {
			c.configure(criteria);
		}

		criteria.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
		criteria.setProjection(Projections.rowCount());
		criteria = criteria.createCriteria("roles");
		criteria = criteria.createCriteria("principals");
		criteria.add(Restrictions.in("id", new Long[] { principal.getId() }));

		return (Long) criteria.uniqueResult();
	}

	@Override
	public Long getAssignableResourceCount(Principal... principals) {

		Set<Long> ids = new HashSet<Long>();
		for (Principal p : principals) {
			ids.add(p.getId());
		}
		Criteria crit = sessionFactory.getCurrentSession().createCriteria(
				getResourceClass());
		crit.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
		crit.setProjection(Projections.rowCount());
		crit = crit.createCriteria("roles");
		crit = crit.createCriteria("principals");
		crit.add(Restrictions.in("id", ids));

		return (Long) crit.uniqueResult();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<AssignableResource> getAllAssignableResources(
			List<Principal> principals) {

		Set<Long> ids = new HashSet<Long>();
		for (Principal p : principals) {
			ids.add(p.getId());
		}
		Criteria crit = sessionFactory.getCurrentSession().createCriteria(
				AssignableResource.class);
		crit.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
		crit = crit.createCriteria("roles");
		crit = crit.createCriteria("principals");
		crit.add(Restrictions.in("id", ids));

		return (List<AssignableResource>) crit.list();
	}

	@SuppressWarnings("unchecked")
	@Override
	public T getResourceByIdAndPrincipals(Long resourceId,
			List<Principal> principals) {

		Set<Long> ids = new HashSet<Long>();
		for (Principal p : principals) {
			ids.add(p.getId());
		}
		Criteria crit = sessionFactory.getCurrentSession().createCriteria(
				getResourceClass());
		crit.add(Restrictions.eq("id", resourceId));
		crit.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
		crit = crit.createCriteria("roles");
		crit = crit.createCriteria("principals");
		crit.add(Restrictions.in("id", ids));

		return (T) crit.uniqueResult();
	}

	protected <K extends AssignableResourceSession<T>> K createResourceSession(
			T resource, Session session, K newSession) {

		newSession.setSession(session);
		newSession.setResource(resource);

		save(newSession);

		return newSession;
	}

	@Override
	public T getResourceByName(String name) {
		return get("name", name, getResourceClass(), new DeletedCriteria(false));
	}

	@Override
	public T getResourceByName(String name, boolean deleted) {
		return get("name", name, getResourceClass(), new DeletedCriteria(
				deleted));
	}

	@Override
	public T getResourceById(Long id) {
		return get("id", id, getResourceClass());
	}

	@Override
	public void deleteResource(T resource) throws ResourceChangeException {
		delete(resource);
	}

	@Override
	public void saveResource(T resource, Map<String,String> properties) {
		
		for(Map.Entry<String,String> e : properties.entrySet()) {
			setValue(resource, e.getKey(), e.getValue());
		}
		save(resource);
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<T> getResources(Realm realm) {

		Criteria crit = sessionFactory.getCurrentSession().createCriteria(
				getResourceClass());
		crit.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
		crit.setFetchMode("roles", FetchMode.SELECT);
		crit.add(Restrictions.eq("deleted", false));
		crit.add(Restrictions.eq("realm", realm));
		
		return (List<T>) crit.list();
	}

	@Override
	public List<T> search(Realm realm, String searchPattern, int start,
			int length, ColumnSort[] sorting, CriteriaConfiguration... configs) {
		return super.search(getResourceClass(), "name", searchPattern, start,
				length, sorting, ArrayUtils.addAll(configs,
						new RoleSelectMode(),
						new RealmAndDefaultRealmCriteria(realm)));
	}

	@Override
	public long getResourceCount(Realm realm, String searchPattern,
			CriteriaConfiguration... configs) {
		return getCount(getResourceClass(), "name", searchPattern,
				ArrayUtils.addAll(configs, 
						new RoleSelectMode(),
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
