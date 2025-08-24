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
package net.eiroca.sysadm.tools.sysadmserver.collector.task;

import java.util.Properties;
import com.google.gson.JsonObject;
import net.eiroca.sysadm.tools.sysadmserver.collector.GenericTask;

public class HttpTask extends GenericTask {

  public HttpTask() {
  }

  @Override
  public JsonObject run(final JsonObject request) {
    return request;
  }

  @Override
  public void init(final Properties config) throws Exception {
  }

}
