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

import java.util.List;
import org.slf4j.Logger;
import net.eiroca.library.core.Registry;
import net.eiroca.library.system.Logs;
import net.eiroca.sysadm.tools.sysadmserver.collector.MeasureCollector;
import net.eiroca.sysadm.tools.sysadmserver.collector.TraceCollector;
import net.eiroca.sysadm.tools.sysadmserver.collector.action.ActionDef.Method;
import net.eiroca.sysadm.tools.sysadmserver.collector.action.ActionDef.Mode;

public class Actions extends Registry<ActionDef> {

  public static final Logger logger = Logs.getLogger();
  public static final Actions registry = new Actions();

  static {
    Actions.define("/about", AboutAction.NAME, Method.GET, Mode.JSON);
    Actions.define("/rest/about", AboutAction.NAME, Method.GET, Mode.JSON);
    //
    Actions.define("/api/v1/alert/" + MeasureCollector.PARAM_NAMESPACE, AlertAction.NAME, Method.GET, Mode.OBJECT);
    Actions.define("/rest/alert/" + MeasureCollector.PARAM_NAMESPACE, AlertAction.NAME, Method.GET, Mode.OBJECT);
    Actions.define("/rest/alert", AlertAction.NAME, Method.GET, Mode.OBJECT);
    //
    Actions.define("/api/v1/export/" + MeasureCollector.PARAM_NAMESPACE, ExportAction.NAME, Method.GET, Mode.OBJECT);
    Actions.define("/api/v1/export", ExportAction.NAME, Method.GET, Mode.OBJECT);
    Actions.define("/rest/export/" + MeasureCollector.PARAM_NAMESPACE, ExportAction.NAME, Method.GET, Mode.OBJECT);
    Actions.define("/rest/export", ExportAction.NAME, Method.GET, Mode.OBJECT);
    //
    Actions.define("/api/v1/feed/" + MeasureCollector.PARAM_NAMESPACE, FeedAction.NAME, Method.POST, Mode.JSON);
    Actions.define("/rest/feed/" + MeasureCollector.PARAM_NAMESPACE, FeedAction.NAME, Method.GET, Mode.JSON);
    Actions.define("/rest/feed/" + MeasureCollector.PARAM_NAMESPACE, FeedAction.NAME, Method.POST, Mode.JSON);
    Actions.define("/rest/feed", FeedAction.NAME, Method.GET, Mode.JSON);
    Actions.define("/rest/feed", FeedAction.NAME, Method.POST, Mode.JSON);
    //
    Actions.define("/api/v1/metric/" + MeasureCollector.PARAM_NAMESPACE, MetricAction.NAME, Method.GET, Mode.OBJECT);
    Actions.define("/rest/metric/" + MeasureCollector.PARAM_NAMESPACE, MetricAction.NAME, Method.GET, Mode.OBJECT);
    Actions.define("/rest/metric", MetricAction.NAME, Method.GET, Mode.OBJECT);
    //
    Actions.define("/api/v1/ingest", IngestAction.NAME, Method.POST, Mode.JSON);
    Actions.define("/rest/ingest", IngestAction.NAME, Method.POST, Mode.JSON);
    //
    Actions.define("/api/v1/log/" + TraceCollector.PARAM_NAMESPACE, TraceAction.NAME, Method.GET, Mode.JSON);
    Actions.define("/api/v1/log/" + TraceCollector.PARAM_NAMESPACE, TraceAction.NAME, Method.POST, Mode.JSON);
    Actions.define("/rest/log/" + TraceCollector.PARAM_NAMESPACE, TraceAction.NAME, Method.GET, Mode.JSON);
    Actions.define("/rest/log/" + TraceCollector.PARAM_NAMESPACE, TraceAction.NAME, Method.POST, Mode.JSON);
  }

  public static List<String> getActionNames() {
    return Actions.registry.getNames();
  }

  public static void define(final String route, final String clazzname, final Method method, final Mode mode) {
    Actions.registry.addEntry(route, new ActionDef(clazzname, method, mode));
  }

}
