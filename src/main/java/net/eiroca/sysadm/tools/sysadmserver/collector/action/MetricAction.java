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

import java.text.MessageFormat;
import net.eiroca.library.server.ResultResponse;
import net.eiroca.sysadm.tools.sysadmserver.collector.MeasureCollector;
import net.eiroca.sysadm.tools.sysadmserver.collector.util.RestUtils;
import spark.Request;
import spark.Response;

public class MetricAction extends GenericAction {

  public final static String NAME = MetricAction.class.getName();
  public final static String PERM = "collector.action.metric";

  public MetricAction() {
    super(MetricAction.PERM);
  }

  @Override
  public Object execute(final String namespace, final Request request, final Response response) throws Exception {
    final ResultResponse<Object> result = new ResultResponse<>(0);
    result.message = MessageFormat.format("Namespace: {0}", namespace);
    final StringBuilder sb = new StringBuilder(1024);
    sb.append('{');
    RestUtils.measures2json(sb, MeasureCollector.getCollector().getMetrics(namespace));
    sb.append('}');
    result.setResult(sb.toString());
    return result;
  }

}
