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

import net.eiroca.library.server.ResultResponse;
import net.eiroca.sysadm.tools.sysadmserver.SystemContext;
import net.eiroca.sysadm.tools.sysadmserver.collector.GenericAction;
import net.eiroca.sysadm.tools.sysadmserver.collector.GenericHandler;
import net.eiroca.sysadm.tools.sysadmserver.collector.handler.TaskHandler;
import spark.Request;
import spark.Response;

public class TaskAction extends GenericAction {

  public final static String PERM = "collector.action.task";

  public static final String getNamespace(final Request request) {
    String namespace = request.params(GenericHandler.PARAM_NAMESPACE);
    if (namespace == null) {
      namespace = GenericHandler.DEFALT_NAMESPACE;
    }
    return namespace;
  }

  public TaskAction() {
    super(TaskAction.PERM);
  }

  @Override
  public Object execute(final String namespace, final Request request, final Response response) throws Exception {
    final ResultResponse<Object> result = new ResultResponse<>(0);
    if (!"POST".equals(request.requestMethod())) {
      result.setMessage("Invalid Method");
      result.setStatus(1);
      return result;
    }
    String id = request.params(TaskHandler.PARAM_ID);
    String body = request.body();
    SystemContext.taskHandler.run(namespace, id, body, result);
    return result;
  }

}
