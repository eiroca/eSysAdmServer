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

import java.nio.file.Path;
import java.util.Properties;
import net.eiroca.library.config.parameter.BooleanParameter;
import net.eiroca.library.config.parameter.IntegerParameter;
import net.eiroca.library.config.parameter.PathParameter;
import net.eiroca.library.config.parameter.StringParameter;
import net.eiroca.library.system.ContextParameters;
import net.eiroca.sysadm.tools.sysadmserver.util.params.HostnameParameter;
import net.eiroca.sysadm.tools.sysadmserver.util.params.LocalPathParameter;

public final class SystemConfig {

  public static final String ME = "eSysAdmServer";

  public static final String EXPORTER_PREFIX = "exporter.";

  protected static final String VAR_PREFIX = null;
  protected static final ContextParameters config = new ContextParameters();
  //
  protected static StringParameter _hostname = new HostnameParameter(SystemConfig.config, "hostname", "locahost");
  protected static PathParameter _lockfile = new LocalPathParameter(SystemConfig.config, "lockfile", SystemConfig.ME + ".lock");
  protected static IntegerParameter _schedulerWorkers = new IntegerParameter(SystemConfig.config, "scheduler.workers", 4);
  protected static PathParameter _rule_engine_path = new LocalPathParameter(SystemConfig.config, "rule-engine.path", "rule-engine.config");
  protected static PathParameter _alias_path = new LocalPathParameter(SystemConfig.config, "alias.path", "alias.config");
  protected static PathParameter _hostgroups_path = new LocalPathParameter(SystemConfig.config, "hostgroups.path", "hostgroups.config");
  protected static StringParameter _hostgroup_tag_prefix = new StringParameter(SystemConfig.config, "hostgroups.tag-prefix", "#");
  protected static PathParameter _keystore_path = new LocalPathParameter(SystemConfig.config, "keystore.path", "keystore.config");
  protected static PathParameter _monitors_path = new LocalPathParameter(SystemConfig.config, "monitors.path", "monitors");
  // Consumers
  protected static IntegerParameter _consumers_sleeptime = new IntegerParameter(SystemConfig.config, "consumers.sleeptime", 10);
  // Collector
  protected static IntegerParameter _collector_port = new IntegerParameter(SystemConfig.config, "collector.port", 1972);
  protected static BooleanParameter _collector_enabled = new BooleanParameter(SystemConfig.config, "collector.enabled", true);
  //
  protected static BooleanParameter _dryrun = new BooleanParameter(SystemConfig.config, "dryrun", false);

  public String basePath = "";
  public String hostname;
  public Path lockfile;
  public int scheduler_workers;
  public Path rule_engine_path;
  public Path alias_path;
  public Path hostgroups_path;
  public String hostgroups_tag_prefix;
  public Path keystore_path;
  public Path monitors_path;
  public int consumers_sleeptime;
  public int collector_port;
  public boolean collector_enabled;
  public boolean dryrun;

  public SystemConfig() {
  }

  public void setup(final Properties params) throws Exception {
    SystemConfig.config.loadConfig(params, null);
    SystemConfig.config.saveConfig(this, SystemConfig.VAR_PREFIX, true, true);
  }

}
