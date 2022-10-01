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
package net.eiroca.sysadm.tools.sysadmserver.manager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

abstract public class GenericRuleBasedManager<RuleType> extends GenericManager {

  protected static final String RULE_FILEEXT = ".rule";

  protected final Map<String, RuleType> roles = new HashMap<>();

  protected void loadRules(final Path rules_path) throws IOException {
    roles.clear();
    final Stream<Path> roleConfigs = Files.find(rules_path, 1, (filePath, fileAttr) -> {
      final boolean ok = fileAttr.isRegularFile() && filePath.toString().endsWith(GenericRuleBasedManager.RULE_FILEEXT);
      return ok;
    });
    roleConfigs.forEach(path -> createRule(path));
    roleConfigs.close();
  }

  public RuleType getRole(final String name) {
    return roles.get(name);
  }

  protected abstract void createRule(Path confPath);

}547

