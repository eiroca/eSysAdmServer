/**
 *
 * Copyright (C) 1999-2020 Enrico Croce - AGPL >= 3.0
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
package net.eiroca.sysadm.tools.sysadmserver.manager;

import net.eiroca.sysadm.tools.sysadmserver.SystemContext;

public class GenericManager implements ISysAdmManager {

  private boolean started = false;

  @Override
  public void start() throws Exception {
    if (started) { throw new IllegalStateException(); }
    SystemContext.logger.info("Starting " + getClass().getCanonicalName());
    started = true;
  }

  @Override
  public void stop() throws Exception {
    if (!started) { throw new IllegalStateException(); }
    started = false;
  }

  @Override
  public boolean isStarted() {
    return started;
  }

}
