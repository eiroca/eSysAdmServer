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
package net.eiroca.sysadm.tools.sysadmserver.collector;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.eiroca.library.core.Helper;
import net.eiroca.library.db.LibDB;
import net.eiroca.sysadm.tools.sysadmserver.SystemContext;
import net.eiroca.sysadm.tools.sysadmserver.event.Alert;
import net.eiroca.sysadm.tools.sysadmserver.event.EventSeverity;
import net.eiroca.sysadm.tools.sysadmserver.manager.CollectorManager;

public class AlertCollector extends GenericCollector {

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
    CollectorManager.logger.debug("data: " + data);
    if (data != null) {
      JsonObject json = null;
      try {
        json = JsonParser.parseString(data).getAsJsonObject();
        final JsonElement events = json.get("events");
        CollectorManager.logger.debug("events: " + events);
        if (events != null) {
          final JsonArray entries = events.getAsJsonArray();
          for (int i = 0; i < entries.size(); i++) {
            final JsonObject event = entries.get(i).getAsJsonObject().getAsJsonObject("event");
            CollectorManager.logger.trace("event: " + event);
            final Date start = getDate(event, "start", null);
            final Date end = getDate(event, "end", null);
            final String message = get(event, "message", null);
            final String severity = get(event, "severity", "INFO");
            final String host = get(event, "host", null);
            alert = new Alert();
            alert.start = start;
            alert.end = end;
            alert.message = message;
            try {
              alert.severity = severity != null ? EventSeverity.valueOf(severity) : EventSeverity.INFO;
            }
            catch (final IllegalArgumentException e) {
              alert.severity = EventSeverity.WARN;
            }
            if (host != null) {
              alert.tag.add("host", host);
            }
            CollectorManager.logger.trace("alert: {0}", alert);
            try {
              flush(alert);
            }
            catch (final Exception e) {
              CollectorManager.logger.error("Flushing Failed: " + e.getMessage(), e);
            }
          }
        }
      }
      catch (final Exception e) {
        CollectorManager.logger.warn("Invalid json: " + e.getMessage());
        CollectorManager.logger.info("Invalid json: " + data);
      }
    }
    return alert;
  }

  private final String get(final JsonElement event, final String name, final String def) {
    final JsonObject o = event.getAsJsonObject();
    final JsonElement kv = (o != null) ? o.get(name) : null;
    return (kv != null) ? kv.getAsString() : def;
  }

  private final Date getDate(final JsonElement event, final String name, final Date def) {
    Date result = def;
    final JsonObject o = event.getAsJsonObject();
    final JsonElement kv = (o != null) ? o.get(name) : null;
    if (kv != null) {
      final String val = kv.getAsString();
      if (val != null) {
        try {
          result = ISO8601.parse(val);
        }
        catch (final ParseException e) {
          CollectorManager.logger.debug("parsing error: " + o, e);
        }
      }
    }
    return result;
  }

  //--
  // config

  // data
  public final SimpleDateFormat ISO8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXX");

  private long count = 0;
  private Connection conn = null;

  private void flush(final Alert a) {
    final List<Object> vals = new ArrayList<>();
    CollectorManager.logger.debug("flushing: " + a);
    vals.clear();
    vals.add(System.currentTimeMillis() + "." + count);
    final String state = (a.end != null) ? "CLOSED" : "OPEN";
    vals.add(state);
    vals.add(a.start);
    vals.add(a.end);
    vals.add(a.message);
    switch (a.severity) {
      case CRITICAL:
        vals.add(1);
        break;
      case SEVERE:
        vals.add(2);
        break;
      case WARN:
        vals.add(3);
        break;
      default:
        vals.add(4);
        break;
    }
    vals.add(a.tag.tagValue("host"));
    exportIncident(vals);
    count++;
  }

  private void exportIncident(final List<Object> vals) {
    final String[] fields = SystemContext.alertCollectorConfig.tableFields;
    if ((vals == null) || (fields == null) || (vals.size() != fields.length)) {
      final StringBuilder sb = new StringBuilder();
      Helper.writeList(sb, vals);
      CollectorManager.logger.error("Invalid data: " + sb);
      CollectorManager.logger.debug("fields: " + fields.length);
    }
    else {
      final StringBuilder sb = new StringBuilder();
      Helper.writeList(sb, vals);
      try {
        if (SystemContext.config.dryrun) {
          CollectorManager.logger.info(sb.toString());
        }
        else {
          if (conn == null) {
            CollectorManager.logger.debug("Getting a new connection to DB");
            conn = SystemContext.alertCollectorConfig.dbConfig.getConnection();
            CollectorManager.logger.debug("last_error: " + SystemContext.alertCollectorConfig.dbConfig.getLastError());
          }
          CollectorManager.logger.debug(MessageFormat.format("Inserting {0}: {1} ", SystemContext.alertCollectorConfig.tableName, sb.toString()));
          if (conn != null) {
            LibDB.insertRecord(conn, SystemContext.alertCollectorConfig.tableName, fields, vals.toArray(), SystemContext.alertCollectorConfig.maxSize);
          }
        }
      }
      catch (final SQLIntegrityConstraintViolationException e) {
        CollectorManager.logger.warn("SQLIntegrityConstraintViolationException");
      }
      catch (final SQLException e) {
        CollectorManager.logger.error("SQLException", e);
        Helper.close(conn);
        conn = null;
      }
    }
  }
}
