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
package net.eiroca.sysadm.tools.sysadmserver.handler;

import java.io.IOException;
import java.io.StringWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import io.javalin.http.Context;
import net.eiroca.library.core.LibStr;
import net.eiroca.sysadm.tools.sysadmserver.collector.GenericHandler;
import net.eiroca.sysadm.tools.sysadmserver.event.Alert;
import net.eiroca.sysadm.tools.sysadmserver.event.AlertState;
import net.eiroca.sysadm.tools.sysadmserver.event.EventSeverity;
import net.eiroca.sysadm.tools.sysadmserver.exporter.alert.DBAlertExporter;
import net.eiroca.sysadm.tools.sysadmserver.exporter.alert.GenericAlertExporter;
import net.eiroca.sysadm.tools.sysadmserver.exporter.alert.HookAlertExporter;
import net.eiroca.sysadm.tools.sysadmserver.exporter.alert.LoggerAlertExporter;
import net.eiroca.sysadm.tools.sysadmserver.manager.CollectorManager;

public class AlertHandler extends GenericHandler {

  private AlertHandlerContext context;
  private final ObjectMapper mapper = new ObjectMapper();

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

  public synchronized int processAlertsFormMessage(final String namespace, final Context ctx, String msg) {
    int cnt = 0;
    if (LibStr.isNotEmptyOrNull(msg)) {
      cnt = 1;
      Alert a = new Alert();
      a.message = msg;
      a.severity = EventSeverity.ERROR;
      flush(a);
    }
    return cnt;
  }

  public synchronized int processAlertsFormJson(final String namespace, final Context ctx, String data) {
    final List<Alert> alerts = new ArrayList<>();
    int cnt = 0;
    CollectorManager.logger.debug("alert json: " + data);
    if (LibStr.isEmptyOrNull(data)) { return cnt; }
    // Input transformation via Freemaker Template
    final String templateName = namespace + "_format.ftl";
    Template srcTemplate = null;
    srcTemplate = context.getTemplate(templateName);
    if (srcTemplate != null) {
      try {
        @SuppressWarnings("unchecked")
        final Map<String, Object> model = mapper.readValue(data, Map.class);
        final StringWriter out = new StringWriter();
        srcTemplate.process(model, out);
        data = out.getBuffer().toString();
      }
      catch (IOException | TemplateException e) {
        AlertHandlerContext.logInvalidJSon(data, e);
        return 0;
      }
    }
    readAlerts(alerts, data);
    for (Alert alert : alerts) {
      if (alert == null) {
        continue;
      }
      for (final String key : AlertHandler.QUERYDATA) {
        final String val = ctx.queryParam(key);
        if (val != null) {
          alert.tags.put(key, val);
        }
      }
      if (context.config_validationLevel >= 0) {
        for (int i = 0; i < (context.config_validationLevel + 1); i++) {
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

  public void readAlerts(final List<Alert> alerts, final String data) {
    final JsonObject json = context.getJson(data);
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
    for (final Entry<String, String> x : context.def.entrySet()) {
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
    for (final GenericAlertExporter exporter : exporters) {
      if (exporter.beginBulk()) {
        exporter.process(a);
        exporter.endBulk();
      }
    }
  }

  protected List<GenericAlertExporter> exporters = new ArrayList<>();

  @Override
  public void init(final Properties conf) throws Exception {
    context = new AlertHandlerContext(conf);
    context.setup(conf);
    exporters.clear();
    exporters.add(new LoggerAlertExporter("alert.export.LOG."));
    exporters.add(new DBAlertExporter("alert.export.DB."));
    exporters.add(new HookAlertExporter("alert.export.HOOK."));
    for (final GenericAlertExporter exporter : exporters) {
      exporter.setup(context);
    }
  }

}
