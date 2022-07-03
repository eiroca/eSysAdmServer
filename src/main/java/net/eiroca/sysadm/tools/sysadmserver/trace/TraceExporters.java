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
package net.eiroca.sysadm.tools.sysadmserver.trace;

import java.util.ArrayList;
import java.util.List;
import net.eiroca.library.core.Registry;
import net.eiroca.library.system.Logs;
import net.eiroca.sysadm.tools.sysadmserver.trace.exporter.ElasticTraceExporter;
import net.eiroca.sysadm.tools.sysadmserver.trace.exporter.LoggerTraceExporter;

public class TraceExporters {

  public static final Registry<String> registry = new Registry<>();

  public static final List<String> defaultExporters = new ArrayList<>();

  static {
    TraceExporters.defaultExporters.add(LoggerTraceExporter.ID);
  }

  static {
    TraceExporters.registry.addEntry(ElasticTraceExporter.ID, ElasticTraceExporter.class.getName());
    TraceExporters.registry.addEntry(LoggerTraceExporter.ID, LoggerTraceExporter.class.getName());
  }

  public static ITraceExporter newInstance(final String name) {
    ITraceExporter obj = null;
    try {
      obj = (ITraceExporter)Class.forName(TraceExporters.registry.value(name)).newInstance();
    }
    catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
      Logs.ignore(e);
    }
    return obj;
  }

}
