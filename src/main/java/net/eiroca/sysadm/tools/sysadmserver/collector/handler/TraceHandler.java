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
import net.eiroca.sysadm.tools.sysadmserver.SystemContext;
import net.eiroca.sysadm.tools.sysadmserver.collector.GenericRuleBasedHandler;

public class TraceHandler extends GenericRuleBasedHandler<TraceRule> {

  public static final Logger traceLogger = Logs.getLogger("Traces");

  @Override
  public void init(Properties config) throws Exception {
    loadRules(RULE_FILEEXT, SystemContext.config.trace_rules_path);
  }

  @Override
  protected TraceRule createRule(final String name, final Properties config) {
    return new TraceRule(name, config);
  }

  public boolean process(final String namespace, final String body) {
    TraceRule rule = getRule(namespace);
    if (rule != null) { return rule.process(body); }
    return true;
  }

}
