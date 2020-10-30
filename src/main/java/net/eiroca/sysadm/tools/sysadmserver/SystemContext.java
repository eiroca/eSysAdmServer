/**
 *
 * Copyright (C) 1999-2020 Enrico Croce - AGPL >= 3.0
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
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import net.eiroca.library.core.Helper;
import net.eiroca.library.license.api.License;
import net.eiroca.library.license.api.LicenseManager;
import net.eiroca.library.scheduler.DelayPolicy;
import net.eiroca.library.scheduler.SchedulerPolicy;
import net.eiroca.library.scheduler.Task;
import net.eiroca.library.server.ServerResponse;
import net.eiroca.library.sysadm.monitoring.api.EventRule;
import net.eiroca.library.sysadm.monitoring.sdk.ICredentialProvider;
import net.eiroca.library.sysadm.monitoring.sdk.MeasureConsumer;
import net.eiroca.library.sysadm.monitoring.sdk.RuleEngine;
import net.eiroca.library.system.Context;
import net.eiroca.library.system.IContext;
import net.eiroca.library.system.LibFile;
import net.eiroca.library.system.Logs;
import net.eiroca.sysadm.tools.sysadmserver.collector.AlertCollectorConfig;
import net.eiroca.sysadm.tools.sysadmserver.scheduler.MyScheduler;
import net.eiroca.sysadm.tools.sysadmserver.util.CredentialStore;
import net.eiroca.sysadm.tools.sysadmserver.util.HostGroups;

public final class SystemContext {

  public static final ServerResponse LICENCE_ERROR = new ServerResponse(-9999, "License is expired, no action is taken");

  public static final Logger logger = Logs.getLogger(SystemConfig.ME);

  public static final SystemConfig config = new SystemConfig();
  public static final AlertCollectorConfig alertCollectorConfig = new AlertCollectorConfig();

  public static License license;
  public static Properties properties;
  public static MyScheduler scheduler;
  public static MeasureConsumer consumer_metrics;
  public static HostGroups hostGroups;
  public static ICredentialProvider keyStore;

  public static void init(final String path) throws Exception {
    SystemContext.initLicense();
    // Configuration
    SystemContext.properties = new Properties();
    SystemContext.properties.putAll(System.getProperties());
    final InputStream prop = LibFile.findResource(path + SystemConfig.ME + ".config", path + SystemConfig.ME + ".properties");
    if (prop != null) {
      try {
        final Properties localConf = Helper.loadProperties(prop, false);
        SystemContext.properties.putAll(localConf);
      }
      catch (final IOException e) {
      }
    }
    SystemContext.logger.debug("config:" + SystemContext.properties);
    SystemContext.config.basePath = path;
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
    engine.addRule(new EventRule()); // default rule
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
    SystemContext.alertCollectorConfig.setup(SystemContext.properties);
  }

  public static Properties getSubConfig(final Properties config, String prefix) {
    final Properties exporterConfig = new Properties();
    int len = prefix.length();
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
