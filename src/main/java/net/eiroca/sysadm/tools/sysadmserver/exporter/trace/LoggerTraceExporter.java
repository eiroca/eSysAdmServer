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
package net.eiroca.sysadm.tools.sysadmserver.exporter.trace;

import org.slf4j.Logger;
import net.eiroca.library.config.parameter.StringParameter;
import net.eiroca.library.core.LibStr;
import net.eiroca.library.system.IContext;
import net.eiroca.library.system.Logs;
import net.eiroca.sysadm.tools.sysadmserver.exporter.GenericExporter;

public class LoggerTraceExporter extends GenericTraceExporter {

  public LoggerTraceExporter(String param_prefix) {
    super(param_prefix);
    // TODO Auto-generated constructor stub
  }

  public static final String ID = "log".toLowerCase();
  //
  public static StringParameter _logger = new StringParameter(LoggerTraceExporter.config, "logger", "Traces");
  // Dynamic mapped to parameters
  protected String config_logger;
  //
  protected Logger traceLog = null;

  @Override
  public String getId() {
    return LoggerTraceExporter.ID;
  }

  @Override
  public void setup(final IContext context) throws Exception {
    super.setup(context);
    GenericExporter.config.convert(context, GenericExporter.CONFIG_PREFIX, this, "config_");
    traceLog = LibStr.isNotEmptyOrNull(config_logger) ? Logs.getLogger(config_logger) : null;
  }

  @Override
  public void process(final String trace) {
    traceLog.info(trace);
  }

  @Override
  public boolean beginBulk() {
    return (traceLog != null);
  }

}
