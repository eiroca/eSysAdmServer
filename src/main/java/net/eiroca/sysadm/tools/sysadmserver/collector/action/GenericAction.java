/**
 *
 * Copyright (C) 1999-2020 Enrico Croce - AGPL >= 3.0
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
package net.eiroca.sysadm.tools.sysadmserver.collector.action;

import net.eiroca.library.server.ServerResponse;
import net.eiroca.sysadm.tools.sysadmserver.SystemContext;
import net.eiroca.sysadm.tools.sysadmserver.util.Role;
import spark.Request;
import spark.Response;
import spark.Route;

public abstract class GenericAction implements Route {

  private static final ServerResponse LICENCE_ERROR = new ServerResponse(-9999, "License is expired, no action is taken");
  private static final ServerResponse PERMISSION_ERROR = new ServerResponse(-9998, "No permission to execute the action");

  protected String permission = null;

  public GenericAction(final String permAction) {
    permission = permAction;
  }

  @Override
  public Object handle(final Request request, final Response response) throws Exception {
    if (!SystemContext.isLicenseValid()) { return GenericAction.LICENCE_ERROR; }
    if (!canRun(request)) { return GenericAction.PERMISSION_ERROR; }
    return null;
  }

  protected boolean canRun(final Request request) {
    if (permission == null) { return true; }
    final Role role = SystemContext.roleManager.getRole(request);
    if (role != null) { return role.isAllowed(permission); }
    return true; // for backward compatibility
  }

}
