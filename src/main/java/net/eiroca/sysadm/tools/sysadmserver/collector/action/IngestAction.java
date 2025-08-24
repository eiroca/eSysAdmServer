/**
 *
 * Copyright (C) 1999-2025 Enrico Croce - AGPL >= 3.0
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
import java.util.Date;
import java.util.SortedMap;
import java.util.TreeMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.eiroca.ext.library.gson.GsonUtil;
import net.eiroca.library.core.Helper;
import net.eiroca.library.core.LibDate;
import net.eiroca.library.data.Tags;
import net.eiroca.library.metrics.Statistic;
import net.eiroca.library.metrics.datum.IDatum;
import net.eiroca.library.server.ServerResponse;
import net.eiroca.library.sysadm.monitoring.sdk.MeasureFields;
import net.eiroca.library.sysadm.monitoring.sdk.MeasureProducer;
import net.eiroca.sysadm.tools.sysadmserver.SystemConfig;
import net.eiroca.sysadm.tools.sysadmserver.SystemContext;
import net.eiroca.sysadm.tools.sysadmserver.collector.GenericAction;
import net.eiroca.sysadm.tools.sysadmserver.collector.handler.MeasureHandler;
import net.eiroca.sysadm.tools.sysadmserver.manager.CollectorManager;
import spark.Request;
import spark.Response;

/**
 * Bulk Ingest Metrics to the controller
 *
 */
public class IngestAction extends GenericAction {

  public final static String PERM = "collector.action.ingest";

  public IngestAction() {
    super(IngestAction.PERM);
  }

  @Override
  public Object execute(final String namespace, final Request request, final Response response) throws Exception {
    final ServerResponse result = new ServerResponse(0);
    final String body = request.body();
    CollectorManager.logger.trace("Body: " + body);
    int rows = 0;
    if (body != null) {
      String ip = request.ip();
      if (ip == null) {
        ip = SystemContext.config.hostname;
      }
      final SortedMap<String, Object> meta = new TreeMap<>();
      meta.put(MeasureFields.FLD_SOURCE, SystemConfig.ME);
      meta.put(MeasureFields.FLD_HOST, ip);
      // process the request string
      rows = processRequestParameter(SystemContext.measureHandler, body, meta);
    }
    result.message = MessageFormat.format("Processed: {0} measure(s).", rows);
    return result;
  }

  public int processRequestParameter(final MeasureHandler collector, final String data, final SortedMap<String, Object> meta) {
    int rows = 0;
    if (data == null) { return rows; }
    final JsonElement json = JsonParser.parseString(data);
    if (json.isJsonArray()) {
      for (final JsonElement e : json.getAsJsonArray()) {
        if (e.isJsonNull()) {
          continue;
        }
        if (e.isJsonObject()) {
          if (processMetric(collector, meta, e.getAsJsonObject())) {
            rows++;
          }
        }

      }
    }
    else if (json.isJsonObject()) {
      if (processMetric(collector, meta, json.getAsJsonObject())) {
        rows++;
      }
    }
    return rows;
  }

  private boolean processMetric(final MeasureHandler collector, final SortedMap<String, Object> baseMeta, final JsonObject json) {
    final SortedMap<String, Object> meta = new TreeMap<>();
    meta.putAll(baseMeta);
    try {
      final Date timestamp = GsonUtil.getDate(json, MeasureFields.FLD_DATETIME, LibDate.ISO8601_2, LibDate.ISO8601_1);
      final String namespace = GsonUtil.getString(json, MeasureFields.FLD_GROUP);
      final String metric = GsonUtil.getString(json, MeasureFields.FLD_METRIC);
      final JsonElement v = json.get(MeasureFields.FLD_VALUE);
      if ((v == null) || (timestamp == null) || (metric == null) || (namespace == null)) { return false; }
      //
      final String splitGroup = GsonUtil.getString(json, MeasureFields.FLD_SPLIT_GROUP);
      final String splitName = GsonUtil.getString(json, MeasureFields.FLD_SPLIT_NAME);
      Statistic m = collector.getMetric(namespace, metric);
      if ((splitGroup != null) && (splitName != null)) {
        m = (Statistic)m.getSplitting(splitGroup);
        m = (Statistic)m.getSplitting(splitName);
      }
      final Tags dTag = m.getTags();
      final IDatum d = m.getDatum();
      d.init(0.0);
      d.setValue(timestamp.getTime(), v.getAsDouble());
      final JsonElement tags = json.get(MeasureFields.FLD_TAGS_ALT);
      if ((tags != null) && (tags.isJsonArray())) {
        for (final JsonElement t : tags.getAsJsonArray()) {
          final String tag = t.getAsString();
          if (tag != null) {
            dTag.add(tag);
          }
        }
      }
      final String source = GsonUtil.getString(json, MeasureFields.FLD_SOURCE);
      if (source != null) {
        meta.put(MeasureFields.FLD_SOURCE, source);
      }
      final String host = GsonUtil.getString(json, MeasureFields.FLD_HOST);
      if (host != null) {
        meta.put(MeasureFields.FLD_HOST, host);
      }
      if (SystemContext.consumer_metrics != null) {
        MeasureProducer.exportData(SystemContext.consumer_metrics, m.getMetadata(), namespace, metric, splitGroup, splitName, meta, d);
      }
      return true;
    }
    catch (final Exception e) {
      CollectorManager.logger.warn(MessageFormat.format("Invalid ingest entry: {0} for {1}", Helper.getExceptionAsString(e), json));
      SystemContext.logger.debug("Ingestion error: " + Helper.getExceptionAsString(e, true));
      return false;
    }
  }

}
