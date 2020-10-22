/**
 *
 * Copyright (C) 1999-2020 Enrico Croce - AGPL >= 3.0
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

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import net.eiroca.ext.library.gson.SimpleGson;
import net.eiroca.library.core.Helper;
import net.eiroca.library.db.LibDB;
import net.eiroca.sysadm.tools.sysadmserver.SystemContext;
import net.eiroca.sysadm.tools.sysadmserver.event.Alert;
import net.eiroca.sysadm.tools.sysadmserver.event.EventSeverity;
import spark.Request;

public class AlertCollector {

  public static final String PARAM_NAMESPACE = ":namespace";
  public static final String DEFALT_NAMESPACE = "unknown";

  private static AlertCollector collector = null;
  private final ConcurrentHashMap<String, List<Alert>> alerts;

  public static synchronized AlertCollector getCollector() {
    if (AlertCollector.collector == null) {
      AlertCollector.collector = new AlertCollector();
    }
    return AlertCollector.collector;
  }

  private AlertCollector() {
    alerts = new ConcurrentHashMap<>();
  }

  public static final String getNamespace(final Request request) {
    String namespace = request.params(AlertCollector.PARAM_NAMESPACE);
    if (namespace == null) {
      namespace = AlertCollector.DEFALT_NAMESPACE;
    }
    return namespace;
  }

  public synchronized List<Alert> getAlerts(final String namespace) {
    List<Alert> space = alerts.get(namespace);
    if (space == null) {
      space = new ArrayList<>();
      alerts.put(namespace, space);
    }
    return space;
  }

  public synchronized Alert addAlertFormJson(final String namespace, final String data) {
    Alert alert = null;
    if (data != null) {
      final List<Alert> space = getAlerts(namespace);
      SimpleGson j = null;
      try {
        alert = new Alert();
        j = new SimpleGson(data);
        final String start = j.getNode("start").getAsString();
        final String end = j.getNode("start").getAsString();
        final String message = j.getNode("message").getAsString();
        final String severity = j.getNode("severity").getAsString();
        final String host = j.getNode("host").getAsString();
        alert.start = (start != null) ? ISO8601.parse(start) : null;
        alert.end = (end != null) ? ISO8601.parse(end) : null;
        alert.message = message;
        try {
          alert.severity = EventSeverity.valueOf(severity);

        }
        catch (final IllegalArgumentException e) {
          alert.severity = EventSeverity.WARN;
        }
        if (host != null) {
          alert.tag.add("host", host);
        }
        space.add(alert);
      }
      catch (final Exception e) {
        SystemContext.logger.warn("Invalid json: " + data);

      }
      flush(space);
    }
    return alert;
  }

  //--
  // config

  // data
  public final SimpleDateFormat ISO8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");

  private long count = 0;
  private Connection conn = null;

  private void flush(final List<Alert> space) {
    final List<Object> vals = new ArrayList<>();
    final Iterator<Object> i = vals.iterator();
    Alert a;
    while (i.hasNext()) {
      a = (Alert)i.next();
      vals.remove(a);
      vals.clear();
      count++;
      vals.add(System.currentTimeMillis() + "." + count);
      vals.add(a.state);
      vals.add(a.start);
      vals.add(a.end);
      vals.add(a.message);
      vals.add(a.severity);
      vals.add(a.tag.tagValue("host"));
      exportIncident(vals);
    }
  }

  private void exportIncident(final List<Object> vals) {
    final String[] fields = SystemContext.alertCollectorConfig.tableFields;
    if ((vals == null) || (vals.size() != fields.length)) {
      SystemContext.logger.error("invalid data " + vals);
    }
    else {
      if (conn == null) {
        conn = SystemContext.alertCollectorConfig.dbConfig.getConnection();
      }
      final StringBuilder sb = new StringBuilder();
      Helper.writeList(sb, vals);
      try {
        if (SystemContext.config.dryrun) {
          SystemContext.logger.info(sb.toString());
        }
        else {
          SystemContext.logger.trace(sb.toString());
          LibDB.insertRecord(conn, SystemContext.alertCollectorConfig.tableName, fields, vals.toArray(), SystemContext.alertCollectorConfig.maxSize);
        }
      }
      catch (final SQLIntegrityConstraintViolationException e) {
        SystemContext.logger.warn("SQLIntegrityConstraintViolationException");
      }
      catch (final SQLException e) {
        SystemContext.logger.error("SQLException", e);
        Helper.close(conn);
        conn = null;
      }
    }
  }
}
