/**
 *
 * Copyright (C) 2001-2019 eIrOcA (eNrIcO Croce & sImOnA Burzio) - AGPL >= 3.0
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
package net.eiroca.sysadm.tools.sysadmserver.scheduler;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import net.eiroca.library.scheduler.Scheduler;
import net.eiroca.library.scheduler.Task;
import net.eiroca.sysadm.tools.sysadmserver.SystemContext;

final public class MyScheduler extends Scheduler {

  public MyScheduler(final int workers) {
    super(Executors.newFixedThreadPool(workers), 1, TimeUnit.SECONDS, SystemContext.ME + ".scheduler");
  }

  @Override
  public void onTaskStart(final Task task) {
    SystemContext.logger.info("START of " + task.getId());
  }

  @Override
  public void onTaskEnd(final Task task) {
    SystemContext.logger.info("END of " + task.getId());
  }

}
