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
import com.google.gson.JsonObject;
import net.eiroca.ext.library.gson.LibGson;
import net.eiroca.library.server.ResultResponse;
import net.eiroca.sysadm.tools.sysadmserver.SystemContext;
import net.eiroca.sysadm.tools.sysadmserver.collector.GenericRuleBasedHandler;
import net.eiroca.sysadm.tools.sysadmserver.collector.GenericTask;
import net.eiroca.sysadm.tools.sysadmserver.collector.Tasks;

public class TaskHandler extends GenericRuleBasedHandler<GenericTask> {

  private static final String PROP_TYPE = "type";
  public static final String PARAM_ID = ":id";

  @Override
  public void init(final Properties config) throws Exception {
    loadRules(GenericRuleBasedHandler.RULE_FILEEXT, SystemContext.config.task_rules_path);
  }

  @Override
  protected GenericTask createRule(final String name, final Properties config) {
    final String type = config.getProperty(TaskHandler.PROP_TYPE);
    if (type == null) { return null; }
    final String className = Tasks.registry.get(type);
    if (className == null) { return null; }
    try {
      final GenericTask task = (GenericTask)Class.forName(className).newInstance();
      task.init(config);
      return task;
    }
    catch (final Exception e) {
      GenericRuleBasedHandler.logger.error("Unable to create task " + name + " of type:" + type + " Exception: " + e.getMessage(), e);
    }
    return null;
  }

  public void run(final String namespace, final String id, final String body, final ResultResponse<Object> result) {
    final GenericTask task = getRule(id);
    if (task == null) {
      result.setStatus(1);
      result.setMessage("Invalid task id");
      return;
    }
    final JsonObject request = (body != null) ? LibGson.fromString(body) : null;
    JsonObject response = null;
    response = task.run(request);
    if (response != null) {
      result.setResult(response);
    }
    else {
      result.setStatus(1);
      result.setMessage("Unable to run the task");
    }
  }

}
