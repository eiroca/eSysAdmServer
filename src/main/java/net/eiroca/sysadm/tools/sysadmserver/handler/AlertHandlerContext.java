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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import freemarker.template.Configuration;
import freemarker.template.Template;
import net.eiroca.ext.library.gson.GsonUtil;
import net.eiroca.library.config.parameter.IntegerParameter;
import net.eiroca.library.config.parameter.LocalPathParameter;
import net.eiroca.library.core.Helper;
import net.eiroca.library.core.LibStr;
import net.eiroca.library.csv.CSVData;
import net.eiroca.library.sysadm.monitoring.sdk.ServerContext;
import net.eiroca.library.system.ContextParameters;
import net.eiroca.library.system.LibFile;
import net.eiroca.sysadm.tools.sysadmserver.SystemConfig;
import net.eiroca.sysadm.tools.sysadmserver.SystemContext;
import net.eiroca.sysadm.tools.sysadmserver.manager.CollectorManager;

public class AlertHandlerContext extends ServerContext {

  private static final String CONFIG_PREFIX_PARAM = "alert.";
  private static final String CONFIG_PREFIX_VAR = "config_";

  protected static transient ContextParameters config = new ContextParameters();

  protected static transient IntegerParameter _validationLevel = new IntegerParameter(AlertHandlerContext.config, "validationLevel", -1);
  protected static transient LocalPathParameter _defaultTagPath = new LocalPathParameter(AlertHandlerContext.config, "defaultTagPath", null, SystemConfig.pathGetter);
  protected static transient LocalPathParameter _templatesDir = new LocalPathParameter(AlertHandlerContext.config, "templatesDir", "&templates", SystemConfig.pathGetter);

  public Integer config_validationLevel;
  public Path config_defaultTagPath;
  public Path config_templatesDir;

  public transient Map<String, String> def = new HashMap<>();
  public transient Map<String, String> mapping = new HashMap<>();

  private transient final Map<String, Template> templates = new HashMap<>();
  private transient final Configuration cfg = new Configuration(Configuration.VERSION_2_3_34);

  public AlertHandlerContext(final Properties properties) {
    super("AlertHandler", properties);

  }

  public void setup(final Properties params) throws Exception {
    SystemContext.logger.trace("Params: " + params);

    AlertHandlerContext.config.loadConfig(params, AlertHandlerContext.CONFIG_PREFIX_PARAM);
    AlertHandlerContext.config.saveConfig(this, AlertHandlerContext.CONFIG_PREFIX_VAR, true, true);

    def.clear();
    if ((config_defaultTagPath != null) && (Files.exists(config_defaultTagPath))) {
      loadDefault(config_defaultTagPath.toString());
    }
    mapping.clear();
    SystemContext.logger.info("AlertHandlerContext.config: " + this);
  }

  private void loadDefault(final String path) {
    final CSVData csv = new CSVData(path);
    if (csv.size() > 0) {
      for (int i = 0; i < csv.size(); i++) {
        final String[] data = csv.getData(i);
        if ((data == null) || (data.length != 2)) {
          continue;
        }
        final String key = data[0];
        final String val = data[1];
        def.put(key, val);
      }
    }
  }

  public JsonObject getJson(final String data) {
    JsonObject json = null;
    try {
      json = JsonParser.parseString(data).getAsJsonObject();
    }
    catch (final Exception e) {
      AlertHandlerContext.logInvalidJSon(data, e);
    }
    return json;
  }

  public static void logInvalidJSon(final String data, final Exception e) {
    CollectorManager.logger.warn("Invalid alert json: " + e.getMessage());
    CollectorManager.logger.info("Invalid alert json: " + data);
  }

  public Template getTemplate(final String templateName) {
    Template t = null;
    synchronized (templates) {
      if (templates.containsKey(templateName)) { return templates.get(templateName); }
      final String templatePath = config_templatesDir + Helper.FS + templateName;
      final String template = LibFile.readString(templatePath);
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

  @Override
  public String toString() {
    return GsonUtil.toJSON(this);
  }

}
