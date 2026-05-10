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
import org.apache.http.HttpHost;
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
  private static final String PREFIX_ALERT_HOOK = "HOOK.";

  private static final String VAR_DB_PREFIX = "db_";
  private static final String VAR_LOG_PREFIX = "log_";
  private static final String VAR_HOOK_PREFIX = "hook_";

  protected static transient ContextParameters config = new ContextParameters();
  protected static transient ContextParameters configDB = new ContextParameters();
  protected static transient ContextParameters configLog = new ContextParameters();
  protected static transient ContextParameters configHook = new ContextParameters();
  //
  protected static transient IntegerParameter _validationLevel = new IntegerParameter(AlertConfig.config, "validationLevel", -1);
  protected static transient LocalPathParameter _defaultTagPath = new LocalPathParameter(AlertConfig.config, "defaultTagPath", null);
  protected static transient LocalPathParameter _templatesPath = new LocalPathParameter(AlertConfig.config, "templatesPath", "&templates");
  //
  protected static transient BooleanParameter _dbEnabled = new BooleanParameter(AlertConfig.configDB, "enabled", false);
  protected static transient IntegerParameter _maxSize = new IntegerParameter(AlertConfig.configDB, "maxSize", 250);
  protected static transient StringParameter _tableName = new StringParameter(AlertConfig.configDB, "tableName", null);
  protected static transient ListParameter _tableFields = new ListParameter(AlertConfig.configDB, "tableFields", null);
  //
  protected static transient BooleanParameter _logEnabled = new BooleanParameter(AlertConfig.configLog, "enabled", true);
  protected static transient StringParameter _outputTemplate = new StringParameter(AlertConfig.configLog, "template", null);
  protected static transient BooleanParameter _prettyJson = new BooleanParameter(AlertConfig.configLog, "prettyJson", false);
  //
  protected static transient BooleanParameter _hookEnabled = new BooleanParameter(AlertConfig.configHook, "enabled", true);
  protected static transient StringParameter _hookTemplate = new StringParameter(AlertConfig.configHook, "template", null);
  protected static transient StringParameter _hookUrl = new StringParameter(AlertConfig.configHook, "url", null);
  protected static transient StringParameter _hookToken = new StringParameter(AlertConfig.configHook, "token", null);
  protected static transient StringParameter _hookHeader = new StringParameter(AlertConfig.configHook, "header", "Authorization");
  protected static transient StringParameter _hookProxyHost = new StringParameter(AlertConfig.configHook, "proxyhost", null);
  protected static transient IntegerParameter _hookProxyPort = new IntegerParameter(AlertConfig.configHook, "proxypost", 8080);

  public static final char KEY_SEP = '#';

  //
  public Integer validationLevel;
  public transient Path defaultTagPath;
  public transient Path templatesPath;

  // DB Export
  public Boolean db_enabled;
  public int db_maxSize;
  public String db_tableName;
  public String[] db_tableFields;
  public final transient DBConfig dbConfig = new DBConfig(null);

  // Log Export
  public Boolean log_enabled;
  public transient String log_template;
  public Boolean log_prettyJson;

  // WebHook Export
  public Boolean hook_enabled;
  public String hook_template;
  public String hook_url;
  public String hook_token;
  public String hook_header;
  public transient String hook_proxyhost;
  public transient int hook_proxyport;
  public HttpHost hookProxy = null;

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
    // WebHook
    AlertConfig.configLog.loadConfig(params, AlertConfig.PREFIX_ALERT_EXPORT + AlertConfig.PREFIX_ALERT_HOOK);
    AlertConfig.configLog.saveConfig(this, AlertConfig.VAR_HOOK_PREFIX, true, true);
    if (hook_proxyhost != null) {
      hookProxy = new HttpHost(hook_proxyhost, hook_proxyport);
    }
    else {
      hookProxy = null;
    }
    //
    def.clear();
    if ((defaultTagPath != null) && (Files.exists(defaultTagPath))) {
      loadDefault(defaultTagPath.toString());
    }
    mapping.clear();
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

  @Override
  public String toString() {
    return GsonUtil.toJSON(this);
  }

}
