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
package net.eiroca.sysadm.tools.sysadmserver.collector.task;

import java.util.Properties;
import org.slf4j.Logger;
import com.google.gson.JsonObject;
import net.eiroca.library.system.Logs;
import net.eiroca.sysadm.tools.sysadmserver.collector.GenericTask;

public class TraceTask extends GenericTask {

  private static final String DEF_LOGGER = "Traces";
  private static final String PROP_LOGGER = "logger";

  private Logger traceLogger;

  public TraceTask() {
  }

  @Override
  public JsonObject run(final JsonObject request) {
    traceLogger.info(request.toString());
    return request;
  }

  @Override
  public void init(final Properties config) throws Exception {
    final String logger = config.getProperty(TraceTask.PROP_LOGGER, TraceTask.DEF_LOGGER);
    traceLogger = Logs.getLogger(logger);
  }

}
