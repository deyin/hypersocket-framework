package com.hypersocket.tests.json.menu;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.hypersocket.auth.AuthenticationPermission;
import com.hypersocket.json.JsonResourceStatus;
import com.hypersocket.json.JsonRoleResourceStatus;
import com.hypersocket.realm.UserPermission;
import com.hypersocket.tests.AbstractServerTest;

public abstract class AbstractMenuTest extends AbstractServerTest {

	static JsonRoleResourceStatus jsonCreateRole;

	@BeforeClass
	public static void init() throws Exception {
		logon("System", "admin", "Password123?");
		JsonResourceStatus jsonCreateUser = createUser("System", "user",
				"user", false);
		changePassword("user", jsonCreateUser);
		Long[] permissions = {
				getPermissionId(AuthenticationPermission.LOGON.getResourceKey()),
				getPermissionId(UserPermission.CREATE.getResourceKey()),
				getPermissionId(UserPermission.READ.getResourceKey()),
				getPermissionId(UserPermission.UPDATE.getResourceKey()),
				getPermissionId(UserPermission.DELETE.getResourceKey()) };
		JsonRoleResourceStatus jsonCreateRole = createRole("newrole",
				permissions);
		addUserToRole(jsonCreateRole.getResource(), jsonCreateUser);
		logoff();
		logon("System", "user", "user");
	}

	@AfterClass
	public static void clean() throws Exception {
		logoff();
	}
}
