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
package net.eiroca.sysadm.tools.sysadmserver.collector.handler;

import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import net.eiroca.library.metrics.Statistic;
import net.eiroca.sysadm.tools.sysadmserver.collector.GenericHandler;

public class MeasureHandler extends GenericHandler {

  private ConcurrentHashMap<String, ConcurrentHashMap<String, Statistic>> measures;

  public MeasureHandler() {
    measures = new ConcurrentHashMap<>();
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

  @Override
  public void init(Properties config) throws Exception {
    // TODO Auto-generated method stub
  }

}
