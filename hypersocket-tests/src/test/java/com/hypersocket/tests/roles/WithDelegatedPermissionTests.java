package com.hypersocket.tests.roles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.util.Assert;

import com.hypersocket.auth.AuthenticationPermission;
import com.hypersocket.json.JsonResourceStatus;
import com.hypersocket.json.JsonRoleResourceStatus;
import com.hypersocket.permissions.json.RoleUpdate;
import com.hypersocket.realm.RolePermission;
import com.hypersocket.tests.AbstractServerTest;

public class WithDelegatedPermissionTests extends AbstractServerTest {

	@BeforeClass
	public static void logOn() throws Exception {

		logOnNewUser(new String[] {
				AuthenticationPermission.LOGON.getResourceKey(),
				RolePermission.CREATE.getResourceKey(),
				RolePermission.READ.getResourceKey(),
				RolePermission.UPDATE.getResourceKey(),
				RolePermission.DELETE.getResourceKey() });
	}

	@AfterClass
	public static void logOff() throws JsonParseException,
			JsonMappingException, IOException {
		logoff();
	}

	@Test
	public void tryWithDelegatedPermissionRoleId()
			throws ClientProtocolException, IOException {
		doGet("/hypersocket/api/roles/role/" + getSystemAdminRole().getId());
	}

	@Test
	public void tryWithDelegatedPermissionRoleByName()
			throws ClientProtocolException, IOException {

		doGet("/hypersocket/api/roles/byName/System%20Administrator");
	}

	@Test
	public void tryWithDelegatedPermissionRoleTemplate()
			throws ClientProtocolException, IOException {
		doGet("/hypersocket/api/roles/template");
	}

	@Test
	public void tryWithDelegatedPermissionRoleList()
			throws ClientProtocolException, IOException {
		doGet("/hypersocket/api/roles/list");
	}

	@Test
	public void tryWithDelegatedPermissionRoleTable()
			throws ClientProtocolException, IOException {
		doGet("/hypersocket/api/roles/table");
	}

	@Test
	public void tryWithDelegatedPermissionCreateRolePost() throws Exception {
		RoleUpdate role = new RoleUpdate();
		role.setName("newRole");
		role.setPermissions(new Long[0]);
		Long[] permissions = { getPermissionId(AuthenticationPermission.LOGON
				.getResourceKey()) };

		role.setPermissions(permissions);
		role.setUsers(new Long[0]);
		role.setGroups(new Long[0]);

		JsonResourceStatus json = getMapper().readValue(
				doPostJson("/hypersocket/api/roles/role", role),
				JsonResourceStatus.class);
		assertTrue(json.isSuccess());
		Assert.notNull(json.getResource().getId());
		assertEquals("newRole", json.getResource().getName());
	}

	@Test
	public void tryWithDelegatedPermissionDeleteRoleId() throws Exception {
		JsonRoleResourceStatus jsonRole = createRole("roleName",
				new Long[] { getPermissionId(AuthenticationPermission.LOGON
						.getResourceKey()) });
		doDelete("/hypersocket/api/roles/role/"
				+ jsonRole.getResource().getId());
	}
}
