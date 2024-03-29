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
import java.util.SortedMap;
import java.util.TreeMap;
import net.eiroca.library.core.LibFormat;
import net.eiroca.library.core.LibStr;
import net.eiroca.library.metrics.Statistic;
import net.eiroca.library.metrics.datum.Datum;
import net.eiroca.library.server.ServerResponse;
import net.eiroca.library.sysadm.monitoring.sdk.MeasureFields;
import net.eiroca.library.sysadm.monitoring.sdk.MeasureProducer;
import net.eiroca.sysadm.tools.sysadmserver.SystemConfig;
import net.eiroca.sysadm.tools.sysadmserver.SystemContext;
import net.eiroca.sysadm.tools.sysadmserver.collector.MeasureCollector;
import net.eiroca.sysadm.tools.sysadmserver.manager.CollectorManager;
import spark.Request;
import spark.Response;

/**
 * Send Metric(s) to the controller
 *
 * Sample URLs:
 *
 * Measure syntax:
 * <ul>
 * <li>Measure[.SplittingName]:SplitValue=value[modifier],SplitValue=value[modifier],...</li>
 * <li>Measure=value[modifier],Measure=value[modifier],...</li>
 * </ul>
 *
 * value type:
 * <ul>
 * <li>literal (case insensitive)</li>
 * <li>double[modifier]</li>
 * <li>timestamp (yyyy-mm-dd hh:mi:ss or yyyy-mm-dd hh:mi:ss:ms or hh:mi:ss:ms or hh:mi:ss)</li>
 * </ul>
 *
 * modifiers:
 * <ul>
 * <li>s ( * 1000)</li>
 * <li>ms ( * 1)</li>
 * <li>m ( * 60000)</li>
 * <li>ns ( / 1000)</li>
 * <li>h ( * 3600000)</li>
 * <li>% ( / 100)</li>
 * <li>! (0->1 1->0 e.g. true! return 0, 1.5! return 0)</li>
 * </ul>
 *
 * supported literals:
 * <ul>
 * <li>true -> 1</li>
 * <li>false -> 0</li>
 * <li>ok -> 0</li>
 * <li>ko -> 1</li>
 * <li>off -> 0</li>
 * <li>on -> 1</li>
 * </ul>
 *
 *
 * /rest/feed?Metrics.server:cpu=100%,ram=10;Alerts.my_app:webserver=false,tomcat=true
 * /rest/feed?Check.Gugol:time=100ms,connect=10ms,Status=200;Check.eppol:time=100ms,connect=10ms,Status=200
 *
 * /rest/feed?Timer1:Response+Time=123,Bytes=234,Latency=332
 *
 * /rest/feed?ResponseTime=1s;Latency=2ms
 *
 * /rest/feed?Live=true
 *
 */
public class FeedAction extends GenericAction {

  private static final String REGEX_NL = "(\n|\r)+";
  private static final String POST = "POST";

  public final static String NAME = FeedAction.class.getName();
  public final static String PERM = "collector.action.feed";

  public FeedAction() {
    super(FeedAction.PERM);
  }

  @Override
  public Object execute(final String namespace, final Request request, final Response response) throws Exception {
    final ServerResponse result = new ServerResponse(0);
    String[] data = null;
    if (FeedAction.POST.equalsIgnoreCase(request.requestMethod())) {
      final String body = request.body();
      CollectorManager.logger.trace("Body: " + body);
      if (body != null) {
        data = body.split(FeedAction.REGEX_NL);
      }
    }
    else {
      final String queryParams = request.queryString();
      CollectorManager.logger.trace("Query: " + queryParams);
      if (queryParams != null) {
        data = queryParams.split(";");
      }
    }
    // process the request string
    final SortedMap<String, Object> meta = new TreeMap<>();
    meta.put(MeasureFields.FLD_SOURCE, SystemConfig.ME);
    String ip = request.ip();
    if (ip == null) {
      ip = SystemContext.config.hostname;
    }
    meta.put(MeasureFields.FLD_HOST, ip);
    final int rows = processRequestParameter(namespace, data, meta);
    result.message = MessageFormat.format("Namespace: {0} processed: {1} measure(s).", namespace, rows);
    return result;
  }

  public int processRequestParameter(final String namespace, final String[] valuePairs, final SortedMap<String, Object> meta) {
    int rows = 0;
    if (valuePairs == null) { return rows; }
    final MeasureCollector collector = MeasureCollector.getCollector();
    for (String valuePair : valuePairs) {
      if (valuePair == null) {
        continue;
      }
      CollectorManager.logger.debug("Processing " + valuePair);
      String metricName = null;
      String splitName = null;
      final int colonIx = valuePair.indexOf(":");
      if (colonIx > 0) {
        final int mtrcIx = valuePair.indexOf(".");
        if (mtrcIx > 0) {
          metricName = LibStr.urlDecode(valuePair.substring(0, mtrcIx));
          splitName = LibStr.urlDecode(valuePair.substring(mtrcIx + 1, colonIx));
        }
        else {
          splitName = null;
          metricName = LibStr.urlDecode(valuePair.substring(0, colonIx));
        }
        valuePair = valuePair.substring(colonIx + 1);
      }
      // there might be multiple measures separated by ,
      final String[] measuresAndValues = valuePair.split(",");
      String metric = null;
      String split = null;
      for (final String measureAndValue : measuresAndValues) {
        final String[] measureNameAndValue = measureAndValue.split("=");
        if (measureNameAndValue.length != 2) {
          continue;
        }
        final String subMeasureName = LibStr.urlDecode(measureNameAndValue[0]);
        final String measureValue = measureNameAndValue[1];
        final Double doubleValue = LibFormat.getValue(measureValue);
        if (doubleValue == null) {
          continue;
        }
        if (metricName != null) {
          metric = metricName;
          split = subMeasureName;
        }
        else {
          metric = subMeasureName;
        }
        Statistic m = collector.getMetric(namespace, metric);
        m.addValue(doubleValue);
        if (splitName != null) {
          m = (Statistic)m.getSplitting(splitName);
          m.addValue(split, doubleValue);
        }
        if (SystemContext.consumer_metrics != null) {
          final Datum d = new Datum(doubleValue);
          MeasureProducer.exportData(SystemContext.consumer_metrics, m.getMetadata(), namespace, metric, splitName, split, meta, d);
        }
        rows++;
      }
    }
    return rows;
  }

}
