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
package net.eiroca.sysadm.tools.sysadmserver;

import java.util.concurrent.ConcurrentHashMap;
import net.eiroca.library.core.Helper;
import net.eiroca.library.measure.GenericAggregatedMeasure;
import spark.Request;

public class MeasureCollector {

  public static final int SERVER_PORT = 1972;
  public static final String CFG_SERVERPORT = "server.port";
  public static final String SERVER_APINAME = "Measure Collector";
  public static final String SERVER_APIVERS = "0.0.1";

  public static final String PARAM_NAMESPACE = ":namespace";

  public static final String DEFALT_NAMESPACE = "unknown";

  public static final String getNamespace(final Request request) {
    String namespace = request.params(MeasureCollector.PARAM_NAMESPACE);
    if (namespace == null) {
      namespace = MeasureCollector.DEFALT_NAMESPACE;
    }
    return namespace;
  }

  private static MeasureCollector collector = null;

  static synchronized public MeasureCollector getCollector() {
    if (MeasureCollector.collector == null) {
      MeasureCollector.collector = new MeasureCollector();
    }
    return MeasureCollector.collector;
  }

  private ConcurrentHashMap<String, ConcurrentHashMap<String, GenericAggregatedMeasure>> measures;

  private MeasureCollector() {
    measures = new ConcurrentHashMap<>();
  }

  public synchronized ConcurrentHashMap<String, GenericAggregatedMeasure> getMetrics(final String namespace) {
    ConcurrentHashMap<String, GenericAggregatedMeasure> space = measures.get(namespace);
    if (space == null) {
      space = new ConcurrentHashMap<>();
      measures.put(namespace, space);
    }
    return space;
  }

  public synchronized GenericAggregatedMeasure getMetric(final String namespace, final String metric) {
    final ConcurrentHashMap<String, GenericAggregatedMeasure> space = getMetrics(namespace);
    GenericAggregatedMeasure measure = space.get(metric);
    if (measure == null) {
      measure = new GenericAggregatedMeasure(metric);
      space.put(metric, measure);
    }
    return measure;
  }

  public synchronized ConcurrentHashMap<String, GenericAggregatedMeasure> exportMeasures(final String namespace) {
    final ConcurrentHashMap<String, GenericAggregatedMeasure> result = getMetrics(namespace);
    measures.put(namespace, new ConcurrentHashMap<>());
    return result;
  }

  public synchronized ConcurrentHashMap<String, ConcurrentHashMap<String, GenericAggregatedMeasure>> exportMeasures() {
    final ConcurrentHashMap<String, ConcurrentHashMap<String, GenericAggregatedMeasure>> result = measures;
    measures = new ConcurrentHashMap<>();
    return result;
  }

  public static int getServerPort() {
    final int port = Helper.getInt(System.getProperty(MeasureCollector.CFG_SERVERPORT), MeasureCollector.SERVER_PORT);
    return port;
  }
}
