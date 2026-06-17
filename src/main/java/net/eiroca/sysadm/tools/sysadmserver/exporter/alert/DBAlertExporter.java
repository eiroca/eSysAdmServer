/**
 *
 * Copyright (C) 1999-2026 Enrico Croce - AGPL >= 3.0
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
package net.eiroca.sysadm.tools.sysadmserver.exporter.alert;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import net.eiroca.library.config.parameter.BooleanParameter;
import net.eiroca.library.config.parameter.IntegerParameter;
import net.eiroca.library.config.parameter.ListParameter;
import net.eiroca.library.config.parameter.StringParameter;
import net.eiroca.library.core.Helper;
import net.eiroca.library.db.DBConfig;
import net.eiroca.library.db.LibDB;
import net.eiroca.library.sysadm.monitoring.sdk.ServerContext;
import net.eiroca.sysadm.tools.sysadmserver.SystemContext;
import net.eiroca.sysadm.tools.sysadmserver.event.Alert;
import net.eiroca.sysadm.tools.sysadmserver.exporter.GenericExporter;
import net.eiroca.sysadm.tools.sysadmserver.handler.AlertHandlerContext;
import net.eiroca.sysadm.tools.sysadmserver.manager.CollectorManager;

public class DBAlertExporter extends GenericAlertExporter {

  public static final String ID = "db".toLowerCase();

  protected static transient BooleanParameter _dbEnabled = new BooleanParameter(GenericExporter.config, "enabled", false);
  protected static transient IntegerParameter _maxSize = new IntegerParameter(GenericExporter.config, "maxSize", 250);
  protected static transient StringParameter _tableName = new StringParameter(GenericExporter.config, "tableName", null);
  protected static transient ListParameter _tableFields = new ListParameter(GenericExporter.config, "tableFields", null);

  public int config_maxSize;
  public String config_tableName;
  public String[] config_tableFields;

  private transient DBConfig dbConfig = new DBConfig(null);
  private transient Connection conn = null;

  public DBAlertExporter(final String prefix) {
    super(prefix);
  }

  @Override
  public String getId() {
    return DBAlertExporter.ID;
  }

  @Override
  public void setup(final AlertHandlerContext context) throws Exception {
    super.setup(context);
    final Properties exporterConfig = Helper.getSubConfig(SystemContext.properties, param_prefix);
    final ServerContext dbcontext = new ServerContext(param_prefix, exporterConfig);
    dbcontext.setCredentialProvider(SystemContext.keyStore);
    dbConfig.setup(dbcontext);
  }

  @Override
  public void process(final Alert a) {
    if ((config_tableName == null) || (config_tableFields == null)) { return; }
    final List<Object> vals = new ArrayList<>();
    vals.clear();
    vals.add(System.currentTimeMillis() + "." + Helper.getNextCountID());
    final String state = (a.end != null) ? "CLOSED" : "OPEN";
    vals.add(state);
    vals.add(a.start);
    vals.add(a.end);
    vals.add(a.message);
    switch (a.severity) {
      case CRITICAL:
        vals.add(1);
        break;
      case SEVERE:
        vals.add(2);
        break;
      case WARN:
        vals.add(3);
        break;
      default:
        vals.add(4);
        break;
    }
    vals.add(a.tags.get("host"));
    final String[] fields = config_tableFields;
    if (vals.size() != fields.length) {
      final StringBuilder sb = new StringBuilder();
      Helper.writeList(sb, vals);
      CollectorManager.logger.error("Invalid data: " + sb);
      CollectorManager.logger.debug("fields: " + fields.length);
    }
    else {
      final StringBuilder sb = new StringBuilder();
      Helper.writeList(sb, vals);
      try {
        if (SystemContext.config.dryrun) {
          CollectorManager.logger.info(sb.toString());
        }
        else {
          if (conn == null) {
            CollectorManager.logger.debug("Getting a new connection to DB");
            conn = dbConfig.getConnection();
            CollectorManager.logger.debug((conn != null) ? "Connection: OK" : "Connection error: " + dbConfig.getLastError());
          }
          if (conn != null) {
            CollectorManager.logger.debug(MessageFormat.format("Inserting {0}: {1} ", config_tableName, sb.toString()));
            LibDB.insertRecord(conn, config_tableName, fields, vals.toArray(), config_maxSize);
          }
          else {
            CollectorManager.logger.debug("No connection for inserting");
          }
        }
      }
      catch (final SQLIntegrityConstraintViolationException e) {
        CollectorManager.logger.warn("SQLIntegrityConstraintViolationException");
        CollectorManager.logger.debug("SQLException", e);
      }
      catch (final SQLException e) {
        CollectorManager.logger.error("SQLException", e);
        Helper.close(conn);
        conn = null;
      }
    }
  }

}
