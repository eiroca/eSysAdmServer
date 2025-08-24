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

import java.util.Properties;
import org.slf4j.Logger;
import net.eiroca.library.system.Logs;
import net.eiroca.sysadm.tools.sysadmserver.collector.GenericRule;

public class TraceRule extends GenericRule {

  private static final String DEF_LOGGER = "Traces";
  private static final String PROP_LOGGER = "logger";

  private Logger traceLogger;

  public TraceRule(final String name, final Properties config) {
    super(name, config);
  }

  @Override
  protected void readConf(final Properties config) {
    String logger = config.getProperty(PROP_LOGGER, DEF_LOGGER);
    traceLogger = Logs.getLogger(logger);
  }

  public boolean process(final String body) {
    traceLogger.info(body);
    return true;
  }

}
