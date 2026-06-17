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
package net.eiroca.sysadm.tools.sysadmserver.exporter;

import org.slf4j.Logger;
import net.eiroca.library.system.ContextParameters;
import net.eiroca.library.system.IContext;
import net.eiroca.library.system.Logs;

public abstract class GenericExporter<T, C extends IContext> implements IExporter<T, C> {

  protected static Logger logger = Logs.getLogger();
  protected static ContextParameters config = new ContextParameters();

  protected static String CONFIG_PREFIX = null;

  protected C context = null;

  protected String param_prefix = null;
  protected String var_prefix = "config_";

  public GenericExporter(String param_prefix) {
    super();
    this.param_prefix = param_prefix;
  }

  @Override
  public void setup(final C context) throws Exception {
    context.info(getId(), " setup");
    this.context = context;
    config.loadConfig(context, param_prefix);
    config.saveConfig(this, var_prefix, true, true);
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

}
