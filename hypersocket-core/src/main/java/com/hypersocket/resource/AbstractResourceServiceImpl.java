package com.hypersocket.resource;

import java.lang.reflect.Field;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.hypersocket.auth.AuthenticatedServiceImpl;
import com.hypersocket.permissions.AccessDeniedException;
import com.hypersocket.permissions.PermissionType;
import com.hypersocket.realm.Realm;
import com.hypersocket.realm.RealmService;
import com.hypersocket.tables.ColumnSort;

@Repository
public abstract class AbstractResourceServiceImpl<T extends RealmResource>
		extends AuthenticatedServiceImpl implements AbstractResourceService<T> {

	static Logger log = LoggerFactory
			.getLogger(AbstractAssignableResourceRepositoryImpl.class);

	static final String RESOURCE_BUNDLE = "AssignableResourceService";

	@Autowired
	RealmService realmService;

	protected abstract AbstractResourceRepository<T> getRepository();

	protected abstract String getResourceBundle();

	public abstract Class<? extends PermissionType> getPermissionType();

	protected PermissionType getUpdatePermission() {
		return getPermission("UPDATE");
	}

	protected PermissionType getCreatePermission() {
		return getPermission("CREATE");
	}

	protected PermissionType getDeletePermission() {
		return getPermission("DELETE");
	}

	protected PermissionType getReadPermission() {
		return getPermission("READ");
	}

	protected PermissionType getPermission(String name) {
		try {
			Field f = getPermissionType().getField(name);

			return (PermissionType) f.get(null);
		} catch (Exception e) {
			throw new IllegalStateException(
					"Could not resolve update permission on PermissionType "
							+ getPermissionType().getName());
		}
	}

	@Override
	public void createResource(T resource) throws ResourceCreationException,
			AccessDeniedException {

		assertPermission(getCreatePermission());

		try {
			getResourceByName(resource.getName());
			ResourceCreationException ex = new ResourceCreationException(
					RESOURCE_BUNDLE, "generic.alreadyExists.error",
					resource.getName());
			fireResourceCreationEvent(resource, ex);
			throw ex;
		} catch (ResourceNotFoundException ex) {
			try {
				// Make sure any resources in default realm
				// (system) are available across all realms
				if (getCurrentRealm().isSystem()) {
					resource.setRealm(null);
				} else {
					resource.setRealm(getCurrentRealm());
				}

				getRepository().saveResource(resource);
				fireResourceCreationEvent(resource);
			} catch (Throwable t) {
				log.error("Failed to create resource", t);
				fireResourceCreationEvent(resource, t);
				if (t instanceof ResourceCreationException) {
					throw (ResourceCreationException) t;
				} else {
					throw new ResourceCreationException(RESOURCE_BUNDLE,
							"generic.create.error", t.getMessage());
				}
			}
		}

	}

	protected abstract void fireResourceCreationEvent(T resource);

	protected abstract void fireResourceCreationEvent(T resource, Throwable t);

	public void updateResource(T resource) throws ResourceChangeException,
			AccessDeniedException {
		assertPermission(getUpdatePermission());

		try {
			getRepository().saveResource(resource);
			fireResourceUpdateEvent(resource);
		} catch (Throwable t) {
			fireResourceUpdateEvent(resource, t);
			if (t instanceof ResourceChangeException) {
				throw (ResourceChangeException) t;
			} else {
				throw new ResourceChangeException(RESOURCE_BUNDLE,
						"generic.update.error", t.getMessage());
			}
		}
	}

	protected abstract void fireResourceUpdateEvent(T resource);

	protected abstract void fireResourceUpdateEvent(T resource, Throwable t);

	@Override
	public void deleteResource(T resource) throws ResourceChangeException,
			AccessDeniedException {

		assertPermission(getDeletePermission());

		try {
			getRepository().deleteResource(resource);
			fireResourceDeletionEvent(resource);
		} catch (Throwable t) {
			fireResourceDeletionEvent(resource, t);
			if (t instanceof ResourceChangeException) {
				throw (ResourceChangeException) t;
			} else {
				throw new ResourceChangeException(RESOURCE_BUNDLE,
						"generic.delete.error", t.getMessage());
			}
		}

	}

	protected abstract void fireResourceDeletionEvent(T resource);

	protected abstract void fireResourceDeletionEvent(T resource, Throwable t);

	@Override
	public List<T> getResources(Realm realm) throws AccessDeniedException {

		assertPermission(getReadPermission());

		return getRepository().getResources(realm);

	}

	@Override
	public List<T> searchResources(Realm realm, String search, int start,
			int length, ColumnSort[] sorting) throws AccessDeniedException {

		assertPermission(getReadPermission());

		return getRepository().search(realm, search, start, length, sorting);
	}

	@Override
	public long getResourceCount(Realm realm, String search)
			throws AccessDeniedException {

		assertPermission(getReadPermission());

		return getRepository().getResourceCount(realm, search);
	}

	@Override
	public List<T> getResources() {
		return getRepository().getResources();
	}

	@Override
	public T getResourceByName(String name) throws ResourceNotFoundException {
		T resource = getRepository().getResourceByName(name);
		if (resource == null) {
			throw new ResourceNotFoundException(getResourceBundle(),
					"error.invalidResourceName", name);
		}
		return resource;
	}

	@Override
	public T getResourceById(Long id) throws ResourceNotFoundException {
		T resource = getRepository().getResourceById(id);
		if (resource == null) {
			throw new ResourceNotFoundException(getResourceBundle(),
					"error.invalidResourceId", id);
		}
		return resource;
	}
}