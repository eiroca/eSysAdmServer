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
package net.eiroca.sysadm.tools.sysadmserver.collector;

import java.util.Arrays;
import java.util.Properties;
import net.eiroca.library.config.parameter.IntegerParameter;
import net.eiroca.library.config.parameter.ListParameter;
import net.eiroca.library.config.parameter.StringParameter;
import net.eiroca.library.db.DBConfig;
import net.eiroca.library.sysadm.monitoring.sdk.ServerContext;
import net.eiroca.library.system.ContextParameters;
import net.eiroca.sysadm.tools.sysadmserver.SystemContext;

public class AlertCollectorConfig {

  private static final String VAR_PREFIX = null;

  protected static ContextParameters config = new ContextParameters();
  //
  protected static IntegerParameter _maxSize = new IntegerParameter(AlertCollectorConfig.config, "maxSize", 250);
  protected static StringParameter _tableName = new StringParameter(AlertCollectorConfig.config, "tableName", null);
  protected static ListParameter _tableFields = new ListParameter(AlertCollectorConfig.config, "tableFields", null);
  //
  public int maxSize;
  public String tableName;
  public String[] tableFields;

  public final DBConfig dbConfig = new DBConfig(null);

  public static final String CONFIG_PREFIX = "consumer.config.DB.";

  public AlertCollectorConfig() {
    AlertCollectorConfig.config.saveConfig(this, AlertCollectorConfig.VAR_PREFIX, true, true);
  }

  public void setup(final Properties params) throws Exception {
    AlertCollectorConfig.config.loadConfig(params, null);
    AlertCollectorConfig.config.saveConfig(this, AlertCollectorConfig.VAR_PREFIX, true, true);
    final ServerContext context = new ServerContext(AlertCollectorConfig.CONFIG_PREFIX, params);
    context.setCredentialProvider(SystemContext.keyStore);
    dbConfig.setup(context);
    SystemContext.logger.info("AlertCollector.Cofig: " + this);
    SystemContext.logger.info("AlertCollector.DBCofig: " + dbConfig);
  }

  @Override
  public String toString() {
    return "AlertCollectorConfig [maxSize=" + maxSize + ", tableName=" + tableName + ", tableFields=" + Arrays.toString(tableFields) + "]";
  }

}
