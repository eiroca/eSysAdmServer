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
package net.eiroca.sysadm.tools.sysadmserver.collector;

import java.util.concurrent.ConcurrentHashMap;
import net.eiroca.library.metrics.Statistic;
import spark.Request;

public class MeasureCollector {

  public static final String PARAM_NAMESPACE = ":namespace";
  public static final String DEFALT_NAMESPACE = "unknown";

  private static MeasureCollector collector = null;
  private ConcurrentHashMap<String, ConcurrentHashMap<String, Statistic>> measures;

  public static synchronized MeasureCollector getCollector() {
    if (MeasureCollector.collector == null) {
      MeasureCollector.collector = new MeasureCollector();
    }
    return MeasureCollector.collector;
  }

  private MeasureCollector() {
    measures = new ConcurrentHashMap<>();
  }

  public static final String getNamespace(final Request request) {
    String namespace = request.params(MeasureCollector.PARAM_NAMESPACE);
    if (namespace == null) {
      namespace = MeasureCollector.DEFALT_NAMESPACE;
    }
    return namespace;
  }

  public synchronized ConcurrentHashMap<String, Statistic> getMetrics(final String namespace) {
    ConcurrentHashMap<String, Statistic> space = measures.get(namespace);
    if (space == null) {
      space = new ConcurrentHashMap<>();
      measures.put(namespace, space);
    }
    return space;
  }

  public synchronized Statistic getMetric(final String namespace, final String metric) {
    final ConcurrentHashMap<String, Statistic> space = getMetrics(namespace);
    Statistic measure = space.get(metric);
    if (measure == null) {
      measure = new Statistic(metric);
      space.put(metric, measure);
    }
    return measure;
  }

  public synchronized ConcurrentHashMap<String, Statistic> exportMeasures(final String namespace) {
    final ConcurrentHashMap<String, Statistic> result = getMetrics(namespace);
    measures.put(namespace, new ConcurrentHashMap<>());
    return result;
  }

  public synchronized ConcurrentHashMap<String, ConcurrentHashMap<String, Statistic>> exportMeasures() {
    final ConcurrentHashMap<String, ConcurrentHashMap<String, Statistic>> result = measures;
    measures = new ConcurrentHashMap<>();
    return result;
  }
}
