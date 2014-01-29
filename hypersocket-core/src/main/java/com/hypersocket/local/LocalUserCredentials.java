/*******************************************************************************
 * Copyright (c) 2013 Hypersocket Limited.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 ******************************************************************************/
package com.hypersocket.local;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import com.hypersocket.auth.PasswordEncryptionType;
import com.hypersocket.repository.AbstractEntity;

@Entity
@Table(name="local_user_credentials")
public class LocalUserCredentials extends AbstractEntity<Long> {

	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column(name="id")
	Long id;
	
	@OneToOne
	LocalUser user;
	
	@Column(name="password", nullable=false)
	byte[] password;
	
	@Column(name="salt", nullable=false)
	byte[] salt;
	
	@Column(name="encryption_type", nullable=false)
	PasswordEncryptionType encryptionType;
	
	@Column(name="password_change_required")
	boolean passwordChangeRequired;
	
	public LocalUser getUser() {
		return user;
	}

	public void setUser(LocalUser user) {
		this.user = user;
	}

	public byte[] getPassword() {
		return password;
	}
	
	public void setPassword(byte[] password) {
		this.password = password;
	}
	
	public byte[] getSalt() {
		return salt;
	}
	
	public void setSalt(byte[] salt) {
		this.salt = salt;
	}
	
	public PasswordEncryptionType getEncryptionType() {
		return encryptionType;
	}
	
	public void setEncryptionType(PasswordEncryptionType encryptionType) {
		this.encryptionType = encryptionType;
	}
	
	public boolean isPasswordChangeRequired() {
		return passwordChangeRequired;
	}

	public void setPasswordChangeRequired(boolean passwordChangeRequired) {
		this.passwordChangeRequired = passwordChangeRequired;
	}

	@Override
	public Long getId() {
		return id;
	}
}
