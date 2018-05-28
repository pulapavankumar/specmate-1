	package com.specmate.connectors.internal;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

import com.specmate.common.OSGiUtil;
import com.specmate.config.api.IConfigService;
import com.specmate.connectors.api.ConfigurableBase;
import com.specmate.connectors.api.Connector;
import com.specmate.connectors.api.Exporter;
import com.specmate.connectors.api.IExportService;
import com.specmate.connectors.api.IProjectService;
import com.specmate.connectors.api.IRequirementsSource;
import com.specmate.connectors.api.Project;
import com.specmate.connectors.config.ProjectConfig;

/**
 * Service that configures connectors and exporters based on configured projects
 */
@Component(immediate = true)
public class ProjectService implements IProjectService {

	/** Time to wait for the configured services to appear. */
	private static final int SERVICE_WAIT_TIME = 5000;

	/** The config service */
	private IConfigService configService;

	/** Mapping from project names to configured project. */
	private Map<String, Project> projects = new HashMap<>();

	/** The config admin service. */
	private ConfigurationAdmin configAdmin;

	/** The log service. */
	private LogService logService;

	/** Context of this bundle. */
	private BundleContext context;

	@Activate
	public void activate(BundleContext context) {
		this.context = context;
		String[] projectsNames = configService.getConfigurationPropertyArray("project.projects");
		if (projectsNames == null) {
			return;
		}
		
		for (int i = 0; i < projectsNames.length; i++) {
			Project project = new Project();
			project.setName(projectsNames[i]);
			String projectPrefix = "project." + projectsNames[i];

			Connector connector = createConnector(projectPrefix);
			if (connector == null) {
				continue;
			}
			project.setConnector(connector);

			Exporter exporter = createExporter(projectPrefix);
			if (exporter == null) {
				continue;
			}
			project.setExporter(exporter);

			projects.put(project.getName(), project);
		}

		for (Project project : projects.values()) {
			configureConnector(project.getConnector());
			configureExporter(project.getExporter());
		}
	}

	/**
	 * Creates an exporter from the config for the project given by the config
	 * prefix.
	 */
	private Exporter createExporter(String projectPrefix) {
		String exporterPrefix = projectPrefix + "." + "exporter";
		Set<Entry<Object, Object>> exporterConfig = configService.getConfigurationProperties(exporterPrefix);
		if (exporterConfig == null || exporterConfig.isEmpty()) {
			return null;
		}
		Exporter exporter = new Exporter();
		fillConfigurable(exporter, exporterConfig, exporterPrefix);
		return exporter;
	}

	/**
	 * Creates an connector from the config for the project given by the config
	 * prefix.
	 */
	private Connector createConnector(String projectPrefix) {
		String connectorPrefix = projectPrefix + "." + "connector";
		Set<Entry<Object, Object>> connectorConfig = configService.getConfigurationProperties(connectorPrefix);
		if (connectorConfig == null || connectorConfig.isEmpty()) {
			return null;
		}
		Connector connector = new Connector();
		fillConfigurable(connector, connectorConfig, connectorPrefix);
		return connector;
	}

	/** Starts a connector service. */
	private void configureConnector(Connector connector) {
		try {
			OSGiUtil.configureFactory(configAdmin, connector.getPid(), connector.getConfig());
			Filter filter = context.createFilter("(&(" + Constants.OBJECTCLASS + "="
					+ IRequirementsSource.class.getName() + ")" + "(" + ProjectConfig.KEY_CONNECTOR_ID + "="
					+ connector.getConfig().get(ProjectConfig.KEY_CONNECTOR_ID) + "))");
			ServiceTracker<IRequirementsSource, IRequirementsSource> tracker = new ServiceTracker<IRequirementsSource, IRequirementsSource>(
					context, filter, null);
			tracker.open();
			IRequirementsSource requirementsSourceService = tracker.waitForService(SERVICE_WAIT_TIME);
			connector.setRequirementsSourceService(requirementsSourceService);
		} catch (Exception e) {
			this.logService.log(LogService.LOG_ERROR, "Failed attempt to configure connector of type "
					+ connector.getPid() + " with config " + OSGiUtil.configDictionaryToString(connector.getConfig()));
		}
	}

	/** Starts an exporter service. */
	private void configureExporter(Exporter exporter) {
		try {
			OSGiUtil.configureFactory(configAdmin, exporter.getPid(), exporter.getConfig());
			Filter filter = context.createFilter("(&(" + Constants.OBJECTCLASS + "=" + IExportService.class.getName()
					+ ")" + "(" + ProjectConfig.KEY_EXPORTER_ID + "="
					+ exporter.getConfig().get(ProjectConfig.KEY_EXPORTER_ID) + "))");
			ServiceTracker<IExportService, IExportService> tracker = new ServiceTracker<IExportService, IExportService>(
					context, filter, null);
			tracker.open();
			IExportService exporterService = tracker.waitForService(SERVICE_WAIT_TIME);
			exporter.setExporterService(exporterService);
		} catch (Exception e) {
			this.logService.log(LogService.LOG_ERROR, "Failed attempt to configure exporter of type "
					+ exporter.getPid() + " with config " + OSGiUtil.configDictionaryToString(exporter.getConfig()));
		}
	}

	/** Fills the config entries into the configurable object. */
	private void fillConfigurable(ConfigurableBase configurable, Set<Entry<Object, Object>> config, String prefix) {
		Hashtable<String, Object> configTable = new Hashtable<>();
		for (Entry<Object, Object> configEntry : config) {
			String key = (String) configEntry.getKey();
			String connectorConfigKey = key.substring(prefix.length() + 1);
			String pidKey = "pid";
			if (connectorConfigKey.equals(pidKey)) {
				configurable.setPid((String) configEntry.getValue());
			} else {
				configTable.put(connectorConfigKey, configEntry.getValue());
			}
		}
		configurable.setConfig(configTable);
	}

	@Override
	public Project getProject(String projectName) {
		return this.projects.get(projectName);
	}

	@Reference
	public void setConfigService(IConfigService configService) {
		this.configService = configService;
	}

	@Reference
	public void setConfigurationAdmin(ConfigurationAdmin configAdmin) {
		this.configAdmin = configAdmin;
	}

	@Reference
	public void setLogService(LogService logService) {
		this.logService = logService;
	}
}