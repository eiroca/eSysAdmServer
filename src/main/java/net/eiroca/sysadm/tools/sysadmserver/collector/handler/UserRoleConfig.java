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
import java.util.Set;
import net.eiroca.library.core.Helper;
import net.eiroca.library.data.Tags;
import net.eiroca.sysadm.tools.sysadmserver.collector.GenericRule;

public class UserRoleConfig extends GenericRule {

  private static final String PROP_DESCRIPTION = "description";
  private String description;
  private final Tags permissions = new Tags();

  public UserRoleConfig(final String name, final Properties config) {
    super(name, config);
  }

  @Override
  protected void readConf(final Properties config) {
    final Set<String> keys = config.stringPropertyNames();
    for (final String key : keys) {
      if (key.equals(UserRoleConfig.PROP_DESCRIPTION)) {
        description = config.getProperty(UserRoleConfig.PROP_DESCRIPTION);
      }
      if (Helper.getBoolean(config.getProperty(key), true)) {
        addPermission(key);
      }
    }
  }

  public void addPermission(final String name) {
    permissions.add(name);
  }

  public void removePermission(final String name) {
    permissions.remove(name);
  }

  public boolean isAllowed(final String name) {
    return permissions.contains(name);
  }

  public String getDescription() {
    return description;
  }

}
