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

import java.text.MessageFormat;
import net.eiroca.library.server.ResultResponse;
import net.eiroca.sysadm.tools.sysadmserver.collector.AlertCollector;
import net.eiroca.sysadm.tools.sysadmserver.collector.MeasureCollector;
import net.eiroca.sysadm.tools.sysadmserver.event.Alert;
import net.eiroca.sysadm.tools.sysadmserver.manager.CollectorManager;
import spark.Request;
import spark.Response;

public class AlertAction extends GenericAction {

  public AlertAction() {
    super(CollectorManager.PERM_ACTION_ALERT);
  }

  @Override
  public Object handle(final Request request, final Response response) throws Exception {
    final Object r = super.handle(request, response);
    if (r != null) { return r; }
    final String namespace = MeasureCollector.getNamespace(request);
    CollectorManager.logger.info(MessageFormat.format("handle({0})", namespace));
    final ResultResponse<Object> result = new ResultResponse<>(0);
    result.message = MessageFormat.format("Namespace: {0}", namespace);
    final StringBuilder sb = new StringBuilder(1024);
    final String data = request.body();
    final Alert alert = AlertCollector.getCollector().addAlertFormJson(namespace, data);
    sb.append('{').append(alert.toString()).append('}');
    result.setResult(sb.toString());
    return result;
  }

}
