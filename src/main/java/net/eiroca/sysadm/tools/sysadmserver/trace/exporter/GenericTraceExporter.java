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
package net.eiroca.sysadm.tools.sysadmserver.trace.exporter;

import org.slf4j.Logger;
import net.eiroca.library.system.ContextParameters;
import net.eiroca.library.system.IContext;
import net.eiroca.library.system.Logs;
import net.eiroca.sysadm.tools.sysadmserver.trace.ITraceExporter;

public abstract class GenericTraceExporter implements ITraceExporter {

  protected static Logger logger = Logs.getLogger();
  protected static ContextParameters config = new ContextParameters();

  protected static String CONFIG_PREFIX = null;

  protected IContext context = null;

  public GenericTraceExporter() {
    super();
  }

  @Override
  public void setup(final IContext context) throws Exception {
    context.info(getId(), " setup");
    this.context = context;
  }

  @Override
  public void teardown() throws Exception {
    context.info(getId(), " teardown");
  }

  @Override
  public boolean beginBulk() {
    return true;
  }

  @Override
  public void endBulk() {
  }

  @Override
  public void process(final String trace) {
  }

}
