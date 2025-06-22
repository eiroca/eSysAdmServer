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
package net.eiroca.sysadm.tools;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.eiroca.ext.library.gson.SimpleGson;
import net.eiroca.library.sysadm.monitoring.sdk.MeasureFields;

public class Test {

  public static void main(final String[] args) {
    final SimpleGson j = new SimpleGson("{\"@timestamp\":\"2025-06-11T12:50:32.235+02:00\",\"group\":\"DS\",\"host\":\"10.109.42.149\",\"metric\":\"Metrics\",\"source\":\"eSysAdmServer\",\"split\":\"true\",\"splitGroup\":\"Applicativi\",\"splitName\":\"Device con importo anomalo\",\"value\":0.0}");
    final JsonObject json = j.getRoot();
    StringBuffer row = new StringBuffer(256);
    String source = getIt(json, MeasureFields.FLD_SOURCE, true, false);
    String group = getIt(json, MeasureFields.FLD_GROUP, true, false);
    String metric = getIt(json, MeasureFields.FLD_METRIC, true, false);
    append(row, source, '.');
    append(row, group, '.'); 
    append(row, metric, (char)0);
    String split = getIt(json, MeasureFields.FLD_SPLIT, false, true);
    if ((split != null) && "true".equals(split)) {
      String split_group = getIt(json, MeasureFields.FLD_SPLIT_GROUP, true, true);
      String split_value = getIt(json, MeasureFields.FLD_SPLIT_NAME, false, false);
      row.append(',');
      append(row, split_group, '=');
      row.append('"');
      append(row, split_value, (char)0);
      row.append('"');
    }
    String host = getIt(json, MeasureFields.FLD_HOST, false, false);
    if (host != null) {
      row.append(',');
      row.append("hostname=");
      row.append('"');
      row.append(host);
      row.append('"');
    }

    row.append(' ');
    row.append("gauge,");
    appendDouble(row, json, MeasureFields.FLD_VALUE);
    row.append(' ');
    System.out.println(row);

  }

  static final private String getIt(JsonObject json, String field, boolean removeSpaces, boolean lower) {
    JsonElement elem = json.get(field);
    String result = (elem != null) ? elem.getAsString() : null;
    if (result != null) {
      if (removeSpaces) result = result.replace(' ', '_');
      if (lower) result = result.toLowerCase();
    }

    return result;
  }

  static final private void append(StringBuffer row, String field, char sep) {
    if (field != null) {
      row.append(field);
      if (sep != 0) row.append(sep);
    }
  }

  static final private void appendDouble(StringBuffer row, JsonObject json, String field) {
    JsonElement elem = json.get(field);
    if (elem != null) row.append(elem.getAsDouble());
  }

}
