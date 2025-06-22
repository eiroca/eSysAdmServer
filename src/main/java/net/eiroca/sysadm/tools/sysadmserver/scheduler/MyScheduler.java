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
package net.eiroca.sysadm.tools.sysadmserver.scheduler;

import java.text.SimpleDateFormat;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import net.eiroca.library.scheduler.Scheduler;
import net.eiroca.library.scheduler.Task;
import net.eiroca.library.system.Logs;
import net.eiroca.sysadm.tools.sysadmserver.SystemConfig;

final public class MyScheduler extends Scheduler {

  private static final String SCHEDULERNAME = SystemConfig.ME + ".scheduler";

  public static final Logger logger = Logs.getLogger(MyScheduler.SCHEDULERNAME);

  long runningTask = 0;
  long executedTask = 0;

  public MyScheduler(final int workers) {
    super(Executors.newFixedThreadPool(workers), 1, 60, TimeUnit.SECONDS, MyScheduler.SCHEDULERNAME);
  }

  @Override
  public void onTaskStart(final Task task) {
    super.onTaskStart(task);
    MyScheduler.logger.debug(task.getName() + " started");
    runningTask++;
  }

  @Override
  public void onTaskEnd(final Task task) {
    super.onTaskEnd(task);
    MyScheduler.logger.info(task.getName() + " completed");
    executedTask++;
    runningTask--;
  }

  private final static SimpleDateFormat SDF = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

  public void logStat() {
    try {
      MyScheduler.logger.info("Running Task = " + runningTask);
      MyScheduler.logger.debug("Scheduler state = " + schedulerThread.getShedulerState());
      MyScheduler.logger.debug("Scheduler nextRun = " + MyScheduler.SDF.format(new Date(schedulerThread.getNextRun())));
      MyScheduler.logger.debug("Executed Task = " + executedTask);
      for (final Task t : schedulerThread.getTaskList()) {
        MyScheduler.logger.debug(t.getId() + " " + t.getName() + " " + t.getState() + " " + MyScheduler.SDF.format(new Date(t.nextRun())));
      }
    }
    catch (final ConcurrentModificationException e) {
    }
  }

}
