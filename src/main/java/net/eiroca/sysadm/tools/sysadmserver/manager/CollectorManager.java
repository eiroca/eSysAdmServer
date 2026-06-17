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
package net.eiroca.sysadm.tools.sysadmserver.manager;

import java.util.function.Consumer;
import org.slf4j.Logger;
import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import io.javalin.http.ExceptionHandler;
import net.eiroca.library.system.Logs;
import net.eiroca.sysadm.tools.sysadmserver.SystemConfig;
import net.eiroca.sysadm.tools.sysadmserver.SystemContext;
import net.eiroca.sysadm.tools.sysadmserver.collector.GenericHandler;
import net.eiroca.sysadm.tools.sysadmserver.collector.action.AboutAction;
import net.eiroca.sysadm.tools.sysadmserver.collector.action.AlertAction;
import net.eiroca.sysadm.tools.sysadmserver.collector.action.ExportAction;
import net.eiroca.sysadm.tools.sysadmserver.collector.action.FeedAction;
import net.eiroca.sysadm.tools.sysadmserver.collector.action.IngestAction;
import net.eiroca.sysadm.tools.sysadmserver.collector.action.MetricAction;
import net.eiroca.sysadm.tools.sysadmserver.collector.action.TaskAction;
import net.eiroca.sysadm.tools.sysadmserver.collector.action.TraceAction;
import net.eiroca.sysadm.tools.sysadmserver.handler.TaskHandler;

public class CollectorManager extends GenericManager implements Consumer<JavalinConfig>, ExceptionHandler<Exception> {

  private static final String COLLECTORNAME = SystemConfig.ME + ".collector";

  public static final Logger logger = Logs.getLogger(CollectorManager.COLLECTORNAME);

  public static final String SERVER_APINAME = "Measure Collector";
  public static final String SERVER_APIVERS = "0.0.4";

  private static Javalin server;

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
    CollectorManager.server.stop();
  }

  public void initServer() {
    CollectorManager.server = Javalin.create(this);
    CollectorManager.server.start(getServerPort());
  }

  private int getServerPort() {
    return SystemContext.config.collector_port;
  }

  @Override
  public void accept(final JavalinConfig serverConfig) {
    final AboutAction aboutAction = new AboutAction();
    serverConfig.routes.get("/about", aboutAction);
    serverConfig.routes.get("/rest/about", aboutAction);

    final AlertAction alertAction = new AlertAction();
    serverConfig.routes.post(String.format("/api/v1/alert/{%s}", GenericHandler.PARAM_NAMESPACE), alertAction);
    serverConfig.routes.post(String.format("/rest/alert/{%s}", GenericHandler.PARAM_NAMESPACE), alertAction);
    serverConfig.routes.get(String.format("/api/v1/alert/{%s}", GenericHandler.PARAM_NAMESPACE), alertAction);
    serverConfig.routes.get(String.format("/rest/alert/{%s}", GenericHandler.PARAM_NAMESPACE), alertAction);
    serverConfig.routes.post("/rest/alert", alertAction);

    final ExportAction exportAction = new ExportAction();
    serverConfig.routes.get(String.format("/api/v1/export/{%s}", GenericHandler.PARAM_NAMESPACE), exportAction);
    serverConfig.routes.get(String.format("/rest/export/{%s}", GenericHandler.PARAM_NAMESPACE), exportAction);
    serverConfig.routes.get("/api/v1/export", exportAction);
    serverConfig.routes.get("/rest/export", exportAction);

    final FeedAction feedAction = new FeedAction();
    serverConfig.routes.post(String.format("/api/v1/feed/{%s}", GenericHandler.PARAM_NAMESPACE), feedAction);
    serverConfig.routes.get(String.format("/rest/feed/{%s}", GenericHandler.PARAM_NAMESPACE), feedAction);
    serverConfig.routes.post(String.format("/rest/feed/{%s}", GenericHandler.PARAM_NAMESPACE), feedAction);
    serverConfig.routes.get("/rest/feed", feedAction);
    serverConfig.routes.post("/rest/feed", feedAction);

    final MetricAction metricAction = new MetricAction();
    serverConfig.routes.get(String.format("/api/v1/metric/{%s}", GenericHandler.PARAM_NAMESPACE), metricAction);
    serverConfig.routes.get(String.format("/rest/metric/{%s}", GenericHandler.PARAM_NAMESPACE), metricAction);
    serverConfig.routes.get("/rest/metric", metricAction);

    final IngestAction ingestAction = new IngestAction();
    serverConfig.routes.post("/api/v1/ingest", ingestAction);
    serverConfig.routes.post("/rest/ingest", ingestAction);

    final TraceAction traceAction = new TraceAction();
    serverConfig.routes.get(String.format("/api/v1/log/{%s}", GenericHandler.PARAM_NAMESPACE), traceAction);
    serverConfig.routes.get(String.format("/rest/log/{%s}", GenericHandler.PARAM_NAMESPACE), traceAction);
    serverConfig.routes.post(String.format("/api/v1/log/{%s}", GenericHandler.PARAM_NAMESPACE), traceAction);
    serverConfig.routes.post(String.format("/rest/log/{%s}", GenericHandler.PARAM_NAMESPACE), traceAction);

    final TaskAction taskAction = new TaskAction();
    serverConfig.routes.get(String.format("/api/v1/task/{%s}/{%s}", GenericHandler.PARAM_NAMESPACE, TaskHandler.PARAM_ID), taskAction);
    serverConfig.routes.get(String.format("/rest/task/{%s}/{%s}", GenericHandler.PARAM_NAMESPACE, TaskHandler.PARAM_ID), taskAction);
    serverConfig.routes.get(String.format("/api/v1/task/{%s}", TaskHandler.PARAM_ID), taskAction);
    serverConfig.routes.get(String.format("/rest/task/{%s}", TaskHandler.PARAM_ID), taskAction);

    serverConfig.routes.exception(Exception.class, this);
  }

  @Override
  public void handle(final Exception e, final Context ctx) {
    SystemContext.logger.error("Collector Error:" + e.getMessage(), e);
    ctx.status(600);
    ctx.result("Internal error");
  }

}
