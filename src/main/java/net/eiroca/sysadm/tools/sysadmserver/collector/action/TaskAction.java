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

import io.javalin.http.Context;
import net.eiroca.library.server.ResultResponse;
import net.eiroca.sysadm.tools.sysadmserver.SystemContext;
import net.eiroca.sysadm.tools.sysadmserver.collector.GenericAction;
import net.eiroca.sysadm.tools.sysadmserver.collector.handler.TaskHandler;

public class TaskAction extends GenericAction {

  public final static String PERM = "collector.action.task";

  public TaskAction() {
    super(TaskAction.PERM);
  }

  @Override
  public Object execute(final String namespace, final Context ctx) throws Exception {
    final ResultResponse<Object> result = new ResultResponse<>(0);
    if (!"POST".equals(ctx.req().getMethod())) {
      result.setMessage("Invalid Method");
      result.setStatus(1);
      return result;
    }
    final String id = ctx.pathParam(TaskHandler.PARAM_ID);
    final String body = ctx.body();
    SystemContext.taskHandler.run(namespace, id, body, result);
    return result;
  }

}
