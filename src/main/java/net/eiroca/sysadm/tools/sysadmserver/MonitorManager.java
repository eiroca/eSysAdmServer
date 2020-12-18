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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import net.eiroca.library.core.Helper;
import net.eiroca.library.core.LibTimeUnit;
import net.eiroca.library.diagnostics.IServerMonitor;
import net.eiroca.library.diagnostics.ServerMonitors;
import net.eiroca.library.scheduler.FixedFrequencyPolicy;
import net.eiroca.library.scheduler.Task;
import net.eiroca.library.sysadm.monitoring.api.IMeasureConsumer;
import net.eiroca.library.sysadm.monitoring.sdk.MeasureProducer;
import net.eiroca.library.sysadm.monitoring.sdk.ServerContext;
import net.eiroca.sysadm.tools.sysadmserver.util.Configuration;

public class MonitorManager {

  private static final String MONITOR_BASENAME = "Monitor.";
  private static final String MONITOR_FILEEXT = ".monitor";

  static Configuration configuration;
  static List<MeasureProducer> monitors = new ArrayList<>();

  public static void start() throws IOException {
    MonitorManager.configuration = new Configuration("config.", SystemContext.properties);
    final Stream<Path> monitorConfigs = Files.find(SystemContext.config.monitors_path, 1, (filePath, fileAttr) -> {
      final boolean ok = fileAttr.isRegularFile() && filePath.toString().endsWith(MonitorManager.MONITOR_FILEEXT);
      return ok;
    });
    monitorConfigs.forEach(path -> MonitorManager.createMonitor(SystemContext.consumer_metrics, path));
    monitorConfigs.close();
  }

  private static void createMonitor(final IMeasureConsumer consumer, final Path confPath) {
    try {
      final Properties monitorConfig = Helper.loadProperties(confPath.toString(), false);
      final String configNode = monitorConfig.getProperty("config");
      MonitorManager.configuration.update(configNode, monitorConfig);
      final List<String> hosts = MonitorManager.resolveHosts(monitorConfig);
      if (hosts.size() == 0) {
        SystemContext.logger.error("No hosts declared for the monitor: " + confPath);
        return;
      }
      final String type = monitorConfig.getProperty("monitor-type");
      final IServerMonitor checker = ServerMonitors.build(type);
      final String monitorType = checker.getClass().getSimpleName();
      final long freq = LibTimeUnit.getFrequency(monitorConfig.getProperty("monitor-freq"), 60, TimeUnit.SECONDS, 10, 1 * 24 * 60 * 60);
      final ServerContext context = new ServerContext(MonitorManager.MONITOR_BASENAME + monitorType, monitorConfig);
      context.setCredentialProvider(SystemContext.keyStore);
      final String name = monitorConfig.getProperty("name");
      final MeasureProducer monitor = new MeasureProducer(name, checker, hosts, SystemContext.hostGroups, consumer);
      final Task t = SystemContext.addTask(monitor, new FixedFrequencyPolicy(freq, TimeUnit.SECONDS));
      t.setName(name);
      monitor.setId(t.getId());
      monitor.setup(context);
      MonitorManager.monitors.add(monitor);
      SystemContext.logger.info(t.getName() + " ID:" + t.getId() + " config:" + confPath);
      SystemContext.logger.debug("monitor=" + monitor.toString());
    }
    catch (final Exception e) {
      SystemContext.logger.error(confPath + " Error: ", e);
    }
  }

  private static List<String> resolveHosts(final Properties monitorConfig) {
    final List<String> hosts = new ArrayList<>();
    String hostName;
    String aHost = monitorConfig.getProperty("host", null);
    if (aHost != null) {
      hostName = aHost.trim();
      if (SystemContext.hostGroups.hasTag(hostName)) {
        for (final String hostInGroup : SystemContext.hostGroups.getHostByTag(hostName)) {
          hosts.add(hostInGroup);
        }
      }
      else {
        hosts.add(hostName);
      }
    }
    aHost = monitorConfig.getProperty("hosts", null);
    if (aHost != null) {
      for (final String s : aHost.split(" ")) {
        hostName = s.trim();
        if (SystemContext.hostGroups.hasTag(hostName)) {
          for (final String hostInGroup : SystemContext.hostGroups.getHostByTag(hostName)) {
            hosts.add(hostInGroup);
          }
        }
        else {
          hosts.add(hostName);
        }
      }
    }
    return hosts;
  }

  public static void stop() {
    SystemContext.logger.info("Stopping monitoring");
    for (final MeasureProducer m : MonitorManager.monitors) {
      try {
        m.teardown();
      }
      catch (final Exception e) {
        SystemContext.logger.error("SystemError: ", e);
      }
    }
  }

}
