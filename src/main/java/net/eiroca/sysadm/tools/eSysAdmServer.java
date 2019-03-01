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
package net.eiroca.sysadm.tools;

import org.slf4j.Logger;
import net.eiroca.library.system.Logs;
import net.eiroca.sysadm.tools.sysadmserver.JsonTransformer;
import net.eiroca.sysadm.tools.sysadmserver.LicenseCheck;
import net.eiroca.sysadm.tools.sysadmserver.MeasureCollector;
import net.eiroca.sysadm.tools.sysadmserver.action.AboutAction;
import net.eiroca.sysadm.tools.sysadmserver.action.ExportAction;
import net.eiroca.sysadm.tools.sysadmserver.action.FeedAction;
import net.eiroca.sysadm.tools.sysadmserver.action.MetricAction;
import spark.ResponseTransformer;
import spark.Spark;

public class eSysAdmServer {

  private static final Logger logger = Logs.getLogger();

  public static void main(final String[] args) {
    LicenseCheck.init();
    if (!LicenseCheck.isValid()) {
      eSysAdmServer.logger.error("Invalid license");
      System.exit(1);
    }
    eSysAdmServer.logger.info(LicenseCheck.ME + " licensed to " + LicenseCheck.license.getHolder());
    Spark.port(MeasureCollector.getServerPort());
    final ResponseTransformer jSonRender = new JsonTransformer();
    Spark.get("/about", new AboutAction(), jSonRender);
    Spark.get("/rest/feed", new FeedAction(), jSonRender);
    Spark.get("/rest/feed/" + MeasureCollector.PARAM_NAMESPACE, new FeedAction(), jSonRender);
    Spark.get("/rest/metric", new MetricAction(), jSonRender);
    Spark.get("/rest/metric/" + MeasureCollector.PARAM_NAMESPACE, new MetricAction(), jSonRender);
    Spark.get("/rest/export", new ExportAction(), jSonRender);
    Spark.get("/rest/export/" + MeasureCollector.PARAM_NAMESPACE, new ExportAction(), jSonRender);

    Spark.get("/api/v1/feed/" + MeasureCollector.PARAM_NAMESPACE, new FeedAction(), jSonRender);
    Spark.get("/api/v1/metric/" + MeasureCollector.PARAM_NAMESPACE, new MetricAction(), jSonRender);
    Spark.get("/api/v1/export/" + MeasureCollector.PARAM_NAMESPACE, new ExportAction(), jSonRender);

    Spark.exception(Exception.class, (e, request, response) -> {
      e.printStackTrace();
    });

  }
}
