/**
 *
 * Copyright (C) 1999-2026 Enrico Croce - AGPL >= 3.0
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

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.eiroca.library.core.Helper;
import net.eiroca.library.db.LibDB;
import net.eiroca.library.system.Logs;
import net.eiroca.sysadm.tools.sysadmserver.SystemContext;
import net.eiroca.sysadm.tools.sysadmserver.collector.GenericHandler;
import net.eiroca.sysadm.tools.sysadmserver.event.Alert;
import net.eiroca.sysadm.tools.sysadmserver.event.AlertState;
import net.eiroca.sysadm.tools.sysadmserver.event.EventSeverity;
import net.eiroca.sysadm.tools.sysadmserver.manager.CollectorManager;
import spark.Request;

public class AlertHandler extends GenericHandler {

  private static final Logger alertLogger = Logs.getLogger("Alerts");
  private static final String SPLIT_REG = "[" + AlertConfig.KEY_SEP + "]";

  private final AlertConfig config = new AlertConfig();
  private final ConcurrentHashMap<String, List<Alert>> alerts;

  private long count = 0;
  private Connection conn = null;

  public AlertHandler() {
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

  private static final String[] METADATA = {
      "application", "module", "component", "host"
  };

  private Map<String, String> readTags(final JsonObject json) {
    final Map<String, String> tags = new HashMap<>();
    final JsonArray arr = json.getAsJsonArray("entityTags");
    for (final JsonElement e : arr) {
      final String key = get(e, "key", null);
      final String val = get(e, "value", null);
      if ((key != null) && (val != null)) {
        tags.put(key, val);
      }
    }
    return tags;
  }

  public synchronized int addAlertFormJson(final String namespace, final Request request, final String data) {
    final List<Alert> alerts = new ArrayList<>();
    int cnt = 0;
    CollectorManager.logger.debug("alert json: " + data);
    if (data != null) {
      JsonObject json = null;
      try {
        json = JsonParser.parseString(data).getAsJsonObject();
        if ("dynatrace".equalsIgnoreCase(namespace)) {
          dynatraceAlert(alerts, json);
        }
        else {
          defaultAlert(alerts, json);
        }
      }
      catch (final Exception e) {
        CollectorManager.logger.warn("Invalid json: " + e.getMessage());
        CollectorManager.logger.info("Invalid json: " + data);
      }
    }
    for (Alert alert : alerts) {
      if (alert != null) {
        cnt++;
        for (final String key : AlertHandler.METADATA) {
          final String val = request.queryParams(key);
          if (val != null) {
            alert.tags.put(key, val);
          }
        }
        if (config.validationLevel >= 0) {
          for (int i = 0; i < (config.validationLevel + 1); i++) {
            if (alert.tags.get(AlertHandler.METADATA[i]) == null) {
              alert = null;
              break;
            }
          }
        }
      }
      CollectorManager.logger.trace("alert: {0}", alert);
      try {
        if (alert != null) {
          flush(alert);
        }
      }
      catch (final Exception e) {
        CollectorManager.logger.error("Flushing Failed: " + e.getMessage(), e);
      }
    }
    return cnt;
  }

  private void dynatraceAlert(final List<Alert> alerts, final JsonObject json) {
    final Alert alert;
    final Date start = getDynatraceDate(json, "startTime", null);
    final Date end = getDynatraceDate(json, "endTime", null);
    final String id = get(json, "displayId", null);
    final String message = get(json, "title", null);
    final String severity = get(json, "severityLevel", "SEVERE");
    alert = build(id, start, end, message, severity);
    if (alert != null) {
      final String state = get(json, "status", null);
      switch (state) {
        case "CLOSED":
          alert.state = AlertState.CLOSED;
          break;
        default:
          alert.state = AlertState.INPROGRESS;
          break;
      }
      final Map<String, String> tags = readTags(json);
      CollectorManager.logger.debug("DT tags: " + tags);
      for (final Entry<String, String> e : tags.entrySet()) {
        final String oldtag = e.getKey() + AlertConfig.KEY_SEP + e.getValue();
        final String newtag = config.mapping.get(oldtag);
        if (newtag != null) {
          final String[] t = newtag.split(AlertHandler.SPLIT_REG);
          if (t.length == 2) {
            alert.tags.put(t[0], t[1]);
          }
        }
      }
      String host = findHost(json, "affectedEntities");
      if (host == null) findHost(json, "impactedEntities");
      if (host != null) {
        alert.tags.put(METADATA[3], host);
      }
      alerts.add(alert);
    }
  }

  private String findHost(JsonObject json, String section) {
    String host = null;
    final JsonArray entities = json.getAsJsonArray(section);
    if (entities != null) {
      for (int i = 0; i < entities.size(); i++) {
        final JsonObject entity = entities.get(i).getAsJsonObject();
        if (entity != null) {
          String name = get(entity, "name", null);
          if (name != null) {
            JsonElement id = entity.get("entityId");
            if (id != null) {
              String hostId = get(id, "id", null);
              if ((hostId != null) && (hostId.startsWith("HOST-"))) {
                host = name;
                break;
              }
            }
          }
        }
      }
    }
    CollectorManager.logger.debug("find host in " + section + ": " + host);
    return host;
  }

  public void defaultAlert(final List<Alert> alerts, final JsonObject json) {
    Alert alert = null;
    final JsonElement events = json.get("events");
    CollectorManager.logger.debug("events: " + events);
    if (events != null) {
      final JsonArray entries = events.getAsJsonArray();
      for (int i = 0; i < entries.size(); i++) {
        final JsonObject event = entries.get(i).getAsJsonObject().getAsJsonObject("event");
        CollectorManager.logger.trace("event: " + event);
        final Date start = getDate(event, "start", null);
        final Date end = getDate(event, "end", null);
        final String id = get(event, "id", null);
        final String message = get(event, "message", null);
        final String severity = get(event, "severity", "SEVERE");
        alert = build(id, start, end, message, severity);
        alert.state = AlertState.NEW;
        if (end != null) {
          alert.state = AlertState.CLOSED;
        }
        for (final String key : AlertHandler.METADATA) {
          final String val = get(event, key, null);
          if (val != null) {
            alert.tags.put(key, val);
          }

        }
        alerts.add(alert);
      }
    }
  }

  private Alert build(final String id, final Date start, final Date end, final String message, final String severity) {
    final Alert alert = new Alert();
    if (id != null) {
      alert.id = id;
    }
    alert.start = start;
    alert.end = end;
    alert.message = message;
    try {
      alert.severity = severity != null ? EventSeverity.valueOf(severity) : EventSeverity.SEVERE;
    }
    catch (final IllegalArgumentException e) {
      alert.severity = EventSeverity.WARN;
    }
    for (final Entry<String, String> x : config.def.entrySet()) {
      alert.tags.put(x.getKey(), x.getValue());
    }
    CollectorManager.logger.debug("base alert: " + alert);
    return alert;
  }

  private final static String get(final JsonElement event, final String name, final String def) {
    final JsonObject o = event.getAsJsonObject();
    final JsonElement kv = (o != null) ? o.get(name) : null;
    return (kv != null) ? kv.getAsString() : def;
  }

  private final static Date getDate(final JsonElement event, final String name, final Date def) {
    SimpleDateFormat ISO8601_1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    SimpleDateFormat ISO8601_2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXX");
    Date result = def;
    final JsonObject o = event.getAsJsonObject();
    final JsonElement kv = (o != null) ? o.get(name) : null;

    if (kv != null) {
      final String val = kv.getAsString();
      if (val != null) {
        try {
          result = ISO8601_1.parse(val);
        }
        catch (final ParseException e1) {
          try {
            result = ISO8601_2.parse(val);
          }
          catch (final ParseException e2) {
            try {
              long ts = Long.parseLong(val);
              if (ts < 1000000000000L) ts = ts * 1000; // Seconds to ms
              result = new Date(ts);
            }
            catch (final NumberFormatException e3) {
              System.out.println("parsing error: " + o);
            }
          }
        }
      }
    }
    return result;
  }

  private final static Date getDynatraceDate(final JsonElement event, final String name, final Date def) {
    Date result = null;
    final JsonObject o = event.getAsJsonObject();
    final JsonElement kv = (o != null) ? o.get(name) : null;
    if (kv != null) {
      final String val = kv.getAsString();
      if (val != null) {
        try {
          final long ts = Long.parseLong(val);
          if (ts > 0) {
            result = new Date(ts);
          }
        }
        catch (final NumberFormatException e) {
          CollectorManager.logger.debug("parsing error: " + o, e);
        }
      }
    }
    return result;
  }

  private void flush(final Alert a) {
    CollectorManager.logger.debug("flushing: " + a);
    if (!config.db_enabled) {
      exportIncidentDB(a);
    }
    if (config.log_enabled) {
      exporIncidentLog(a);
    }
    count++;
  }

  private void exporIncidentLog(final Alert a) {
    String fmt = null;
    switch (a.state) {
      case NEW:
        fmt = config.log_newFormat;
        break;
      case INPROGRESS:
        fmt = config.log_inprogressFormat;
        break;
      case CLOSED:
        fmt = config.log_closedFormat;
        break;
    }
    if (fmt != null) {
      final String msg = MessageFormat.format(fmt, //
          a.id, // 0
          a.state, // 1
          a.start, // 2
          a.end, // 3
          a.message, // 4
          a.tags.get(AlertHandler.METADATA[0]), // 5 "application"
          a.tags.get(AlertHandler.METADATA[1]), // 6 "module"
          a.tags.get(AlertHandler.METADATA[2]), // 7 "component"
          a.tags.get(AlertHandler.METADATA[3]) //  8 "host"
      );
      switch (a.severity) {
        case CRITICAL:
          AlertHandler.alertLogger.error(msg);
          break;
        case SEVERE:
        case ERROR:
          AlertHandler.alertLogger.error(msg);
          break;
        case WARN:
          AlertHandler.alertLogger.warn(msg);
          break;
        case INFO:
          AlertHandler.alertLogger.info(msg);
          break;
      }
    }
  }

  private void exportIncidentDB(final Alert a) {
    if ((config.db_tableName == null) || (config.db_tableFields == null)) { return; }

    final List<Object> vals = new ArrayList<>();
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
    vals.add(a.tags.get("host"));
    final String[] fields = config.db_tableFields;
    if (vals.size() != fields.length) {
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
            conn = config.dbConfig.getConnection();
            CollectorManager.logger.debug((conn != null) ? "Connection: OK" : "Connection error: " + config.dbConfig.getLastError());
          }
          if (conn != null) {
            CollectorManager.logger.debug(MessageFormat.format("Inserting {0}: {1} ", config.db_tableName, sb.toString()));
            LibDB.insertRecord(conn, config.db_tableName, fields, vals.toArray(), config.db_maxSize);
          }
          else {
            CollectorManager.logger.debug("No connection for inserting");
          }
        }
      }
      catch (final SQLIntegrityConstraintViolationException e) {
        CollectorManager.logger.warn("SQLIntegrityConstraintViolationException");
        CollectorManager.logger.debug("SQLException", e);
      }
      catch (final SQLException e) {
        CollectorManager.logger.error("SQLException", e);
        Helper.close(conn);
        conn = null;
      }
    }
  }

  @Override
  public void init(final Properties conf) throws Exception {
    config.setup(conf);
  }

}
