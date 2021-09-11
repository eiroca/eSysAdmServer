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
package net.eiroca.sysadm.tools.sysadmserver.collector.util;

import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.eiroca.library.core.LibStr;
import net.eiroca.library.metrics.Statistic;

public class RestUtils {

  public static void measures2json(final StringBuilder sb, final ConcurrentHashMap<String, Statistic> measures) {
    boolean first = true;
    for (final Entry<String, Statistic> x : measures.entrySet()) {
      if (first) {
        first = false;
      }
      else {
        sb.append(',');
      }
      final String name = x.getKey();
      final Statistic s = x.getValue();
      LibStr.encodeJson(sb, name);
      sb.append(":");
      s.toJson(sb);
    }
  }

  public static void namespaces2json(final StringBuilder sb, final ConcurrentHashMap<String, ConcurrentHashMap<String, Statistic>> namespaces) {
    boolean first = true;
    for (final Entry<String, ConcurrentHashMap<String, Statistic>> n : namespaces.entrySet()) {
      if (first) {
        first = false;
      }
      else {
        sb.append(',');
      }
      final String name = n.getKey();
      final ConcurrentHashMap<String, Statistic> s = n.getValue();
      LibStr.encodeJson(sb, name);
      sb.append(":{");
      RestUtils.measures2json(sb, s);
      sb.append('}');
    }
  }

}
