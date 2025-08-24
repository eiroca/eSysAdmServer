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
package net.eiroca.sysadm.tools.sysadmserver.collector;

import java.text.MessageFormat;
import net.eiroca.library.core.Helper;
import net.eiroca.library.server.ServerResponse;
import net.eiroca.sysadm.tools.sysadmserver.SystemContext;
import net.eiroca.sysadm.tools.sysadmserver.collector.handler.UserRoleConfig;
import net.eiroca.sysadm.tools.sysadmserver.manager.CollectorManager;
import spark.Request;
import spark.Response;
import spark.Route;

public abstract class GenericAction implements Route {

  private static final ServerResponse LICENCE_ERROR = new ServerResponse(-9999, "License is expired, no action is taken");
  private static final ServerResponse PERMISSION_ERROR = new ServerResponse(-9998, "No permission to execute the action");

  protected String permission = null;
  protected String name = null;

  public GenericAction(final String permAction) {
    permission = permAction;
    name = getClass().getSimpleName();
  }

  @Override
  final public Object handle(final Request request, final Response response) throws Exception {
    if (!SystemContext.isLicenseValid()) { return GenericAction.LICENCE_ERROR; }
    UserRoleConfig role = canRun(request);
    String ip = request.ip();
    if (ip == null) ip = "-";
    if (role == null) {
      CollectorManager.logger.warn(MessageFormat.format("{0}|{1}|-|-|-|PERMISSION_ERROR", name, ip));
      return GenericAction.PERMISSION_ERROR;
    }
    String roleName = role.getName();
    String thread = Thread.currentThread().getName();
    CollectorManager.logger.debug(MessageFormat.format("{0}|{1}|{2}|START", name, roleName, thread));
    long t = System.currentTimeMillis();
    String err = "-";
    Object o = null;
    String namespace = null;
    try {
      namespace = GenericHandler.getNamespace(request);
      o = execute(namespace, request, response);
    }
    catch (final Exception e) {
      err = Helper.getExceptionAsString(e);
      throw e;
    }
    finally {
      t = System.currentTimeMillis() - t;
      CollectorManager.logger.info(MessageFormat.format("{0}|{1}|{2}|{3}|{4,number,#}|{5}", name, ip, roleName, namespace, t, err));
      CollectorManager.logger.debug(MessageFormat.format("{0}|{1}|{2}|END", name, roleName, thread));
    }
    return o;
  }

  abstract public Object execute(String namespace, final Request request, final Response response) throws Exception;

  protected UserRoleConfig canRun(final Request request) {
    if (permission == null) { return null; }
    UserRoleConfig role = SystemContext.userRoleHandler.getRole(request);
    if (role != null) {
      if (!role.isAllowed(permission)) {
        role = null;
      }
    }
    return role;
  }

}
