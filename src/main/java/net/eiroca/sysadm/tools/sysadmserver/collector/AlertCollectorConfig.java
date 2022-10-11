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
package net.eiroca.sysadm.tools.sysadmserver.collector;

import java.util.Arrays;
import java.util.Properties;
import net.eiroca.library.config.parameter.BooleanParameter;
import net.eiroca.library.config.parameter.IntegerParameter;
import net.eiroca.library.config.parameter.ListParameter;
import net.eiroca.library.config.parameter.StringParameter;
import net.eiroca.library.db.DBConfig;
import net.eiroca.library.sysadm.monitoring.sdk.ServerContext;
import net.eiroca.library.system.ContextParameters;
import net.eiroca.sysadm.tools.sysadmserver.SystemContext;

public class AlertCollectorConfig {

  private static final String PREFIX_ALERT = "alert.export.";
  private static final String PREFIX_ALERT_DB = "DB";
  private static final String PREFIX_ALERT_LOG = "LOG";
  private static final String VAR_PREFIX = null;

  protected static ContextParameters config = new ContextParameters();
  //
  protected static IntegerParameter _maxSize = new IntegerParameter(AlertCollectorConfig.config, "maxSize", 250);
  protected static StringParameter _tableName = new StringParameter(AlertCollectorConfig.config, "tableName", null);
  protected static ListParameter _tableFields = new ListParameter(AlertCollectorConfig.config, "tableFields", null);
  //
  protected static BooleanParameter _logEnabled = new BooleanParameter(AlertCollectorConfig.config, "log", true);
  protected static StringParameter _newFormat = new StringParameter(AlertCollectorConfig.config, "newFormat", "OPEN");
  protected static StringParameter _inprogressFormat = new StringParameter(AlertCollectorConfig.config, "inprogressFormat", "OPEN");
  protected static StringParameter _closedFormat = new StringParameter(AlertCollectorConfig.config, "closedFormat", "CLOSE");

  // DB Export
  public int maxSize;
  public String tableName;
  public String[] tableFields;
  public final DBConfig dbConfig = new DBConfig(null);

  // Log Export
  public Boolean log;
  public String newFormat;
  public String inprogressFormat;
  public String closedFormat;

  public AlertCollectorConfig() {
  }

  public void setup(final Properties params) throws Exception {
    SystemContext.logger.debug("Context: " + params);
    //
    AlertCollectorConfig.config.loadConfig(params, AlertCollectorConfig.PREFIX_ALERT + "." + PREFIX_ALERT_DB);
    AlertCollectorConfig.config.saveConfig(this, AlertCollectorConfig.VAR_PREFIX, true, true);
    final ServerContext context = new ServerContext(AlertCollectorConfig.PREFIX_ALERT, SystemContext.getSubConfig(params, AlertCollectorConfig.PREFIX_ALERT));
    context.setCredentialProvider(SystemContext.keyStore);
    dbConfig.setup(context);
    //
    AlertCollectorConfig.config.loadConfig(params, AlertCollectorConfig.PREFIX_ALERT + "." + PREFIX_ALERT_LOG);
    AlertCollectorConfig.config.saveConfig(this, AlertCollectorConfig.VAR_PREFIX, true, true);
    //
    SystemContext.logger.info("AlertCollector.config: " + this);
    SystemContext.logger.info("AlertCollector.DBConfig: " + dbConfig);

  }

  @Override
  public String toString() {
    return "AlertCollectorConfig [maxSize=" + maxSize + ", tableName=" + tableName + ", tableFields=" + Arrays.toString(tableFields) + "]";
  }

}
