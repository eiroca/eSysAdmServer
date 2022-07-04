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
import java.util.Map;
import net.eiroca.library.config.parameter.StringParameter;
import net.eiroca.library.core.Helper;
import net.eiroca.library.sysadm.monitoring.api.IContextEnabled;
import net.eiroca.library.system.ContextParameters;
import net.eiroca.library.system.IContext;

public class TraceConsumer implements IContextEnabled, Runnable {

  private static final String CONFIG_PREFIX = null;

  public static final String STATUS_OK = "OK";

  public static ContextParameters config = new ContextParameters();
  public static StringParameter _timezone = new StringParameter(TraceConsumer.config, "timezone", null);
  // Dynamic mapped to parameters
  protected String config_timezone;
  //
  private final Object dataLock = new Object();
  private List<String> buffer = new ArrayList<>();
  //
  protected IContext context = null;
  protected Map<String, String> alias;

  private static List<ITraceExporter> exporters = new ArrayList<>();
  static {
    for (final String name : TraceExporters.registry.getNames()) {
      TraceConsumer.exporters.add(TraceExporters.newInstance(name));
    }
  }

  public TraceConsumer(final Map<String, String> alias) {
    this.alias = alias;
  }

  @Override
  public void setup(final IContext context) throws Exception {
    this.context = context;
    TraceConsumer.config.convert(context, TraceConsumer.CONFIG_PREFIX, this, "config_");
    for (final ITraceExporter connector : TraceConsumer.exporters) {
      connector.setup(context);
    }
    context.info(this.getClass().getName(), " setup done");
  }

  @Override
  public void teardown() throws Exception {
    context.info(this.getClass().getName(), " teardown");
    for (final ITraceExporter connector : TraceConsumer.exporters) {
      connector.teardown();
    }
  }

  public void addTrace(final String trace) {
    synchronized (dataLock) {
      buffer.add(trace);
    }
  }

  public List<String> swap() {
    List<String> result = null;
    synchronized (dataLock) {
      if (buffer.size() > 0) {
        result = buffer;
        buffer = new ArrayList<>();
      }
    }
    return result;
  }

  @Override
  public void run() {
    if (context != null) {
      context.debug("run export");
    }
    final List<String> events = swap();
    if ((events != null) && (events.size() > 0)) {
      try {
        flush(events);
      }
      catch (final Exception e) {
        context.error("Error exporting measures", e);
        context.info(e.getMessage(), " ", Helper.getStackTraceAsString(e));
      }
    }
  }

  private void flush(final List<String> traces) throws Exception {
    context.debug("flush events");
    final List<ITraceExporter> validConnectors = new ArrayList<>();
    for (final ITraceExporter connector : TraceConsumer.exporters) {
      if (connector.beginBulk()) {
        validConnectors.add(connector);
      }
    }
    for (final String trace : traces) {
      for (final ITraceExporter connector : validConnectors) {
        connector.process(trace);
      }
    }
    for (final ITraceExporter connector : validConnectors) {
      connector.endBulk();
    }
    context.info("Exported traces(s): " + traces.size());
  }

  public boolean exportTrace(final String trace) {
    context.debug("exportTrace ", trace);
    addTrace(trace);
    return true;
  }

}
