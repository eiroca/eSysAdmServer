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

import net.eiroca.ext.library.gson.GsonCursor;
import net.eiroca.ext.library.gson.SimpleGson;
import net.eiroca.library.server.ResultResponse;
import net.eiroca.sysadm.tools.sysadmserver.SystemContext;
import net.eiroca.sysadm.tools.sysadmserver.collector.GenericAction;
import spark.Request;
import spark.Response;

public class TraceAction extends GenericAction {

  public final static String PERM = "collector.action.trace";

  public TraceAction() {
    super(TraceAction.PERM);
  }

  @Override
  public Object execute(final String namespace, final Request request, final Response response) throws Exception {
    final ResultResponse<Object> result = new ResultResponse<>(0);
    String body;
    if ("GET".equals(request.requestMethod())) {
      SimpleGson data = new SimpleGson(true);
      GsonCursor json = new GsonCursor(data);
      for (final String a : request.queryParams()) {
        final String v = request.queryParams(a);
        json.addProperty(a, v);
      }
      body = json.toString();
    }
    else {
      body = request.body();
    }
    boolean ok = SystemContext.traceHandler.process(namespace, body);
    if (!ok) {
      result.setMessage("KO");
      result.setStatus(1);
    }
    else {
      result.setMessage("OK");
    }
    return result;
  }

}
