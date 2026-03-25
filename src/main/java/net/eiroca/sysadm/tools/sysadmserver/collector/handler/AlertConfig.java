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
package net.eiroca.sysadm.tools.sysadmserver.collector.handler;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import net.eiroca.ext.library.gson.GsonUtil;
import net.eiroca.library.config.parameter.BooleanParameter;
import net.eiroca.library.config.parameter.IntegerParameter;
import net.eiroca.library.config.parameter.ListParameter;
import net.eiroca.library.config.parameter.StringParameter;
import net.eiroca.library.csv.CSVData;
import net.eiroca.library.db.DBConfig;
import net.eiroca.library.sysadm.monitoring.sdk.ServerContext;
import net.eiroca.library.system.ContextParameters;
import net.eiroca.sysadm.tools.sysadmserver.SystemContext;
import net.eiroca.sysadm.tools.sysadmserver.util.params.LocalPathParameter;

public class AlertConfig {

  private static final String PREFIX_ALERT = "alert.";
  private static final String PREFIX_ALERT_EXPORT = "alert.export.";

  private static final String PREFIX_ALERT_DB = "DB.";
  private static final String PREFIX_ALERT_LOG = "LOG.";

  private static final String VAR_DB_PREFIX = "db_";
  private static final String VAR_LOG_PREFIX = "log_";

  protected static transient ContextParameters config = new ContextParameters();
  protected static transient ContextParameters configDB = new ContextParameters();
  protected static transient ContextParameters configLog = new ContextParameters();
  //
  protected static transient IntegerParameter _validationLevel = new IntegerParameter(AlertConfig.config, "validationLevel", -1);
  protected static transient LocalPathParameter _defaultTagPath = new LocalPathParameter(AlertConfig.config, "defaultTagPath", null);
  protected static transient LocalPathParameter _dynatraceTagMappingPath = new LocalPathParameter(AlertConfig.config, "dynatraceTagMappingPath", null);
  //
  protected static transient BooleanParameter _dbEnabled = new BooleanParameter(AlertConfig.configDB, "enabled", false);
  protected static transient IntegerParameter _maxSize = new IntegerParameter(AlertConfig.configDB, "maxSize", 250);
  protected static transient StringParameter _tableName = new StringParameter(AlertConfig.configDB, "tableName", null);
  protected static transient ListParameter _tableFields = new ListParameter(AlertConfig.configDB, "tableFields", null);
  //
  protected static transient BooleanParameter _logEnabled = new BooleanParameter(AlertConfig.configLog, "enabled", true);
  protected static transient StringParameter _newFormat = new StringParameter(AlertConfig.configLog, "newFormat", "OPEN");
  protected static transient StringParameter _inprogressFormat = new StringParameter(AlertConfig.configLog, "inprogressFormat", "UPDATE");
  protected static transient StringParameter _closedFormat = new StringParameter(AlertConfig.configLog, "closedFormat", "CLOSE");

  public static final char KEY_SEP = '#';

  //
  public Integer validationLevel;
  public transient Path defaultTagPath;
  public transient Path dynatraceTagMappingPath;
  // DB Export
  public Boolean db_enabled;
  public int db_maxSize;
  public String db_tableName;
  public String[] db_tableFields;
  public final transient DBConfig dbConfig = new DBConfig(null);

  // Log Export
  public Boolean log_enabled;
  public String log_newFormat;
  public String log_inprogressFormat;
  public String log_closedFormat;

  public Map<String, String> def = new HashMap<>();
  public Map<String, String> mapping = new HashMap<>();

  public AlertConfig() {
  }

  public void setup(final Properties params) throws Exception {
    SystemContext.logger.debug("Context: " + params);
    //
    AlertConfig.config.loadConfig(params, AlertConfig.PREFIX_ALERT);
    AlertConfig.config.saveConfig(this, null, true, true);
    //
    AlertConfig.configDB.loadConfig(params, AlertConfig.PREFIX_ALERT_EXPORT + AlertConfig.PREFIX_ALERT_DB);
    AlertConfig.configDB.saveConfig(this, AlertConfig.VAR_DB_PREFIX, true, true);
    final ServerContext context = new ServerContext(AlertConfig.PREFIX_ALERT_EXPORT + AlertConfig.PREFIX_ALERT_DB, SystemContext.getSubConfig(params, AlertConfig.PREFIX_ALERT_EXPORT + AlertConfig.PREFIX_ALERT_DB));
    context.setCredentialProvider(SystemContext.keyStore);
    dbConfig.setup(context);
    //
    AlertConfig.configLog.loadConfig(params, AlertConfig.PREFIX_ALERT_EXPORT + AlertConfig.PREFIX_ALERT_LOG);
    AlertConfig.configLog.saveConfig(this, AlertConfig.VAR_LOG_PREFIX, true, true);
    //
    def.clear();
    if ((defaultTagPath != null) && (Files.exists(defaultTagPath))) {
      loadDefault(defaultTagPath.toString());
    }
    mapping.clear();
    if ((dynatraceTagMappingPath != null) && (Files.exists(dynatraceTagMappingPath))) {
      loadMapping(dynatraceTagMappingPath.toString());
    }
    SystemContext.logger.info("AlertCollector.config: " + this);
  }

  private void loadDefault(final String path) {
    final CSVData csv = new CSVData(path);
    if (csv.size() > 0) {
      for (int i = 0; i < csv.size(); i++) {
        final String[] data = csv.getData(i);
        if ((data == null) || (data.length != 2)) {
          continue;
        }
        final String key = data[0];
        final String val = data[1];
        def.put(key, val);
      }
    }
  }

  private void loadMapping(final String path) {
    final CSVData csv = new CSVData(path);
    if (csv.size() > 0) {
      for (int i = 0; i < csv.size(); i++) {
        final String[] data = csv.getData(i);
        if ((data == null) || (data.length != 4)) {
          continue;
        }
        final String key1 = data[0];
        final String val1 = data[1];
        final String key2 = data[2];
        final String val2 = data[3];
        mapping.put(key1 + AlertConfig.KEY_SEP + val1, key2 + AlertConfig.KEY_SEP + val2);
      }
    }
  }

  @Override
  public String toString() {
    return GsonUtil.toJSON(this);
  }

}
