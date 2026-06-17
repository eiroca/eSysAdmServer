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
package net.eiroca.sysadm.tools.sysadmserver.exporter.trace;

import net.eiroca.library.system.IContext;
import net.eiroca.sysadm.tools.sysadmserver.exporter.GenericExporter;

public abstract class GenericTraceExporter extends GenericExporter<String, IContext> {

  public GenericTraceExporter(String param_prefix) {
    super(param_prefix);
  }

  @Override
  public void process(final String trace) {
  }

}
