/**
 *
 * Copyright (C) 1999-2021 Enrico Croce - AGPL >= 3.0
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 *
 **/
package net.eiroca.sysadm.tools.sysadmserver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import net.eiroca.library.core.Helper;
import net.eiroca.library.core.LibStr;
import net.eiroca.library.license.api.License;
import net.eiroca.library.license.api.LicenseManager;
import net.eiroca.library.scheduler.DelayPolicy;
import net.eiroca.library.scheduler.SchedulerPolicy;
import net.eiroca.library.scheduler.Task;
import net.eiroca.library.sysadm.monitoring.api.EventRule;
import net.eiroca.library.sysadm.monitoring.sdk.ICredentialProvider;
import net.eiroca.library.sysadm.monitoring.sdk.MeasureConsumer;
import net.eiroca.library.sysadm.monitoring.sdk.RuleEngine;
import net.eiroca.library.system.Context;
import net.eiroca.library.system.IContext;
import net.eiroca.library.system.LibFile;
import net.eiroca.library.system.Logs;
import net.eiroca.sysadm.tools.sysadmserver.collector.GenericHandler;
import net.eiroca.sysadm.tools.sysadmserver.collector.handler.AlertHandler;
import net.eiroca.sysadm.tools.sysadmserver.collector.handler.MeasureHandler;
import net.eiroca.sysadm.tools.sysadmserver.collector.handler.TaskHandler;
import net.eiroca.sysadm.tools.sysadmserver.collector.handler.TraceHandler;
import net.eiroca.sysadm.tools.sysadmserver.collector.handler.UserRoleHandler;
import net.eiroca.sysadm.tools.sysadmserver.manager.CollectorManager;
import net.eiroca.sysadm.tools.sysadmserver.manager.ISysAdmManager;
import net.eiroca.sysadm.tools.sysadmserver.manager.MonitorManager;
import net.eiroca.sysadm.tools.sysadmserver.scheduler.MyScheduler;
import net.eiroca.sysadm.tools.sysadmserver.util.CredentialStore;
import net.eiroca.sysadm.tools.sysadmserver.util.HostGroups;

public final class SystemContext {

  public static final Logger logger = Logs.getLogger(SystemConfig.ME);

  public static final SystemConfig config = new SystemConfig();

  public static License license;
  public static Properties properties;
  public static MyScheduler scheduler;
  public static MeasureConsumer consumer_metrics;
  public static HostGroups hostGroups;
  public static ICredentialProvider keyStore;

  public static final CollectorManager collectorManager = new CollectorManager();
  public static final MonitorManager monitorManager = new MonitorManager();

  public static final AlertHandler alertHandler = new AlertHandler();
  public static final MeasureHandler measureHandler = new MeasureHandler();
  public static final UserRoleHandler userRoleHandler = new UserRoleHandler();
  public static final TraceHandler traceHandler = new TraceHandler();
  public static final TaskHandler taskHandler = new TaskHandler();

  public static final ISysAdmManager managers[] = {
      SystemContext.collectorManager,
      SystemContext.monitorManager
  };

  public static final GenericHandler handlers[] = {
      SystemContext.alertHandler,
      SystemContext.measureHandler,
      SystemContext.userRoleHandler,
      SystemContext.traceHandler,
      SystemContext.taskHandler
  };

  private static void startManagers() throws Exception {
    SystemContext.logger.info("Starting managers");
    for (final ISysAdmManager manager : SystemContext.managers) {
      manager.start();
    }
  }

  private static void stopManagers() throws Exception {
    SystemContext.logger.info("Stopping managers");
    for (final ISysAdmManager manager : SystemContext.managers) {
      if (manager.isStarted()) {
        try {
          manager.stop();
        }
        catch (final Exception e) {
          SystemContext.logger.warn(Helper.getExceptionAsString(e));
        }
      }
    }
  }

  private static void initHandlers(Properties config) throws Exception {
    SystemContext.logger.info("Init handlers");
    for (final GenericHandler handler : SystemContext.handlers) {
      handler.init(config);
    }
  }

