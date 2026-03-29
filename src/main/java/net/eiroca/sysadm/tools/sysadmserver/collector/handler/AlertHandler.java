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

import java.io.IOException;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import org.slf4j.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import net.eiroca.library.core.Helper;
import net.eiroca.library.core.LibStr;
import net.eiroca.library.db.LibDB;
import net.eiroca.library.system.LibFile;
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

  private final AlertConfig config = new AlertConfig();
  private final ObjectMapper mapper = new ObjectMapper();

  private long count = 0;
  private Connection conn = null;

  private static final String[] QUERYDATA = {
      "application", "module", "component", "host"
  };
  private static final Set<String> SKIP_DATA = new HashSet<>();
  static {
    AlertHandler.SKIP_DATA.add("id");
    AlertHandler.SKIP_DATA.add("start");
    AlertHandler.SKIP_DATA.add("end");
    AlertHandler.SKIP_DATA.add("state");
    AlertHandler.SKIP_DATA.add("message");
  }

  private static void logInvalidJSon(final String data, final Exception e) {
    CollectorManager.logger.warn("Invalid alert json: " + e.getMessage());
    CollectorManager.logger.info("Invalid alert json: " + data);
  }

  public synchronized int processAlertsFormJson(final String namespace, final Request request, String data) {
    final List<Alert> alerts = new ArrayList<>();
    int cnt = 0;
    CollectorManager.logger.debug("alert json: " + data);
    if (LibStr.isEmptyOrNull(data)) { return cnt; }
    // Input transformation via Freemaker Template
    final String templateName = namespace + "_format.ftl";
    Template srcTemplate = null;
    srcTemplate = getTemplate(templateName);
    if (srcTemplate != null) {
      try {
        @SuppressWarnings("unchecked")
        final Map<String, Object> model = mapper.readValue(data, Map.class);
        final StringWriter out = new StringWriter();
        srcTemplate.process(model, out);
        data = out.getBuffer().toString();
      }
      catch (IOException | TemplateException e) {
        AlertHandler.logInvalidJSon(data, e);
        return 0;
      }
    }
    readAlerts(alerts, data);
    for (Alert alert : alerts) {
      if (alert == null) {
        continue;
      }
      for (final String key : AlertHandler.QUERYDATA) {
        final String val = request.queryParams(key);
        if (val != null) {
          alert.tags.put(key, val);
        }
      }
      if (config.validationLevel >= 0) {
        for (int i = 0; i < (config.validationLevel + 1); i++) {
          if (alert.tags.get(AlertHandler.QUERYDATA[i]) == null) {
            alert = null;
            break;
          }
        }
      }
      CollectorManager.logger.trace("alert: {0}", alert);
      try {
        if (alert != null) {
          cnt++;
          flush(alert);
        }
      }
      catch (final Exception e) {
        CollectorManager.logger.error("Flushing Failed: " + e.getMessage(), e);
      }
    }
    return cnt;
  }

  private final Map<String, Template> templates = new HashMap<>();
  Configuration cfg = new Configuration(Configuration.VERSION_2_3_34);

  private Template getTemplate(final String templateName) {
    Template t = null;
    synchronized (templates) {
      if (templates.containsKey(templateName)) { return templates.get(templateName); }
      final String template = LibFile.readString(config.templatesPath + Helper.FS + templateName);
      if (LibStr.isNotEmptyOrNull(template)) {
        try {
          t = new Template(templateName, template, cfg);
        }
        catch (final IOException e) {
        }
      }
      templates.put(templateName, t);
    }
    return t;
  }

  private JsonObject getJson(final String data) {
    JsonObject json = null;
    try {
      json = JsonParser.parseString(data).getAsJsonObject();
    }
    catch (final Exception e) {
      AlertHandler.logInvalidJSon(data, e);
    }
    return json;
  }

  public void readAlerts(final List<Alert> alerts, final String data) {
    final JsonObject json = getJson(data);
    if (json == null) { return; }
    final JsonElement events = json.get("events");
    CollectorManager.logger.debug("events: " + events);
    if (events != null) {
      Alert alert = null;
      final JsonArray entries = events.getAsJsonArray();
      for (int i = 0; i < entries.size(); i++) {
        final JsonObject event = entries.get(i).getAsJsonObject().getAsJsonObject("event");
        CollectorManager.logger.trace("event: " + event);
        final Date start = AlertHandler.getDate(event, "start", null);
        final Date end = AlertHandler.getDate(event, "end", null);
        final String id = AlertHandler.get(event, "id", null);
        final String message = AlertHandler.get(event, "message", null);
        final String severity = AlertHandler.get(event, "severity", "SEVERE");
        alert = build(id, start, end, message, severity);
        alert.state = AlertState.NEW;
        if (end != null) {
          alert.state = AlertState.CLOSED;
        }
        for (final Map.Entry<String, JsonElement> entry : event.entrySet()) {
          final String key = entry.getKey();
          if (!AlertHandler.SKIP_DATA.contains(key)) {
            final JsonElement value = entry.getValue();
            alert.tags.put(key, value.toString());
          }
        }
        for (final String key : AlertHandler.QUERYDATA) {
          final String val = AlertHandler.get(event, key, null);
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
    final SimpleDateFormat ISO8601_1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    final SimpleDateFormat ISO8601_2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXX");
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
              if (ts < 1000000000000L) {
                ts = ts * 1000; // Seconds to ms
              }
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

  private void flush(final Alert a) {
    CollectorManager.logger.debug("flushing: " + a);
    if (config.db_enabled) {
      exportIncidentDB(a);
    }
    if (config.log_enabled) {
      exporIncidentLog(a);
    }
    count = (count + 1) & 0x7FFF_FFFF_FFFF_FFFFL;
  }

  private String getTextFromAlert(final Alert a) {
    String msg = null;
    final String templateName = config.template;
    if (templateName != null) {
      CollectorManager.logger.debug("Applying transformation: " + templateName);
      final Template t = getTemplate(templateName);
      if (t != null) {
        try {
          @SuppressWarnings("unchecked")
          final Map<String, Object> model = mapper.readValue(a.toString(), Map.class);
          final StringWriter dstOut = new StringWriter();
          t.process(model, dstOut);
          msg = dstOut.getBuffer().toString();
          if (config.prettyJson) {
            final JsonObject json = getJson(msg);
            if (json == null) { return null; }
            msg = json.toString();
          }
          CollectorManager.logger.debug("Transformed alert: " + msg);
        }
        catch (IOException | TemplateException e) {
          CollectorManager.logger.info("Invalid alert output transformation: " + e.getMessage());
          CollectorManager.logger.debug("Invalid alert output transformation: " + a);
        }
      }
    }
    if (msg == null) {
      msg = a.toString();
    }
    CollectorManager.logger.trace("getTextFromAlert: " + msg);
    return msg;
  }

  private void exporIncidentLog(final Alert a) {
    final String msg = getTextFromAlert(a);
    if (msg != null) {
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
