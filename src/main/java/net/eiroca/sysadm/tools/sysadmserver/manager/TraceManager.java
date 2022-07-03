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
package net.eiroca.sysadm.tools.sysadmserver.manager;

import java.nio.file.Path;
import java.util.Properties;
import org.slf4j.Logger;
import net.eiroca.library.core.Helper;
import net.eiroca.library.core.LibStr;
import net.eiroca.library.system.Logs;
import net.eiroca.sysadm.tools.sysadmserver.SystemContext;
import net.eiroca.sysadm.tools.sysadmserver.util.TraceRule;

public class TraceManager extends GenericRuleBasedManager<TraceRule> {

  public static final Logger traceLogger = Logs.getLogger("Traces");

  @Override
  public void start() throws Exception {
    super.start();
    loadRules(SystemContext.config.trace_roles_path);
  }

  @Override
  public void stop() throws Exception {
    super.stop();
  }

  @Override
  protected void createRule(final Path confPath) {
    try {
      String name = confPath.getFileName().toString();
      name = name.substring(0, name.length() - GenericRuleBasedManager.RULE_FILEEXT.length());
      if (LibStr.isNotEmptyOrNull(name)) {
        final Properties config = Helper.loadProperties(confPath.toString(), false);
        final TraceRule role = new TraceRule(name, config);
        roles.put(name, role);
      }
    }
    catch (final Exception e) {
      SystemContext.logger.error(confPath + " Error: ", e);
    }
  }

}
