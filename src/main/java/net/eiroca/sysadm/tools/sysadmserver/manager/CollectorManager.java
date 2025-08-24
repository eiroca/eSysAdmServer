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
package net.eiroca.sysadm.tools.sysadmserver.manager;

import org.slf4j.Logger;
import net.eiroca.library.core.Helper;
import net.eiroca.library.system.Logs;
import net.eiroca.sysadm.tools.sysadmserver.SystemConfig;
import net.eiroca.sysadm.tools.sysadmserver.SystemContext;
import net.eiroca.sysadm.tools.sysadmserver.collector.ActionDef;
import net.eiroca.sysadm.tools.sysadmserver.collector.Actions;
import net.eiroca.sysadm.tools.sysadmserver.collector.GenericAction;
import net.eiroca.sysadm.tools.sysadmserver.collector.util.JsonTransformer;
import net.eiroca.sysadm.tools.sysadmserver.collector.util.ResultTransformer;
import spark.ResponseTransformer;
import spark.Spark;

public class CollectorManager extends GenericManager {

  private static final String COLLECTORNAME = SystemConfig.ME + ".collector";

  public static final Logger logger = Logs.getLogger(CollectorManager.COLLECTORNAME);

  public static final String SERVER_APINAME = "Measure Collector";
  public static final String SERVER_APIVERS = "0.0.4";

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

  public static GenericAction buildAction(final ActionDef def) {
    GenericAction obj = null;
    if (def != null) {
      try {
        obj = (GenericAction)Class.forName(def.getClassName()).newInstance();
      }
      catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
        CollectorManager.logger.error(Helper.getExceptionAsString("Unable to create class " + def.getClassName(), e, false));
      }
    }
    return obj;
  }

  public void initServer() {
    Spark.port(getServerPort());
    //
    final ResponseTransformer jSonRender = new JsonTransformer();
    final ResponseTransformer resultRender = new ResultTransformer(false);
    //
    for (final String actionName : Actions.getActionNames()) {
      if (actionName == null) {
        continue;
      }
      final ActionDef def = Actions.registry.get(actionName);
      final GenericAction action = CollectorManager.buildAction(def);
      ResponseTransformer t;
      switch (def.getMode()) {
        case JSON: {
          t = jSonRender;
          break;
        }
        default: {
          t = resultRender;
          break;
        }
      }
      switch (def.getMethod()) {
        case POST: {
          Spark.post(actionName, action, t);
        }
        default: {
          Spark.get(actionName, action, t);
        }
      }
    }
    Spark.exception(Exception.class, (e, request, response) -> {
      SystemContext.logger.error("Collector Error:" + e.getMessage(), e);
    });
  }

  private int getServerPort() {
    return SystemContext.config.collector_port;
  }

}
