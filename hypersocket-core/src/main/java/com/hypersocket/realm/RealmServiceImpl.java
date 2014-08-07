/*******************************************************************************
 * Copyright (c) 2013 Hypersocket Limited.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package com.hypersocket.realm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hypersocket.auth.AuthenticatedServiceImpl;
import com.hypersocket.events.EventService;
import com.hypersocket.permissions.AccessDeniedException;
import com.hypersocket.permissions.PermissionCategory;
import com.hypersocket.permissions.PermissionService;
import com.hypersocket.permissions.SystemPermission;
import com.hypersocket.properties.PropertyCategory;
import com.hypersocket.realm.events.GroupCreatedEvent;
import com.hypersocket.realm.events.GroupDeletedEvent;
import com.hypersocket.realm.events.GroupUpdatedEvent;
import com.hypersocket.realm.events.ProfileUpdatedEvent;
import com.hypersocket.realm.events.RealmCreatedEvent;
import com.hypersocket.realm.events.RealmDeletedEvent;
import com.hypersocket.realm.events.RealmUpdatedEvent;
import com.hypersocket.realm.events.UserCreatedEvent;
import com.hypersocket.realm.events.UserDeletedEvent;
import com.hypersocket.realm.events.UserUpdatedEvent;
import com.hypersocket.resource.ResourceChangeException;
import com.hypersocket.resource.ResourceCreationException;
import com.hypersocket.resource.ResourceNotFoundException;
import com.hypersocket.tables.ColumnSort;

@Service
public class RealmServiceImpl extends AuthenticatedServiceImpl implements
		RealmService {

	Map<String, RealmProvider> providersByModule = new HashMap<String, RealmProvider>();

	@Autowired
	RealmRepository realmRepository;

	@Autowired
	PermissionService permissionService;

	@Autowired
	EventService eventService; 
	
	Principal systemPrincipal;

	@PostConstruct
	private void postConstruct() {

		PermissionCategory cat = permissionService.registerPermissionCategory(
				RESOURCE_BUNDLE, "category.realms");

		for (RealmPermission p : RealmPermission.values()) {
			permissionService.registerPermission(p.getResourceKey(), cat);
		}

		cat = permissionService.registerPermissionCategory(RESOURCE_BUNDLE,
				"category.acl");

		for (UserPermission p : UserPermission.values()) {
			permissionService.registerPermission(p.getResourceKey(), cat);
		}
		for (ProfilePermission p : ProfilePermission.values()) {
			permissionService.registerPermission(p.getResourceKey(), cat);
		}
		for (GroupPermission p : GroupPermission.values()) {
			permissionService.registerPermission(p.getResourceKey(), cat);
		}
		for (RolePermission p : RolePermission.values()) {
			permissionService.registerPermission(p.getResourceKey(), cat);
		}
		
		eventService.registerEvent(RealmCreatedEvent.class, RESOURCE_BUNDLE);
		eventService.registerEvent(RealmUpdatedEvent.class, RESOURCE_BUNDLE);
		eventService.registerEvent(RealmDeletedEvent.class, RESOURCE_BUNDLE);
		
		eventService.registerEvent(UserCreatedEvent.class, RESOURCE_BUNDLE);
		eventService.registerEvent(UserUpdatedEvent.class, RESOURCE_BUNDLE);
		eventService.registerEvent(UserDeletedEvent.class, RESOURCE_BUNDLE);
		
		eventService.registerEvent(GroupCreatedEvent.class, RESOURCE_BUNDLE);
		eventService.registerEvent(GroupUpdatedEvent.class, RESOURCE_BUNDLE);
		eventService.registerEvent(GroupDeletedEvent.class, RESOURCE_BUNDLE);
	}

	@Override
	public List<RealmProvider> getProviders() throws AccessDeniedException {

		assertPermission(RealmPermission.READ);

		return new ArrayList<RealmProvider>(providersByModule.values());
	}

	@Override
	public RealmProvider getProviderForRealm(Realm realm) {
		return getProviderForRealm(realm.getResourceCategory());
	}

	@Override
	public RealmProvider getProviderForRealm(String module) {
		if (!providersByModule.containsKey(module))
			throw new IllegalArgumentException(
					"No provider available for realm module " + module);
		return providersByModule.get(module);
	}

	protected boolean hasProviderForRealm(Realm realm) {
		return providersByModule.containsKey(realm.getResourceCategory());
	}

	@Override
	public List<Principal> allUsers(Realm realm) throws AccessDeniedException {

		assertPermission(UserPermission.READ);

		return allPrincipals(realm, PrincipalType.USER);
	}

	@Override
	public List<Principal> allGroups(Realm realm) throws AccessDeniedException {

		assertPermission(GroupPermission.READ);

		return allPrincipals(realm, PrincipalType.GROUP);
	}

	protected List<Principal> allPrincipals(Realm realm, PrincipalType... types) {
		if (types.length == 0) {
			types = PrincipalType.ALL_TYPES;
		}
		return getProviderForRealm(realm).allPrincipals(realm, types);
	}

	@Override
	public void registerRealmProvider(RealmProvider provider) {
		providersByModule.put(provider.getModule(), provider);
	}

	@Override
	public Realm getRealmByName(String realm) throws AccessDeniedException {
		return realmRepository.getRealmByName(realm);
	}

	@Override
	public String getRealmProperty(Realm realm, String resourceKey) {
		
		RealmProvider provider = getProviderForRealm(realm);
		return provider.getValue(realm, resourceKey);
	}
	
	@Override
	public int getRealmPropertyInt(Realm realm, String resourceKey) {
		return Integer.parseInt(getRealmProperty(realm, resourceKey));
	}
	
	@Override
	public Realm getRealmByHost(String host) {
		for (Realm r : allRealms()) {
			RealmProvider provider = getProviderForRealm(r);
			String realmHost = provider.getValue(r, "realm.host");
			if (realmHost != null && !"".equals(realmHost)) {
				if (realmHost.equalsIgnoreCase(host)) {
					return r;
				}
			}
		}
		return null;
	}

	@Override
	public Realm getRealmById(Long id) throws AccessDeniedException {

		assertAnyPermission(RealmPermission.READ,
				SystemPermission.SYSTEM_ADMINISTRATION);

		return realmRepository.getRealmById(id);
	}

	@Override
	public Principal createUser(Realm realm, String username,
			Map<String, String> properties, List<Principal> principals)
			throws ResourceCreationException, AccessDeniedException {

		RealmProvider provider = getProviderForRealm(realm);

		try {

			assertPermission(UserPermission.CREATE);

			Principal principal = provider.createUser(realm, username,
					properties, principals);

			eventService
					.publishEvent(new UserCreatedEvent(this,
							getCurrentSession(), realm, provider, principal,
							principals,
							properties));

			return principal;
		} catch (AccessDeniedException e) {
			eventService.publishEvent(new UserCreatedEvent(this, e,
					getCurrentSession(), realm, provider, username, properties,
					principals));
			throw e;
		} catch (ResourceCreationException e) {
			eventService.publishEvent(new UserCreatedEvent(this, e,
					getCurrentSession(), realm, provider, username, properties,
					principals));
			throw e;
		} catch (Exception e) {
			eventService.publishEvent(new UserCreatedEvent(this, e,
					getCurrentSession(), realm, provider, username, properties,
					principals));
			throw new ResourceCreationException(RESOURCE_BUNDLE,
					"error.unexpectedError", e.getMessage());
		}

	}

	@Override
	public Principal updateUser(Realm realm, 
			Principal user, String username,
			Map<String, String> properties, List<Principal> principals)
			throws ResourceChangeException, AccessDeniedException {

		final RealmProvider provider = getProviderForRealm(realm);

		try {

			assertPermission(UserPermission.UPDATE);

			Principal principal = provider.updateUser(realm, user, username,
					properties, principals);

			eventService
				.publishEvent(new UserUpdatedEvent(this,
						getCurrentSession(), realm, provider, principal,
						principals,
						properties));
			

			return principal;
		} catch (AccessDeniedException e) {
			eventService.publishEvent(new UserUpdatedEvent(this, e,
					getCurrentSession(), realm, provider, username, properties,
					principals));
			throw e;
		} catch (ResourceChangeException e) {
			eventService.publishEvent(new UserUpdatedEvent(this, e,
					getCurrentSession(), realm, provider, username, properties,
					principals));
			throw e;
		} catch (Exception e) {
			eventService.publishEvent(new UserUpdatedEvent(this, e,
					getCurrentSession(), realm, provider, username, properties,
					principals));
			throw new ResourceChangeException(RESOURCE_BUNDLE,
					"error.unexpectedError", e.getMessage());
		}
	}

	@Override
	public boolean verifyPassword(Principal principal, char[] password) {
		return getProviderForRealm(principal.getRealm()).verifyPassword(
				principal, password);
	}

	@Override
	public Principal getPrincipalByName(Realm realm, String principalName,
			PrincipalType... type) {
		if (type.length == 0) {
			type = PrincipalType.ALL_TYPES;
		}
		return getProviderForRealm(realm).getPrincipalByName(principalName,
				realm, type);
	}

//	@Override
//	public Set<Principal> getPrincipalsByProperty(String propertyName,
//			String propertyValue) {
//
//		Set<Principal> ret = new HashSet<Principal>();
//		for (Realm r : allRealms()) {
//			ret.addAll(getProviderForRealm(r).getPrincipalsByProperty(
//					propertyName, propertyValue));
//		}
//		return ret;
//	}

	@Override
	public void deleteRealm(String name) throws ResourceChangeException,
			ResourceNotFoundException, AccessDeniedException {

		assertPermission(RealmPermission.DELETE);

		Realm realm = getRealmByName(name);

		if (realm == null) {
			ResourceNotFoundException ex = new ResourceNotFoundException(
					RESOURCE_BUNDLE, "error.invalidRealm", name);
			eventService.publishEvent(new RealmDeletedEvent(this, ex,
					getCurrentSession(), name));
			throw ex;
		}

		deleteRealm(realm);
	}

	private boolean hasSystemAdministrator(Realm r) {

		Set<Principal> sysAdmins = permissionService
				.getUsersWithPermissions(SystemPermission.SYSTEM_ADMINISTRATION);
		for (Principal p : sysAdmins) {
			if (p.getRealm().equals(r)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public List<Realm> allRealms() {
		return filterRealms(null);
	}

	private List<Realm> filterRealms(Class<? extends RealmProvider> clz) {

		List<Realm> realms = realmRepository.allRealms();
		List<Realm> ret = new ArrayList<Realm>(realms);
		for (Realm r : realms) {
			if (!hasProviderForRealm(r)) {
				ret.remove(r);
			}
			if (clz != null
					&& !clz.isAssignableFrom(getProviderForRealm(r).getClass())) {
				ret.remove(r);
			}
			;
		}
		return ret;
	}

	@Override
	public List<Realm> allRealms(Class<? extends RealmProvider> clz) {
		return filterRealms(clz);
	}

	@Override
	public void changePassword(Principal principal, String oldPassword,
			String newPassword) throws ResourceCreationException,
			ResourceChangeException {

		RealmProvider provider = getProviderForRealm(principal.getRealm());
		if (provider.isReadOnly()) {
			throw new ResourceCreationException(RESOURCE_BUNDLE,
					"error.realmIsReadOnly");
		}

		if (!verifyPassword(principal, oldPassword.toCharArray())) {
			throw new ResourceChangeException(RESOURCE_BUNDLE,
					"error.invalidPassword");
		}
		provider.setPassword(principal, newPassword.toCharArray(), false);
	}

	@Override
	public void setPassword(Principal principal, String password,
			boolean forceChangeAtNextLogon) throws ResourceCreationException {

		RealmProvider provider = getProviderForRealm(principal.getRealm());
		if (provider.isReadOnly()) {
			throw new ResourceCreationException(RESOURCE_BUNDLE,
					"error.realmIsReadOnly");
		}

		provider.setPassword(principal, password.toCharArray(),
				forceChangeAtNextLogon);
	}
	
	@Override
	public boolean isReadOnly(Realm realm) {
		
		RealmProvider provider = getProviderForRealm(realm);
		return provider.isReadOnly();
	}

	@Override
	public Principal getSystemPrincipal() {
		if (systemPrincipal == null) {
			systemPrincipal = getPrincipalByName(
					realmRepository.getRealmByName(SYSTEM_REALM),
					SYSTEM_PRINCIPAL, PrincipalType.SYSTEM);
		}
		return systemPrincipal;
	}

	@Override
	public Realm createRealm(String name, String module,
			Map<String, String> properties) throws AccessDeniedException,
			ResourceCreationException {

		try {
			assertPermission(RealmPermission.CREATE);

			if (realmRepository.getRealmByName(name) != null) {
				ResourceCreationException ex = new ResourceCreationException(
						RESOURCE_BUNDLE, "error.nameAlreadyExists", name);
				eventService.publishEvent(new RealmCreatedEvent(this, ex,
						getCurrentSession(), name));
				throw ex;
			}

			Realm realm = realmRepository.createRealm(name, module, properties,
					getProviderForRealm(module));

			eventService.publishEvent(new RealmCreatedEvent(this,
					getCurrentSession(), realm));

			return realm;
		} catch (AccessDeniedException e) {
			eventService.publishEvent(new RealmCreatedEvent(this, e,
					getCurrentSession(), name));
			throw e;
		} catch (ResourceCreationException e) {
			eventService.publishEvent(new RealmCreatedEvent(this, e,
					getCurrentSession(), name));
			throw e;
		} catch (Throwable t) {
			eventService.publishEvent(new RealmCreatedEvent(this, t,
					getCurrentSession(), name));
			throw new ResourceCreationException(RESOURCE_BUNDLE,
					"error.genericError", name, t.getMessage());
		}
	}

	@Override
	public Realm updateRealm(Realm realm, String name,
			Map<String, String> properties) throws AccessDeniedException,
			ResourceChangeException {

		try {

			assertPermission(RealmPermission.UPDATE);

			if (!realm.getName().equals(name)) {
				if (realmRepository.getRealmByName(name) != null) {
					throw new ResourceChangeException(RESOURCE_BUNDLE,
							"error.nameAlreadyExists", name);
				}
			}

			String oldName = realm.getName();

			realm.setName(name);

			realm = realmRepository.saveRealm(realm, properties,
					getProviderForRealm(realm));

			eventService.publishEvent(new RealmUpdatedEvent(this,
					getCurrentSession(), oldName, realmRepository
							.getRealmById(realm.getId())));
		} catch (AccessDeniedException e) {
			eventService.publishEvent(new RealmUpdatedEvent(this, e,
					getCurrentSession(), name));
			throw e;
		} catch (ResourceChangeException e) {
			eventService.publishEvent(new RealmUpdatedEvent(this, e,
					getCurrentSession(), name));
			throw e;
		} catch (Throwable t) {
			eventService.publishEvent(new RealmUpdatedEvent(this, t,
					getCurrentSession(), name));
			throw new ResourceChangeException(RESOURCE_BUNDLE,
					"error.unexpectedError", t);
		}
		return realm;
	}

	@Override
	public void deleteRealm(Realm realm) throws AccessDeniedException,
			ResourceChangeException {

		try {

			assertPermission(RealmPermission.DELETE);

			List<Realm> realms = realmRepository.allRealms();
			if (realms.size() == 1) {
				throw new ResourceChangeException(RESOURCE_BUNDLE,
						"error.zeroRealms", realm.getName());
			}

			realms.remove(realm);

			if (hasSystemAdministrator(realm)) {
				boolean systemAdministratorPresent = false;

				for (Realm r : realms) {
					if (hasSystemAdministrator(r)) {
						systemAdministratorPresent = true;
						break;
					}
				}

				if (!systemAdministratorPresent) {
					throw new ResourceChangeException(RESOURCE_BUNDLE,
							"error.zeroSysAdmins", realm.getName());
				}
			}

			realmRepository.delete(realm);

			eventService.publishEvent(new RealmDeletedEvent(this,
					getCurrentSession(), realm.getName()));

		} catch (AccessDeniedException e) {
			eventService.publishEvent(new RealmDeletedEvent(this, e,
					getCurrentSession(), realm.getName()));
			throw e;
		} catch (ResourceChangeException e) {
			eventService.publishEvent(new RealmDeletedEvent(this, e,
					getCurrentSession(), realm.getName()));
			throw e;
		} catch (Throwable t) {
			eventService.publishEvent(new RealmDeletedEvent(this, t,
					getCurrentSession(), realm.getName()));
			throw new ResourceChangeException(RESOURCE_BUNDLE,
					"error.unexpectedError", t);
		}
	}

	@Override
	public Collection<PropertyCategory> getRealmPropertyTemplates(Realm realm)
			throws AccessDeniedException {

		assertPermission(RealmPermission.READ);

		RealmProvider provider = getProviderForRealm(realm);

		return provider.getRealmProperties(realm);

	}

	@Override
	public Collection<PropertyCategory> getRealmPropertyTemplates(String module)
			throws AccessDeniedException {
		assertPermission(RealmPermission.READ);

		RealmProvider provider = getProviderForRealm(module);

		return provider.getRealmProperties(null);
	}

	@Override
	public Principal getPrincipalById(Realm realm, Long id,
			PrincipalType... type) throws AccessDeniedException {

		assertPermission(RealmPermission.READ);

		if (type.length == 0) {
			type = PrincipalType.ALL_TYPES;
		}
		return getProviderForRealm(realm).getPrincipalById(id, realm, type);
	}

	@Override
	public boolean requiresPasswordChange(Principal principal, Realm realm) {
		return getProviderForRealm(realm).requiresPasswordChange(principal);
	}

	@Override
	public Principal createGroup(Realm realm, String name,
			List<Principal> principals) throws ResourceCreationException,
			AccessDeniedException {

		RealmProvider provider = getProviderForRealm(realm);

		try {

			assertAnyPermission(GroupPermission.CREATE);

			Principal principal = provider.createGroup(realm, name, principals);

			eventService
					.publishEvent(new GroupCreatedEvent(this,
							getCurrentSession(), realm, provider, principal,
							principals,
							new HashMap<String,String>()));

			return principal;
		} catch (AccessDeniedException e) {
			eventService.publishEvent(new GroupCreatedEvent(this, e,
					getCurrentSession(), realm, provider, name, principals));
			throw e;
		} catch (ResourceCreationException e) {
			eventService.publishEvent(new GroupCreatedEvent(this, e,
					getCurrentSession(), realm, provider, name, principals));
			throw e;
		} catch (Exception e) {
			eventService.publishEvent(new GroupCreatedEvent(this, e,
					getCurrentSession(), realm, provider, name, principals));
			throw new ResourceCreationException(RESOURCE_BUNDLE,
					"createGroup.unexpectedError", e.getMessage());
		}
	}

	@Override
	public Principal updateGroup(Realm realm, Principal group, String name,
			List<Principal> principals) throws ResourceChangeException,
			AccessDeniedException {

		RealmProvider provider = getProviderForRealm(realm);

		try {

			assertPermission(GroupPermission.UPDATE);

			Principal principal = provider.updateGroup(realm, group, name,
					principals);

			eventService
					.publishEvent(new GroupUpdatedEvent(this,
							getCurrentSession(), realm, provider, principal,
							principals,
							new HashMap<String,String>()));

			return principal;
		} catch (AccessDeniedException e) {
			eventService.publishEvent(new GroupUpdatedEvent(this, e,
					getCurrentSession(), realm, provider, name, principals));
			throw e;
		} catch (ResourceChangeException e) {
			eventService.publishEvent(new GroupUpdatedEvent(this, e,
					getCurrentSession(), realm, provider, name, principals));
			throw e;
		} catch (Exception e) {
			eventService.publishEvent(new GroupUpdatedEvent(this, e,
					getCurrentSession(), realm, provider, name, principals));
			throw new ResourceChangeException(RESOURCE_BUNDLE,
					"groupUser.unexpectedError", e.getMessage());
		}

	}

	@Override
	public void deleteGroup(Realm realm, Principal group)
			throws ResourceChangeException, AccessDeniedException {

		RealmProvider provider = getProviderForRealm(realm);

		try {
			assertPermission(GroupPermission.DELETE);

			provider.deleteGroup(group);

			eventService.publishEvent(new GroupDeletedEvent(this,
					getCurrentSession(), realm, provider, group, new HashMap<String,String>()));

		} catch (AccessDeniedException e) {
			eventService.publishEvent(new GroupDeletedEvent(this, e,
					getCurrentSession(), realm, provider, group
							.getPrincipalName()));
			throw e;
		} catch (ResourceChangeException e) {
			eventService.publishEvent(new GroupDeletedEvent(this, e,
					getCurrentSession(), realm, provider, group
							.getPrincipalName()));
			throw e;
		} catch (Throwable e) {
			eventService.publishEvent(new GroupDeletedEvent(this, e,
					getCurrentSession(), realm, provider, group
							.getPrincipalName()));
			throw new ResourceChangeException(RESOURCE_BUNDLE,
					"deleteGroup.unexpectedError", e.getMessage());
		}
	}

	@Override
	public void deleteUser(Realm realm, Principal user)
			throws ResourceChangeException, AccessDeniedException {

		RealmProvider provider = getProviderForRealm(realm);

		try {
			assertPermission(UserPermission.DELETE);

			provider.deleteUser(user);

			eventService.publishEvent(new UserDeletedEvent(this,
					getCurrentSession(), realm, provider, user));

		} catch (AccessDeniedException e) {
			eventService.publishEvent(new UserDeletedEvent(this, e,
					getCurrentSession(), realm, provider, user
							.getPrincipalName()));
			throw e;
		} catch (ResourceChangeException e) {
			eventService.publishEvent(new UserDeletedEvent(this, e,
					getCurrentSession(), realm, provider, user
							.getPrincipalName()));
			throw e;
		} catch (Throwable e) {
			eventService.publishEvent(new UserDeletedEvent(this, e,
					getCurrentSession(), realm, provider, user
							.getPrincipalName()));
			throw new ResourceChangeException(RESOURCE_BUNDLE,
					"deleteUser.unexpectedError", e.getMessage());
		}

	}
	
	@Override
	public String getPrincipalAddress(Principal principal, MediaType type) throws MediaNotFoundException {
		
		RealmProvider provider = getProviderForRealm(principal.getRealm());
		return provider.getAddress(principal, type);
	}

	@Override
	public Collection<PropertyCategory> getUserPropertyTemplates(
			Principal principal) throws AccessDeniedException {

		assertAnyPermission(UserPermission.READ, ProfilePermission.READ);

		RealmProvider provider = getProviderForRealm(principal.getRealm());

		return provider.getUserProperties(principal);
	}

	@Override
	public Collection<PropertyCategory> getUserPropertyTemplates(String module)
			throws AccessDeniedException {

		assertAnyPermission(UserPermission.READ, ProfilePermission.READ);

		RealmProvider provider = getProviderForRealm(module);

		return provider.getUserProperties(null);
	}

	@Override
	public Collection<PropertyCategory> getGroupPropertyTemplates(String module)
			throws AccessDeniedException {

		assertPermission(GroupPermission.READ);

		RealmProvider provider = getProviderForRealm(module);

		return provider.getGroupProperties(null);
	}

	@Override
	public List<Principal> getAssociatedPrincipals(Principal principal) {

		List<Principal> result = getProviderForRealm(principal.getRealm())
				.getAssociatedPrincipals(principal);
		if (!result.contains(principal)) {
			result.add(principal);
		}
		return result;
	}

	@Override
	public List<Principal> getAssociatedPrincipals(Principal principal,
			PrincipalType type) {
		return getProviderForRealm(principal.getRealm())
				.getAssociatedPrincipals(principal, type);
	}

	@Override
	public List<?> getPrincipals(Realm realm, PrincipalType type,
			String searchPattern, int start, int length, ColumnSort[] sorting) throws AccessDeniedException {
		
		assertAnyPermission(UserPermission.READ);
		return getProviderForRealm(realm).getPrincipals(realm, type,
				searchPattern, start, length, sorting);
	}

	@Override
	public Long getPrincipalCount(Realm realm, PrincipalType type,
			String searchPattern) {
		return getProviderForRealm(realm).getPrincipalCount(realm, type,
				searchPattern);
	}

	@Override
	public boolean findUniquePrincipal(String user) {

		int found = 0;
		for (Realm r : allRealms()) {
			Principal p = getPrincipalByName(r, user, PrincipalType.USER);
			if (p != null) {
				found++;
			}
		}
		return found == 1;
	}

	@Override
	public Principal getUniquePrincipal(String username)
			throws ResourceNotFoundException {
		int found = 0;
		Principal ret = null;
		for (Realm r : allRealms()) {
			Principal p = getPrincipalByName(r, username, PrincipalType.USER);
			if (p != null) {
				ret = p;
				found++;
			}
		}
		if (found != 1) {
			throw new ResourceNotFoundException(RESOURCE_BUNDLE,
					"principal.notFound");
		}
		return ret;
	}

	@Override
	public List<Realm> getRealms(String searchPattern, int start, int length,
			ColumnSort[] sorting) throws AccessDeniedException {

		assertPermission(RealmPermission.READ);

		return realmRepository.searchRealms(searchPattern, start, length,
				sorting);
	}

	@Override
	public Long getRealmCount(String searchPattern)
			throws AccessDeniedException {

		assertPermission(RealmPermission.READ);

		return realmRepository.countRealms(searchPattern);
	}

	@Override
	public void updateProfile(Realm realm, Principal principal,
			Map<String, String> properties) throws AccessDeniedException {

		assertPermission(ProfilePermission.UPDATE);

		RealmProvider provider = getProviderForRealm(realm);

		List<Principal> assosiated = provider
				.getAssociatedPrincipals(principal);
		try {
			principal = provider.updateUser(realm, principal,
					principal.getPrincipalName(), properties, assosiated);

			eventService
					.publishEvent(new ProfileUpdatedEvent(this,
							getCurrentSession(), realm, provider, principal,
							assosiated,
							properties));
		} catch (ResourceChangeException e) {

			eventService.publishEvent(new ProfileUpdatedEvent(this, e,
					getCurrentSession(), realm, provider, principal
							.getPrincipalName(), properties, assosiated));
		}

	}

	@Override
	public String getPrincipalDescription(Principal principal) {
		
		RealmProvider provider = getProviderForRealm(principal.getRealm());
		
		return provider.getPrincipalDescription(principal);
	}

}
