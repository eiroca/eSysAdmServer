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
import net.eiroca.library.core.LibStr;
import net.eiroca.library.server.ResultResponse;
import net.eiroca.sysadm.tools.sysadmserver.SystemContext;
import net.eiroca.sysadm.tools.sysadmserver.collector.GenericAction;

public class AlertAction extends GenericAction {

  public final static String PERM = "collector.action.alert";

  public AlertAction() {
    super(AlertAction.PERM);
  }

  @Override
  public Object execute(final String namespace, final Context ctx) throws Exception {
    final ResultResponse<Object> result = new ResultResponse<>(-1, "Generic Error");
    final String data = ctx.body();
    int cnt = -1;
    if (LibStr.isEmptyOrNull(data)) {
      String msg = ctx.queryParam("message");
      cnt = SystemContext.alertHandler.processAlertsFormMessage(namespace, ctx, msg);
    }
    else {
      cnt = SystemContext.alertHandler.processAlertsFormJson(namespace, ctx, data);
    }
    if (cnt < 0) {
      result.setStatus(-2);
      result.setMessage("No data");
    }
    else if (cnt == 0) {
      result.setStatus(-3);
      result.setMessage("Invalid event(s)");
    }
    else {
      final StringBuilder sb = new StringBuilder();
      sb.append(cnt).append(" event(s) processed.");
      result.setResult(sb.toString());
      result.setStatus(0);
      result.setMessage("OK");
    }
    return result;
  }

}
