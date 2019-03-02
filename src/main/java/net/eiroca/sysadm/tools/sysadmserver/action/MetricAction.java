/**
 *
 * Copyright (C) 2001-2019 eIrOcA (eNrIcO Croce & sImOnA Burzio) - AGPL >= 3.0
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
package net.eiroca.sysadm.tools.sysadmserver.action;

import java.text.MessageFormat;
import org.slf4j.Logger;
import net.eiroca.library.server.ResultResponse;
import net.eiroca.library.system.Logs;
import net.eiroca.sysadm.tools.sysadmserver.LicenseCheck;
import net.eiroca.sysadm.tools.sysadmserver.MeasureCollector;
import net.eiroca.sysadm.tools.sysadmserver.Utils;
import spark.Request;
import spark.Response;
import spark.Route;

public class MetricAction implements Route {

  private static Logger logger = Logs.getLogger();

  @Override
  public Object handle(final Request request, final Response response) throws Exception {
    if (!LicenseCheck.isValid()) { return LicenseCheck.LICENCE_ERROR; }
    final String namespace = MeasureCollector.getNamespace(request);
    MetricAction.logger.info(MessageFormat.format("handle({0})", namespace));
    final ResultResponse result = new ResultResponse(0);
    result.message = MessageFormat.format("Namespace: {0}", namespace);
    final StringBuilder sb = new StringBuilder(1024);
    sb.append('{');
    Utils.measures2json(sb, MeasureCollector.getCollector().getMetrics(namespace));
    sb.append('}');
    result.setResult(sb.toString());
    return result;
  }
}
