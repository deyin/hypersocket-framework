package com.hypersocket.dashboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hypersocket.auth.AbstractAuthenticatedServiceImpl;
import com.hypersocket.config.ConfigurationPermission;
import com.hypersocket.i18n.I18NService;
import com.hypersocket.permissions.AccessDeniedException;
import com.hypersocket.permissions.PermissionCategory;
import com.hypersocket.permissions.PermissionService;
import com.hypersocket.scheduler.SchedulerService;

@Service
public class OverviewWidgetServiceImpl extends AbstractAuthenticatedServiceImpl
		implements OverviewWidgetService {

	public static final String RESOURCE_BUNDLE = "OverviewWidgetService";
	private List<OverviewWidget> widgetList = new ArrayList<OverviewWidget>();

	@Autowired
	I18NService i18nService;

	@Autowired
	SchedulerService schedulerService;

	@Autowired
	PermissionService permissionService;

	@PostConstruct
	private void postConstruct() {
		i18nService.registerBundle(RESOURCE_BUNDLE);

		PermissionCategory cat = permissionService.registerPermissionCategory(
				RESOURCE_BUNDLE, "category.overview");

		for (OverviewPermission p : OverviewPermission.values()) {
			permissionService.registerPermission(p, cat);
		}

		this.registerWidget(new OverviewWidget(2, "overview.yourSerial.title",
				"yourSerial", false));
		this.registerWidget(new OverviewWidget(4, "overview.usefulLinks.title",
				"usefulLinks", false));
		this.registerWidget(new OverviewWidget(1,
				"overview.systemMessages.title", "systemMessages", true));
		this.registerWidget(new OverviewWidget(2,
				"overview.featureUsage.title", "featureUsage", true));
		this.registerWidget(new OverviewWidget(3, "overview.featureReel.title",
				"featureReel", true));
		this.registerWidget(new OverviewWidget(3, "overview.quickSetup.title",
				"quickSetup", false));
	}

	public void registerWidget(OverviewWidget widget) {
		widgetList.add(widget);
		Collections.sort(widgetList);
	}

	public List<OverviewWidget> getWidgets() throws AccessDeniedException {
		assertPermission(ConfigurationPermission.READ);
		return widgetList;
	}
}
