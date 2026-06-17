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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import net.eiroca.library.core.Registry;
import net.eiroca.library.system.Logs;
import net.eiroca.sysadm.tools.sysadmserver.exporter.trace.ElasticTraceExporter;
import net.eiroca.sysadm.tools.sysadmserver.exporter.trace.ITraceExporter;
import net.eiroca.sysadm.tools.sysadmserver.exporter.trace.LoggerTraceExporter;

public class AlertExporters {

  public static final Registry<String> registry = new Registry<>();

  public static final List<String> defaultExporters = new ArrayList<>();

  static {
    AlertExporters.registry.addEntry(LoggerTraceExporter.ID, LoggerTraceExporter.class.getName());
    AlertExporters.registry.addEntry(ElasticTraceExporter.ID, ElasticTraceExporter.class.getName());
  }

  static {
    AlertExporters.defaultExporters.add(LoggerTraceExporter.ID);
  }

  public static ITraceExporter newInstance(final String name) {
    ITraceExporter obj = null;
    try {
      String clazzName = AlertExporters.registry.value(name);
      if (clazzName != null) {
        Class<?> clazz = Class.forName(clazzName);
        Constructor<?> constructor = clazz.getConstructor();
        obj = (ITraceExporter)constructor.newInstance();
      }
    }
    catch (InvocationTargetException | ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException e) {
      Logs.ignore(e);
    }
    return obj;
  }

}
