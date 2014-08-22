package com.hypersocket.tests.menu;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;

import com.hypersocket.auth.AuthenticationPermission;
import com.hypersocket.json.JsonMenu;
import com.hypersocket.json.JsonResourceStatus;
import com.hypersocket.json.JsonRoleResourceStatus;
import com.hypersocket.tests.AbstractServerTest;

public class NoPermissionTest extends AbstractServerTest {

	@BeforeClass
	public static void logOn() throws Exception {
		logon("Default", "admin", "Password123?");
		JsonResourceStatus jsonCreateUser = createUser("Default", "user",
				"user", false);
		changePassword("user", jsonCreateUser);
		Long[] permissions = { getPermissionId(AuthenticationPermission.LOGON
				.getResourceKey())};
		JsonRoleResourceStatus jsonCreateRole = createRole("newrole",
				permissions);
		addUserToRole(jsonCreateRole.getResource(), jsonCreateUser);
		logoff();
		logon("Default", "user", "user");
	}
	
	@Test
	public void testAccessMenuNotAvailable()throws Exception{
		String json=doGet("/hypersocket/api/menus");
		debugJSON(json);
		JsonMenu menus=getMapper().readValue(json,JsonMenu.class);
		assertNotNull(menus);
		assertEquals(0,menus.getMenus().length);
	} 
}
