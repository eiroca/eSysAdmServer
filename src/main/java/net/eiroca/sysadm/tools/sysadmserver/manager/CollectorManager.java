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
package net.eiroca.sysadm.tools.sysadmserver.manager;

import org.slf4j.Logger;
import net.eiroca.library.system.Logs;
import net.eiroca.sysadm.tools.sysadmserver.SystemConfig;
import net.eiroca.sysadm.tools.sysadmserver.SystemContext;
import net.eiroca.sysadm.tools.sysadmserver.collector.MeasureCollector;
import net.eiroca.sysadm.tools.sysadmserver.collector.action.AboutAction;
import net.eiroca.sysadm.tools.sysadmserver.collector.action.AlertAction;
import net.eiroca.sysadm.tools.sysadmserver.collector.action.ExportAction;
import net.eiroca.sysadm.tools.sysadmserver.collector.action.FeedAction;
import net.eiroca.sysadm.tools.sysadmserver.collector.action.MetricAction;
import net.eiroca.sysadm.tools.sysadmserver.collector.util.JsonTransformer;
import net.eiroca.sysadm.tools.sysadmserver.collector.util.ResultTransformer;
import spark.ResponseTransformer;
import spark.Spark;

public class CollectorManager extends GenericManager {

  private static final String COLLECTORNAME = SystemConfig.ME + ".collector";

  public static final Logger logger = Logs.getLogger(CollectorManager.COLLECTORNAME);

  public static final String SERVER_APINAME = "Measure Collector";
  public static final String SERVER_APIVERS = "0.0.3";

  public static final String PERM_ACTION_ABOUT = "collector.action.about";
  public static final String PERM_ACTION_ALERT = "collector.action.alert";
  public static final String PERM_ACTION_FEED = "collector.action.feed";
  public static final String PERM_ACTION_EXPORT = "collector.action.export";
  public static final String PERM_ACTION_METRIC = "collector.action.metric";

  @Override
  public void start() throws Exception {
    if (SystemContext.config.collector_enabled) {
      super.start();
      initServer();
    }
  }

  @Override
  public void stop() throws Exception {
    super.stop();
    Spark.stop();
  }

  public void initServer() {
    Spark.port(getServerPort());
    //
    final ResponseTransformer jSonRender = new JsonTransformer();
    final ResponseTransformer resultRender = new ResultTransformer(false);
    //
    Spark.get("/about", new AboutAction(), jSonRender);
    Spark.get("/rest/about", new AboutAction(), jSonRender);
    //
    Spark.get("/rest/feed/" + MeasureCollector.PARAM_NAMESPACE, new FeedAction(), jSonRender);
    Spark.post("/rest/feed/" + MeasureCollector.PARAM_NAMESPACE, new FeedAction(), jSonRender);
    Spark.get("/rest/feed", new FeedAction(), jSonRender);
    Spark.post("/rest/feed", new FeedAction(), jSonRender);
    //
    Spark.get("/rest/metric/" + MeasureCollector.PARAM_NAMESPACE, new MetricAction(), resultRender);
    Spark.get("/rest/metric", new MetricAction(), resultRender);
    //
    Spark.get("/rest/export/" + MeasureCollector.PARAM_NAMESPACE, new ExportAction(), resultRender);
    Spark.get("/rest/export", new ExportAction(), resultRender);
    Spark.get("/rest/export/", new ExportAction(), resultRender);
    //
    Spark.get("/rest/alert/" + MeasureCollector.PARAM_NAMESPACE, new AlertAction(), resultRender);
    Spark.post("/rest/alert/" + MeasureCollector.PARAM_NAMESPACE, new AlertAction(), resultRender);
    Spark.get("/rest/alert", new AlertAction(), resultRender);
    Spark.post("/rest/alert", new AlertAction(), resultRender);
    //
    Spark.post("/api/v1/feed/" + MeasureCollector.PARAM_NAMESPACE, new FeedAction(), jSonRender);
    Spark.get("/api/v1/metric/" + MeasureCollector.PARAM_NAMESPACE, new MetricAction(), resultRender);
    Spark.get("/api/v1/export/" + MeasureCollector.PARAM_NAMESPACE, new ExportAction(), resultRender);
    Spark.get("/api/v1/export/", new ExportAction(), resultRender);
    Spark.post("/api/v1/alert/" + MeasureCollector.PARAM_NAMESPACE, new AlertAction(), jSonRender);
    //
    Spark.exception(Exception.class, (e, request, response) -> {
      SystemContext.logger.error("Collector Error:" + e.getMessage(), e);
    });
  }

  private int getServerPort() {
    return SystemContext.config.collector_port;
  }

}
