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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import org.slf4j.Logger;
import net.eiroca.library.data.Pair;
import net.eiroca.library.metrics.Statistic;
import net.eiroca.library.server.ServerResponse;
import net.eiroca.library.system.Logs;
import net.eiroca.sysadm.tools.sysadmserver.LicenseCheck;
import net.eiroca.sysadm.tools.sysadmserver.MeasureCollector;
import spark.Request;
import spark.Response;
import spark.Route;

public class FeedAction implements Route {

  private static Logger logger = Logs.getLogger();

  private static final String REGEX_NL = "(\n|\r)+";

  private static final String POST = "POST";

  /**
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
   * /rest/feed?Gugol:Time.Response=100,Time.Connect=10,Status=200&eppol:Time.Response=100,Time.
   * Connect=10,Status=200
   * /rest/feed?ResponseTime.Site:Gugol=100,Time.Connect=10,Status=200&eppol:Time
   * .Response=100,Time.Connect=10,Status=200
   *
   * /rest/feed?Timer1:Response Time=123,Bytes=234,Latency=332;
   *
   * /rest/feed?ResponseTime=1s;Latency=2ms
   *
   * /rest/feed?Live=true
   *
   * @param request
   * @return
   */

  static ArrayList<Pair<String, Double>> MODIFIERSTR = new ArrayList<>();
  static {
    FeedAction.MODIFIERSTR.add(new Pair<>("%", 1.0 / 100.0));
    FeedAction.MODIFIERSTR.add(new Pair<>("ns", 1 / 1000.0));
    FeedAction.MODIFIERSTR.add(new Pair<>("ms", 1.0));
    FeedAction.MODIFIERSTR.add(new Pair<>("s", 1000.0));
    FeedAction.MODIFIERSTR.add(new Pair<>("\"", 1000.0));
    FeedAction.MODIFIERSTR.add(new Pair<>("'", 60000.0));
    FeedAction.MODIFIERSTR.add(new Pair<>("m", 60000.0));
    FeedAction.MODIFIERSTR.add(new Pair<>("h", 3600000.0));
  }
  static HashMap<String, Double> STRVALUE = new HashMap<>();
  static {
    FeedAction.STRVALUE.put("true", 1.0);
    FeedAction.STRVALUE.put("false", 0.0);
    FeedAction.STRVALUE.put("ok", 0.0);
    FeedAction.STRVALUE.put("ko", 1.0);
    FeedAction.STRVALUE.put("on", 1.0);
    FeedAction.STRVALUE.put("off", 0.0);
  }

  @Override
  public Object handle(final Request request, final Response response) throws Exception {
    if (!LicenseCheck.isValid()) { return LicenseCheck.LICENCE_ERROR; }
    final String namespace = MeasureCollector.getNamespace(request);
    FeedAction.logger.info(MessageFormat.format("handle({0})", namespace));
    final ServerResponse result = new ServerResponse(0);
    String[] data = null;
    if (FeedAction.POST.equalsIgnoreCase(request.requestMethod())) {
      final String body = request.body();
      FeedAction.logger.trace("Body: " + body);
      if (body != null) {
        data = body.split(FeedAction.REGEX_NL);
      }
    }
    else {
      final String queryParams = request.queryString();
      FeedAction.logger.trace("Query: " + queryParams);
      if (queryParams != null) {
        data = queryParams.split(";");
      }
    }

    // process the request string
    final int rows = processRequestParameter(namespace, data);
    result.message = MessageFormat.format("Namespace: {0} processed: {1} measure(s).", namespace, rows);
    return result;
  }

  private String decodeString(final String s) {
    String result;
    try {
      result = URLDecoder.decode(s, "UTF-8");
    }
    catch (final UnsupportedEncodingException e) {
      result = s;
    }
    return result;
  }

  public int processRequestParameter(final String namespace, final String[] valuePairs) {
    int rows = 0;
    if (valuePairs == null) { return rows; }
    final MeasureCollector collector = MeasureCollector.getCollector();
    for (String valuePair : valuePairs) {
      if (valuePair == null) {
        continue;
      }
      FeedAction.logger.debug("Processing " + valuePair);
      String metricName = null;
      String splitName = null;
      final int colonIx = valuePair.indexOf(":");
      if (colonIx > 0) {
        final int mtrcIx = valuePair.indexOf(".");
        if (mtrcIx > 0) {
          metricName = decodeString(valuePair.substring(0, mtrcIx));
          splitName = decodeString(valuePair.substring(mtrcIx + 1, colonIx));
        }
        else {
          splitName = null;
          metricName = decodeString(valuePair.substring(0, colonIx));
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
        final String subMeasureName = decodeString(measureNameAndValue[0]);
        final String measureValue = measureNameAndValue[1];
        final Double doubleValue = getValue(measureValue);
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
          m = m.getSplitting(splitName);
          m.addValue(split, doubleValue);
        }
        rows++;
      }
    }
    return rows;
  }

  public Double getValue(String value) {
    final String oldVal = value;
    if (value == null) { return null; }
    value = value.trim().toLowerCase();
    boolean negated = false;
    double modifier = 1.0;
    Double val = null;
    if (value.endsWith("!")) {
      negated = true;
      value = value.substring(0, value.length() - 1);
    }
    for (final String strVal : FeedAction.STRVALUE.keySet()) {
      if (value.startsWith(strVal)) {
        val = FeedAction.STRVALUE.get(strVal);
        value = value.substring(strVal.length());
        break;
      }
    }
    for (final Pair<String, Double> strVal : FeedAction.MODIFIERSTR) {
      if (value.endsWith(strVal.getLeft())) {
        modifier = strVal.getRight().doubleValue();
        value = value.substring(0, value.length() - strVal.getLeft().length());
        break;
      }
    }
    if (val == null) {
      try {
        val = new Double(Double.parseDouble(value) * modifier);
      }
      catch (final NumberFormatException e) {
      }
    }
    if (negated) {
      if (Math.abs(val) < 0.000001) {
        val = 1.0;
      }
      else {
        val = 0.0;
      }
    }
    FeedAction.logger.trace(MessageFormat.format("original={4} value={0} multiplier={1} negated={2} result={3}", value, modifier, negated, val, oldVal));
    return val;
  }
}