  public static void init(final Path configPath) throws Exception {
    SystemContext.initLicense();
    // Configuration
    SystemContext.properties = LibFile.loadConfiguration(configPath.toString());
    SystemContext.config.configPath = configPath.getParent().toString();
    if (LibStr.isEmptyOrNull(SystemContext.config.configPath)) {
      SystemContext.config.configPath = "";
    }
    else {
      if (!SystemContext.config.configPath.endsWith(Helper.FS)) {
        SystemContext.config.configPath += Helper.FS;
      }
    }
    SystemContext.logger.debug("path: " + SystemContext.config.configPath + " config:" + SystemContext.properties);
    SystemContext.config.setup(SystemContext.properties);
    // Lock file
    SystemContext.logger.info("lockFile: " + SystemContext.config.lockfile.toString());
    LibFile.writeString(SystemContext.config.lockfile.toString(), "delete me to stop the process");
    // Scheduler
    SystemContext.logger.info("Starting scheduler");
    SystemContext.scheduler = new MyScheduler(SystemContext.config.scheduler_workers);
    SystemContext.scheduler.start();
    // Event Rule Engine
    final RuleEngine engine = new RuleEngine();
    final Properties ruleConfig = Helper.loadProperties(SystemContext.config.rule_engine_path.toString(), false);
    engine.loadRules(ruleConfig);
    engine.addRule(new EventRule("*")); // default rule
    // Alias
    final Properties aliasProp = Helper.loadProperties(SystemContext.config.alias_path.toString(), false);
    final Map<String, String> alias = new HashMap<>();
    for (final String name : aliasProp.stringPropertyNames()) {
      alias.put(name, aliasProp.getProperty(name));
    }
    // consumers
    // Metrics
    final Properties exporterConfig = SystemContext.getSubConfig(SystemContext.properties, SystemConfig.EXPORTER_PREFIX);
    final IContext context = new Context("Exporter", exporterConfig);
    SystemContext.consumer_metrics = new MeasureConsumer(engine, alias);
    SystemContext.consumer_metrics.setup(context);
    final Task t = SystemContext.addTask(SystemContext.consumer_metrics, new DelayPolicy(SystemContext.config.consumers_sleeptime, TimeUnit.SECONDS));
    t.setName("Metric consumer");
    SystemContext.logger.info(t.getName() + " ID:" + t.getId());
    // Hostgroups
    SystemContext.hostGroups = new HostGroups(SystemContext.config.hostgroups_path, SystemContext.config.hostgroups_tag_prefix);
    // Keystore
    SystemContext.keyStore = new CredentialStore(SystemContext.config.keystore_path);
    // Init Managers & handlers
    SystemContext.startManagers();
    SystemContext.initHandlers(SystemContext.properties);
  }

  public static Properties getSubConfig(final Properties config, final String prefix) {
    final Properties exporterConfig = new Properties();
    final int len = prefix.length();
    for (final String propName : config.stringPropertyNames()) {
      if (propName.startsWith(prefix)) {
        final String val = config.getProperty(propName);
        exporterConfig.setProperty(propName.substring(len), val);
      }
    }
    return exporterConfig;
  }

  public static void initLicense() {
    SystemContext.license = LicenseManager.getInstance().getLicense(SystemConfig.ME, true);
    if (!SystemContext.isLicenseValid()) {
      SystemContext.logger.error("Invalid license");
      SystemContext.logger.debug("License: " + SystemContext.license);
      System.exit(1);
    }
    SystemContext.logger.info(SystemConfig.ME + " licensed to " + SystemContext.license.getHolder());
  }

  public static boolean isLicenseValid() {
    return LicenseManager.isValidLicense(SystemContext.license);
  }

  public static Task addTask(final Runnable task, final SchedulerPolicy policy) {
    return SystemContext.scheduler.addTask(task, policy);
  }

  public static void done() {
    try {
      SystemContext.logger.info("Stopping consumer");
      SystemContext.consumer_metrics.teardown();
      SystemContext.stopManagers();
    }
    catch (final Exception e) {
      SystemContext.logger.error("SystemError: ", e);
    }
    try {
      SystemContext.logger.info("Stopping scheduler");
      SystemContext.scheduler.stop();
    }
    catch (final Exception e) {
      SystemContext.logger.error("SystemError: ", e);
    }
    try {
      Files.delete(SystemContext.config.lockfile);
    }
    catch (final IOException e) {
      Logs.ignore(e);
    }
  }
}
