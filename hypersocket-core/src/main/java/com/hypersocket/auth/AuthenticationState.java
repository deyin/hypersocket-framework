/*******************************************************************************
 * Copyright (c) 2013 Hypersocket Limited.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package com.hypersocket.auth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.hypersocket.realm.Principal;
import com.hypersocket.realm.PrincipalType;
import com.hypersocket.realm.Realm;
import com.hypersocket.session.Session;

public class AuthenticationState {

	AuthenticationScheme scheme;
	String remoteAddress;
	Integer currentIndex = new Integer(0);
	List<AuthenticationModule> modules;
	List<PostAuthenticationStep> postAuthenticationSteps = new ArrayList<PostAuthenticationStep>();
	String lastErrorMsg;
	boolean lastErrorIsResourceKey;
	String lastPrincipalName;
	String lastRealmName;
	Realm realm;
	Principal principal;
	Session session;
	int attempts = 0;
	Locale locale;
	String homePage = "";
	
	Map<String, String> parameters = new HashMap<String, String>();
	Map<String, Object> environment = new HashMap<String, Object>();
	AuthenticationState(String remoteAddress, Locale locale, Map<String,Object> environment) {
		this.remoteAddress = remoteAddress;
		this.environment = environment;
		this.locale = locale;
	}

	public AuthenticationModule getCurrentModule() {
		if (currentIndex >= modules.size())
			throw new IllegalStateException(
					"Current index is greater than the number of modules");
		return modules.get(currentIndex);
	}

	public void addParameter(String name, String value) {
		parameters.put(name, value);
	}

	public boolean isNew() {
		return attempts <= 1;
	}

	public Integer getCurrentIndex() {
		return currentIndex;
	}

	public boolean isAuthenticationComplete() {
		return currentIndex >= modules.size();
	}

	void nextModule() {
		this.currentIndex++;
	}

	public AuthenticationScheme getScheme() {
		return scheme;
	}

	public String getRemoteAddress() {
		return remoteAddress;
	}

	public List<AuthenticationModule> getModules() {
		return modules;
	}

	public String getLastErrorMsg() {
		return lastErrorMsg;
	}

	public void setLastErrorMsg(String lastErrorMsg) {
		this.lastErrorMsg = lastErrorMsg;
	}

	public Principal getPrincipal() {
		return principal;
	}

	public void setPrincipal(Principal principal) {
		this.principal = principal;
	}

	public Realm getRealm() {
		return realm;
	}

	public void setRealm(Realm realm) {
		this.realm = realm;
	}

	public Session getSession() {
		return session;
	}

	public void setSession(Session session) {
		this.session = session;
	}

	public void addPostAuthenticationStep(PostAuthenticationStep proc) {
		postAuthenticationSteps.add(proc);
	}

	public boolean hasPostAuthenticationStep() {
		return postAuthenticationSteps.size() > 0;
	}

	public PostAuthenticationStep getCurrentPostAuthenticationStep() {
		if (!hasPostAuthenticationStep()) {
			return null;
		}
		return postAuthenticationSteps.get(0);
	}

	public void nextPostAuthenticationStep() {
		postAuthenticationSteps.remove(0);
	}

	public Locale getLocale() {
		return locale;
	}

	public boolean hasParameter(String name) {
		return parameters.containsKey(name);
	}
	
	public String getParameter(String name) {
		return parameters.get(name);
	}
	
	public void setScheme(AuthenticationScheme scheme) {
		this.scheme = scheme;
	}

	public void setModules(List<AuthenticationModule> modules) {
		this.modules = modules;
	}
	
	public void setLastErrorIsResourceKey(boolean lastErrorIsResourceKey) {
		this.lastErrorIsResourceKey = lastErrorIsResourceKey;
	}
	
	public boolean getLastErrorIsResourceKey() {
		return lastErrorIsResourceKey;
	}

	public void setLastPrincipalName(String username) {
		this.lastPrincipalName = username;
	}

	public void setLastRealmName(String realmName) {
		this.lastRealmName = realmName;
	}
	
	public String getLastPrincipalName() {
		if(principal==null) {
			if(lastPrincipalName==null) {
				throw new IllegalStateException("Last principal name is not available. Did you forget to set it in your Authenticator?");
			}
			return lastPrincipalName;
		} else {
			return principal.getPrincipalName();
		}
	}
	
	public String getLastRealmName() {
		if(realm==null) {
			if(lastRealmName==null) {
				return "";
			} else {
				return lastRealmName;
			}
		} else {
			return realm.getName();
		}
	}

	public String getUserAgent() {
		return environment.get(BrowserEnvironment.USER_AGENT.toString()).toString();
	}
	
	public void setEnvironmentVariable(String key, Object value) {
		environment.put(key, value);
	}
	
	public boolean hasEnvironmentVariable(String key) {
		return environment.containsKey(key);
	}
	
	public Object getEnvironmentVariable(String key){ 
		return environment.get(key);
	}

	public void fakeCredentials() {
		setPrincipal(new FakePrincipal(lastPrincipalName));
		Realm realm = new Realm();
		realm.setId(-1L);
		realm.setName("Fake");
	}
	
	public boolean hasNextStep() {
		return currentIndex < modules.size() - 1;
	}
	
	class FakePrincipal extends Principal {

		FakePrincipal(String username) {
			this.setId(-1L);
			this.setName(username);
		}
		@Override
		public PrincipalType getType() {
			return PrincipalType.USER;
		}
	}

	public void authAttempted() {
		attempts++;
	}

	public int getAttempts() {
		return attempts;
	}
	
	public void setHomePage(String homePage) {
		this.homePage = homePage;
	}
	
	public String getHomePage() {
		return homePage;
	}

	public Map<String,String> getParameters() {
		return parameters;
	}
}
