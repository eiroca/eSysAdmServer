/**
 *
 * Copyright (C) 1999-2022 Enrico Croce - AGPL >= 3.0
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;
import org.slf4j.Logger;
import net.eiroca.library.core.Helper;
import net.eiroca.library.core.LibStr;
import net.eiroca.library.system.Logs;
import net.eiroca.sysadm.tools.sysadmserver.SystemContext;

abstract public class GenericRuleBasedHandler<RuleType> extends GenericHandler {

  public static final Logger logger = Logs.getLogger();

  protected static final String RULE_FILEEXT = ".rule";
  protected static final String CONFIG_FILEEXT = ".config";

  protected final Map<String, RuleType> rules = new HashMap<>();

  protected void loadRules(final String ext, final Path rules_path) throws IOException {
    rules.clear();
    final Stream<Path> roleConfigs = Files.find(rules_path, 1, (filePath, fileAttr) -> {
      final boolean ok = fileAttr.isRegularFile() && filePath.toString().endsWith(ext);
      return ok;
    });
    roleConfigs.forEach(path -> loadRule(path));
    roleConfigs.close();
  }

  public RuleType getRule(final String name) {
    return rules.get(name);
  }

  protected void loadRule(final Path confPath) {
    try {
      String name = confPath.getFileName().toString();
      name = name.substring(0, name.length() - GenericRuleBasedHandler.RULE_FILEEXT.length());
      if (LibStr.isNotEmptyOrNull(name)) {
        final Properties config = Helper.loadProperties(confPath.toString(), false);
        final RuleType rule = createRule(name, config);
        if (rule != null) {
          rules.put(name, rule);
        }
        else {
          GenericRuleBasedHandler.logger.warn("Unalble to create task " + name);
        }
      }
    }
    catch (final Exception e) {
      SystemContext.logger.error(confPath + " Error: ", e);
    }
  }

  abstract protected RuleType createRule(final String name, Properties config);

}
