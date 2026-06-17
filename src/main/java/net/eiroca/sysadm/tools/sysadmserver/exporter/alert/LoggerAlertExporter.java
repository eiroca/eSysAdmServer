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

import org.slf4j.Logger;
import net.eiroca.library.config.parameter.StringParameter;
import net.eiroca.library.core.LibStr;
import net.eiroca.library.system.Logs;
import net.eiroca.sysadm.tools.sysadmserver.event.Alert;
import net.eiroca.sysadm.tools.sysadmserver.exporter.GenericExporter;
import net.eiroca.sysadm.tools.sysadmserver.handler.AlertHandlerContext;

public class LoggerAlertExporter extends GenericAlertExporter {

  public static final String ID = "log".toLowerCase();

  protected static transient StringParameter _logger = new StringParameter(GenericExporter.config, "logger", "Alerts");

  public String config_logger;

  public Logger alertLogger = null;

  public LoggerAlertExporter(final String prefix) {
    super(prefix);
  }

  @Override
  public String getId() {
    return LoggerAlertExporter.ID;
  }

  @Override
  public void setup(final AlertHandlerContext context) throws Exception {
    super.setup(context);
    alertLogger = LibStr.isNotEmptyOrNull(config_logger) ? Logs.getLogger(config_logger) : null;
    config_enabled = config_enabled && (alertLogger != null);
  }

  @Override
  public void process(final Alert a) {
    final String msg = getTextFromAlert(a);
    if (msg != null) {
      switch (a.severity) {
        case CRITICAL:
          alertLogger.error(msg);
          break;
        case SEVERE:
        case ERROR:
          alertLogger.error(msg);
          break;
        case WARN:
          alertLogger.warn(msg);
          break;
        case INFO:
          alertLogger.info(msg);
          break;
      }
    }
  }

}
