/**
 *
 * Copyright (C) 1999-2026 Enrico Croce - AGPL >= 3.0
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
import net.eiroca.ext.library.gson.GsonCursor;
import net.eiroca.ext.library.gson.SimpleGson;
import net.eiroca.library.server.ResultResponse;
import net.eiroca.sysadm.tools.sysadmserver.SystemContext;
import net.eiroca.sysadm.tools.sysadmserver.collector.GenericAction;

public class TraceAction extends GenericAction {

  public final static String PERM = "collector.action.trace";

  public TraceAction() {
    super(TraceAction.PERM);
  }

  @Override
  public Object execute(final String namespace, final Context ctx) throws Exception {
    final ResultResponse<Object> result = new ResultResponse<>(0);
    String body;
    if ("GET".equals(ctx.req().getMethod())) {
      final SimpleGson data = new SimpleGson(true);
      final GsonCursor json = new GsonCursor(data);
      for (final String a : ctx.queryParamMap().keySet()) {
        final String v = ctx.queryParam(a);
        json.addProperty(a, v);
      }
      body = json.toString();
    }
    else {
      body = ctx.body();
    }
    final boolean ok = SystemContext.traceHandler.process(namespace, body);
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
