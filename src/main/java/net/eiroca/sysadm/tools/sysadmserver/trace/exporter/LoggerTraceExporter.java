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
package net.eiroca.sysadm.tools.sysadmserver.trace.exporter;

import org.slf4j.Logger;
import net.eiroca.library.config.parameter.StringParameter;
import net.eiroca.library.core.LibStr;
import net.eiroca.library.system.IContext;
import net.eiroca.library.system.Logs;

public class LoggerTraceExporter extends GenericTraceExporter {

  public static final String ID = "logger".toLowerCase();
  //
  public static StringParameter _logger = new StringParameter(LoggerTraceExporter.config, "logger", "Traces");
  // Dynamic mapped to parameters
  protected String config_logger;
  //
  protected Logger traceLog = null;

  public LoggerTraceExporter() {
    super();
  }

  @Override
  public void setup(final IContext context) throws Exception {
    super.setup(context);
    GenericTraceExporter.config.convert(context, GenericTraceExporter.CONFIG_PREFIX, this, "config_");
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

  @Override
  public String getId() {
    return LoggerTraceExporter.ID;
  }

}
