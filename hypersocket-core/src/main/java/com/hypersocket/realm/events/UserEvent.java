package com.hypersocket.realm.events;

import java.util.List;
import java.util.Map;

import com.hypersocket.events.CommonAttributes;
import com.hypersocket.realm.Principal;
import com.hypersocket.realm.Realm;
import com.hypersocket.realm.RealmProvider;
import com.hypersocket.session.Session;

public abstract class UserEvent extends RealmEvent {

	private static final long serialVersionUID = -4273120819815237950L;

	public static final String ATTR_PRINCIPAL_NAME = CommonAttributes.ATTR_PRINCIPAL_NAME;
	public static final String ATTR_ASSOCIATED_PRINCIPALS = "attr.associatedPrincipals";

	private Principal principal;

	public UserEvent(Object source, String resourceKey, Session session,
			Realm realm, RealmProvider provider, Principal principal) {
		super(source, resourceKey, true, session, realm);
		this.principal = principal;
		addAttribute(ATTR_PRINCIPAL_NAME, principal.getName());
	}

	public UserEvent(Object source, String resourceKey, Session session,
			Realm realm, RealmProvider provider, Principal principal,
			List<Principal> associatedPrincipals,
			Map<String, String> properties) {
		super(source, resourceKey, true, session, realm);
		this.principal = principal;
		addAttribute(ATTR_PRINCIPAL_NAME, principal.getName());
		addAssociatedPrincipals(associatedPrincipals);
		for (String prop : properties.keySet()) {
			addAttribute(prop, properties.get(prop));
		}
	}

	public UserEvent(Object source, String resourceKey, Throwable e,
			Session session, Realm realmName, RealmProvider provider,
			String principalName) {
		super(source, resourceKey, e, session, realmName);
		addAttribute(ATTR_PRINCIPAL_NAME, principalName);
	}

	public UserEvent(Object source, String resourceKey, Throwable e,
			Session session, Realm realmName, RealmProvider provider,
			String principalName, Map<String, String> properties,
			List<Principal> associatedPrincipals) {
		super(source, resourceKey, e, session, realmName);
		addAttribute(ATTR_PRINCIPAL_NAME, principalName);
		addAssociatedPrincipals(associatedPrincipals);
		for (String prop : properties.keySet()) {
			addAttribute(prop, properties.get(prop));
		}
	}

	private void addAssociatedPrincipals(List<Principal> associatedPrincipals) {
		StringBuffer buf = new StringBuffer();
		for (Principal p : associatedPrincipals) {
			if (buf.length() > 0) {
				buf.append(',');
			}
			buf.append(p.getPrincipalName());
		}
		addAttribute(ATTR_ASSOCIATED_PRINCIPALS, buf.toString());
	}

	public Principal getPrincipal() {
		return principal;
	}

}
