/**
 *
 * Copyright (C) 1999-2019 Enrico Croce - AGPL >= 3.0
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
import net.eiroca.sysadm.tools.sysadmserver.SystemContext;
import net.eiroca.sysadm.tools.sysadmserver.collector.MeasureCollector;
import net.eiroca.sysadm.tools.sysadmserver.collector.RestUtils;
import spark.Request;
import spark.Response;
import spark.Route;

public class MetricAction implements Route {

  @Override
  public Object handle(final Request request, final Response response) throws Exception {
    if (!SystemContext.isLicenseValid()) { return SystemContext.LICENCE_ERROR; }
    final String namespace = MeasureCollector.getNamespace(request);
    SystemContext.logger.info(MessageFormat.format("handle({0})", namespace));
    final ResultResponse result = new ResultResponse(0);
    result.message = MessageFormat.format("Namespace: {0}", namespace);
    final StringBuilder sb = new StringBuilder(1024);
    sb.append('{');
    RestUtils.measures2json(sb, MeasureCollector.getCollector().getMetrics(namespace));
    sb.append('}');
    result.setResult(sb.toString());
    return result;
  }
}
