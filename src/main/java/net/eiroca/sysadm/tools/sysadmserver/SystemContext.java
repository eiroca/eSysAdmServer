/**
 *
 * Copyright (C) 2001-2019 eIrOcA (eNrIcO Croce & sImOnA Burzio) - AGPL >= 3.0
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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import net.eiroca.library.core.Helper;
import net.eiroca.library.license.api.License;
import net.eiroca.library.license.api.LicenseManager;
import net.eiroca.library.scheduler.DelayPolicy;
import net.eiroca.library.scheduler.Scheduler;
import net.eiroca.library.scheduler.SchedulerPolicy;
import net.eiroca.library.server.ServerResponse;
import net.eiroca.library.sysadm.monitoring.sdk.GenericConsumer;
import net.eiroca.library.sysadm.monitoring.sdk.GenericContext;
import net.eiroca.library.system.IContext;
import net.eiroca.library.system.LibFile;
import net.eiroca.library.system.Logs;
import net.eiroca.sysadm.tools.sysadmserver.scheduler.MyScheduler;

public final class SystemContext {

  private static final String EXPORTER_PREFIX = "exporter.";
  private static final String CFG_MONITORS_PATH = "monitors.path";

  public static final String ME = "eSysAdmServer";

  public static final Logger logger = Logs.getLogger(SystemContext.ME);

  public static final String CFG_LOCKFILE = "lockFile";

  public static License license;
  public static ServerResponse LICENCE_ERROR = new ServerResponse(-9999, "License is expired, no action is taken");

  public static Properties config;

  private final static Scheduler scheduler = new MyScheduler();
  public static GenericConsumer consumer = null;

  public static Path lockFile;
  public static Path monitorDefinitionPath;

  public static void init(final String path) throws Exception {
    SystemContext.initLicense();
    // Configuration
    SystemContext.config = new Properties();
    SystemContext.config.putAll(System.getProperties());
    final InputStream prop = LibFile.findResource(path + SystemContext.ME + ".config", path + SystemContext.ME + ".properties");
    if (prop != null) {
      try {
        final Properties localConf = Helper.loadProperties(prop, false);
        SystemContext.config.putAll(localConf);
      }
      catch (final IOException e) {
      }
    }
    SystemContext.logger.debug("config:" + SystemContext.config);
    // Lock file
    String lockPath = path + SystemContext.ME + ".lock";
    lockPath = SystemContext.config.getProperty(SystemContext.CFG_LOCKFILE, lockPath);
    SystemContext.lockFile = Paths.get(lockPath);
    LibFile.writeString(SystemContext.lockFile.toString(), "delete me to stop the process");
    SystemContext.logger.info("lockFile:" + SystemContext.lockFile.toString());
    // Scheduler
    SystemContext.logger.info("Starting scheduler");
    SystemContext.scheduler.start();
    // Measure consumer
    final IContext context = new GenericContext("Exporter", SystemContext.getExporterConfig(SystemContext.config));
    SystemContext.consumer = new GenericConsumer();
    SystemContext.consumer.setup(context);
    final String id = SystemContext.addTask(SystemContext.consumer, new DelayPolicy(5, TimeUnit.SECONDS));
    SystemContext.logger.info("Consumer ID:" + id);
    //
    final String monitorsPath = SystemContext.config.getProperty(SystemContext.CFG_MONITORS_PATH);
    if (monitorsPath == null) {
      SystemContext.monitorDefinitionPath = Paths.get(path, "monitors");
    }
    else {
      SystemContext.monitorDefinitionPath = Paths.get(monitorsPath);
    }
  }

  private static Properties getExporterConfig(final Properties config) {
    final Properties exporterConfig = new Properties();
    for (final String propName : config.stringPropertyNames()) {
      if (propName.startsWith(SystemContext.EXPORTER_PREFIX)) {
        final String val = config.getProperty(propName);
        exporterConfig.setProperty(propName.substring(SystemContext.EXPORTER_PREFIX.length()), val);
      }
    }
    return exporterConfig;
  }

  public static void initLicense() {
    SystemContext.license = LicenseManager.getInstance().getLicense(SystemContext.ME, true);
    if (!SystemContext.isLicenseValid()) {
      SystemContext.logger.error("Invalid license");
      System.exit(1);
    }
    SystemContext.logger.info(SystemContext.ME + " licensed to " + SystemContext.license.getHolder());
  }

  public static boolean isLicenseValid() {
    return LicenseManager.isValidLicense(SystemContext.license);
  }

  public static int getProperty(final String propName, final int defValue) {
    return Helper.getInt(SystemContext.config.getProperty(propName), defValue);
  }

  public static boolean getProperty(final String propName, final boolean defValue) {
    return Helper.getBoolean(SystemContext.config.getProperty(propName), defValue);
  }

  public static String addTask(final Runnable task, final SchedulerPolicy policy) {
    return SystemContext.scheduler.addTask(task, policy);
  }

  public static void done() {
    try {
      SystemContext.consumer.teardown();
    }
    catch (final Exception e) {
      SystemContext.logger.error("SystemError: ", e);
    }
    SystemContext.logger.info("Stopping scheduler");
    SystemContext.scheduler.stop();
  }
}
