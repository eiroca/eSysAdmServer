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
package net.eiroca.sysadm.tools.sysadmserver.exporter.alert;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import net.eiroca.library.config.parameter.BooleanParameter;
import net.eiroca.library.config.parameter.StringParameter;
import net.eiroca.sysadm.tools.sysadmserver.event.Alert;
import net.eiroca.sysadm.tools.sysadmserver.exporter.GenericExporter;
import net.eiroca.sysadm.tools.sysadmserver.handler.AlertHandlerContext;
import net.eiroca.sysadm.tools.sysadmserver.manager.CollectorManager;

public abstract class GenericAlertExporter extends GenericExporter<Alert, AlertHandlerContext> {

  protected static transient BooleanParameter _hookEnabled = new BooleanParameter(GenericExporter.config, "enabled", true);
  protected static transient StringParameter _hookTemplate = new StringParameter(GenericExporter.config, "template", null);
  protected static transient BooleanParameter _prettyJson = new BooleanParameter(config, "prettyJson", false);

  public Boolean config_enabled;
  public String config_template;
  public Boolean config_prettyJson;

  private final ObjectMapper mapper = new ObjectMapper();

  public GenericAlertExporter(final String prefix) {
    super(prefix);
  }

  @Override
  public boolean beginBulk() {
    return config_enabled;
  }

  protected String getTextFromAlert(final Alert a) {
    return getTextFromAlert(a, config_template, config_prettyJson);
  }

  private String getTextFromAlert(final Alert a, final String templateName, final boolean prettyPrint) {
    String msg = null;
    if (templateName != null) {
      CollectorManager.logger.debug("Applying transformation: " + templateName);
      final Template t = context.getTemplate(templateName);
      if (t != null) {
        try {
          @SuppressWarnings("unchecked")
          final Map<String, Object> model = mapper.readValue(a.toString(), Map.class);
          final StringWriter dstOut = new StringWriter();
          t.process(model, dstOut);
          msg = dstOut.getBuffer().toString();
          if (prettyPrint) {
            final JsonObject json = context.getJson(msg);
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

}
