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
package net.eiroca.sysadm.tools.sysadmserver;

import net.eiroca.sysadm.tools.sysadmserver.collector.JsonTransformer;
import net.eiroca.sysadm.tools.sysadmserver.collector.MeasureCollector;
import net.eiroca.sysadm.tools.sysadmserver.collector.ResultTransformer;
import net.eiroca.sysadm.tools.sysadmserver.collector.action.AboutAction;
import net.eiroca.sysadm.tools.sysadmserver.collector.action.ExportAction;
import net.eiroca.sysadm.tools.sysadmserver.collector.action.FeedAction;
import net.eiroca.sysadm.tools.sysadmserver.collector.action.MetricAction;
import spark.ResponseTransformer;
import spark.Spark;

public class CollectorManager {

  public static final String SERVER_APINAME = "Measure Collector";
  public static final String SERVER_APIVERS = "0.0.2";

  private static boolean started = false;

  public static void start() {
    if (SystemContext.config.collector_enabled) {
      CollectorManager.initServer();
      CollectorManager.initAction();
      CollectorManager.started = true;
    }
  }

  public static void stop() {
    if (CollectorManager.started) {
      Spark.stop();
      CollectorManager.started = false;
    }
  }

  public static void initServer() {
    Spark.port(CollectorManager.getServerPort());
  }

  public static void initAction() {
    final ResponseTransformer jSonRender = new JsonTransformer();
    final ResponseTransformer resultRender = new ResultTransformer(false);
    //
    Spark.get("/about", new AboutAction(), jSonRender);
    Spark.get("/rest/feed/" + MeasureCollector.PARAM_NAMESPACE, new FeedAction(), jSonRender);
    Spark.get("/rest/metric/" + MeasureCollector.PARAM_NAMESPACE, new MetricAction(), resultRender);
    Spark.get("/rest/export/" + MeasureCollector.PARAM_NAMESPACE, new ExportAction(), resultRender);
    //
    Spark.get("/rest/feed", new FeedAction(), jSonRender);
    Spark.get("/rest/metric", new MetricAction(), resultRender);
    Spark.get("/rest/export", new ExportAction(), resultRender);
    Spark.get("/rest/export/", new ExportAction(), resultRender);
    //
    Spark.get("/api/v1/feed/" + MeasureCollector.PARAM_NAMESPACE, new FeedAction(), jSonRender);
    Spark.get("/api/v1/metric/" + MeasureCollector.PARAM_NAMESPACE, new MetricAction(), resultRender);
    Spark.get("/api/v1/export/" + MeasureCollector.PARAM_NAMESPACE, new ExportAction(), resultRender);
    Spark.get("/api/v1/export/", new ExportAction(), resultRender);
    //
    Spark.exception(Exception.class, (e, request, response) -> {
      e.printStackTrace();
    });
  }

  private static int getServerPort() {
    return SystemContext.config.collector_port;
  }

}
