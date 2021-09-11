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
package net.eiroca.sysadm.tools.sysadmserver.util.params;

import java.nio.file.Path;
import java.nio.file.Paths;
import net.eiroca.library.config.Parameters;
import net.eiroca.library.config.parameter.PathParameter;
import net.eiroca.library.core.Helper;
import net.eiroca.library.core.LibStr;
import net.eiroca.sysadm.tools.sysadmserver.SystemContext;

public final class LocalPathParameter extends PathParameter {

  String defPath;

  public LocalPathParameter(final Parameters owner, final String paramName, final String defPathStr) {
    super(owner, paramName, null);
    defPath = defPathStr;
  }

  @Override
  public Path getDefault() {
    return Paths.get(defPath);
  }

  @Override
  public Path convertString(String strValue) {
    if (LibStr.isEmptyOrNull(strValue)) {
      strValue = "&" + defPath;
    }
    if (strValue.startsWith("&")) {
      strValue = SystemContext.config.configPath + strValue.substring(1);
    }
    Path value = Helper.getDirPath(strValue);
    if (value == null) {
      value = getDefault();
    }
    return value;
  }

}
