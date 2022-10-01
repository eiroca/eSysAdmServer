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
import net.eiroca.sysadm.tools.sysadmserver.collector.AlertCollector;
import net.eiroca.sysadm.tools.sysadmserver.event.Alert;
import spark.Request;
import spark.Response;

public class AlertAction extends GenericAction {

  public final static String NAME = AlertAction.class.getName();
  public final static String PERM = "collector.action.alert";

  public AlertAction() {
    super(AlertAction.PERM);
  }

  @Override
  public Object execute(final String namespace, final Request request, final Response response) throws Exception {
    final ResultResponse<Object> result = new ResultResponse<>(-1, "Generic Error");
    final StringBuilder sb = new StringBuilder(1024);
    final String data = request.body();
    if (data == null) {
      result.setStatus(-2);
      result.setMessage("No data");
    }
    else {
      final Alert alert = AlertCollector.getCollector().addAlertFormJson(namespace, data);
      if (alert != null) {
        sb.append(alert.toString());
        result.setResult(sb.toString());
        result.setStatus(0);
        result.setMessage("OK");
      }
      else {
        sb.append("{}");
      }

    }
    return result;
  }

}
