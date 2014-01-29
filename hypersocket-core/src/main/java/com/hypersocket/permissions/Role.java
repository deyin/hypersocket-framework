/*******************************************************************************
 * Copyright (c) 2013 Hypersocket Limited.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package com.hypersocket.permissions;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonIgnore;

import com.hypersocket.realm.Principal;
import com.hypersocket.realm.Realm;
import com.hypersocket.resource.AssignableResource;
import com.hypersocket.resource.Resource;

@Entity
@Table(name = "roles", uniqueConstraints = {@UniqueConstraint(columnNames={"name", "realm_id"})})
@XmlRootElement(name="role")
public class Role extends Resource {

	@ManyToMany(fetch=FetchType.EAGER)
	@JoinTable(name = "role_permissions", 
		joinColumns = {@JoinColumn(name="role_id")}, 
		inverseJoinColumns = {@JoinColumn(name="permission_id")})
	private Set<Permission> permissions = new HashSet<Permission>();

	@ManyToOne(optional=true)
	@JoinColumn(name="realm_id")
	Realm realm;
	
	@ManyToMany(fetch=FetchType.EAGER)
	@JoinTable(name = "role_principals", joinColumns={@JoinColumn(name="role_id")}, inverseJoinColumns={@JoinColumn(name="principal_id")})
	Set<Principal> principals = new HashSet<Principal>();
	
	@ManyToMany(fetch=FetchType.EAGER)
	@JoinTable(name = "resource_roles", 
		joinColumns = {@JoinColumn(name="role_id")}, 
		inverseJoinColumns = {@JoinColumn(name="resource_id")})
	private Set<AssignableResource> resources = new HashSet<AssignableResource>();
	
	@Column(name="all_users", nullable=false)
	boolean allUsers;
	
	public Set<Permission> getPermissions() {
		return permissions;
	}
	
	public void setPermissions(Set<Permission> permissions) {
		this.permissions = permissions;
	}	
	
	public boolean isSystem() {
		return realm==null;
	}
	
	public Realm getRealm() {
		return realm;
	}
	
	void setRealm(Realm realm) {
		this.realm = realm;
	}

	public Set<Principal> getPrincipals() {
		return principals;
	}

	public void setPrincipals(Set<Principal> principals) {
		this.principals = principals;
	}
	
	@JsonIgnore
	public Set<AssignableResource> getResources() {
		return resources;
	}
	
	public boolean isAllUsers() {
		return allUsers;
	}
	
	public void setAllUsers(boolean allUsers) {
		this.allUsers = allUsers;
	}

	
}
