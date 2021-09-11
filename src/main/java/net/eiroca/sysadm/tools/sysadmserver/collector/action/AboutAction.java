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
package net.eiroca.sysadm.tools.sysadmserver.collector.action;

import net.eiroca.library.server.ServerResponse;
import net.eiroca.sysadm.tools.sysadmserver.manager.CollectorManager;
import spark.Request;
import spark.Response;

public class AboutAction extends GenericAction {

  private final static ServerResponse ABOUT = new ServerResponse(0, CollectorManager.SERVER_APINAME + " " + CollectorManager.SERVER_APIVERS);

  public AboutAction() {
    super(CollectorManager.PERM_ACTION_ABOUT);
  }

  @Override
  public Object handle(final Request request, final Response response) throws Exception {
    final Object r = super.handle(request, response);
    if (r != null) { return r; }
    return AboutAction.ABOUT;
  }

}
