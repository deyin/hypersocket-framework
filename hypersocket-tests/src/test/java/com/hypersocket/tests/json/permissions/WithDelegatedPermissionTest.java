package com.hypersocket.tests.json.permissions;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.hypersocket.auth.AuthenticationPermission;
import com.hypersocket.json.JsonResourceStatus;
import com.hypersocket.json.JsonRoleResourceStatus;
import com.hypersocket.realm.UserPermission;
import com.hypersocket.tests.AbstractServerTest;

public class WithDelegatedPermissionTest extends AbstractServerTest {

	@BeforeClass
	public static void init() throws Exception {
		logon("System", "admin", "Password123?");
		JsonResourceStatus jsonCreateUser = createUser("System", "user1",
				"password1", false);
		Long[] permissions = {
				getPermissionId(AuthenticationPermission.LOGON.getResourceKey()),
				getPermissionId(UserPermission.CREATE.getResourceKey()) };
		JsonRoleResourceStatus jsonCreateRole = createRole("newrole",
				permissions);
		addUserToRole(jsonCreateRole.getResource(), jsonCreateUser);
		logoff();
		logon("System", "user1", "password1");
	}

	@AfterClass
	public static void clean() throws Exception {
		logoff();
	}

	@Test
	public void testCallPermissionList() throws Exception {
		doGet("/hypersocket/api/permissions/list");
	}

	@Test
	public void testUserCreationPermission() throws Exception {
		doGet("/hypersocket/api/permissions/permission/user.create/");
	}

}
