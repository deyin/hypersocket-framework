package com.hypersocket.realm.events;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;

import com.hypersocket.realm.Principal;
import com.hypersocket.realm.Realm;
import com.hypersocket.realm.RealmProvider;
import com.hypersocket.session.Session;

public class UserCreatedEvent extends UserEvent {

	private static final long serialVersionUID = 128120714278922129L;

	public static final String EVENT_RESOURCE_KEY = "event.userCreated";
	
	public UserCreatedEvent(Object source, Session session, Realm realm,
			RealmProvider provider, Principal principal,
			List<Principal> associatedPrincipals, Map<String,String> properties) {
		super(source, "event.userCreated", session, realm, provider, principal,
				associatedPrincipals, properties);
	}

	public UserCreatedEvent(Object source, Throwable e, Session session,
			Realm realm, RealmProvider provider, String principalName,
			Map<String, String> properties, List<Principal> associatedPrincipals) {
		super(source, "event.userCreated", e, session, realm, provider,
				principalName, properties, associatedPrincipals);
	}

	public String[] getResourceKeys() {
		return ArrayUtils.add(super.getResourceKeys(), EVENT_RESOURCE_KEY);
	}
}
