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
package net.eiroca.sysadm.tools;

import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import net.eiroca.library.core.Helper;
import net.eiroca.library.dynatrace.exporter.DynatraceExporter;
import net.eiroca.library.system.LibFile;
import net.eiroca.sysadm.tools.sysadmserver.SystemContext;
import net.eiroca.sysadm.tools.sysadmserver.manager.ISysAdmManager;

public class eSysAdmServer {

  private static final int SLEEPTIME = 5 * 1000;

  public static void main(final String[] args) {
    DynatraceExporter.init();
    final String confPath = eSysAdmServer.getConfigPath(args);
    try {
      eSysAdmServer.listClassPath();
      SystemContext.init(confPath);
      for (final ISysAdmManager manager : SystemContext.managers) {
        manager.start();
      }
      while (true) {
        SystemContext.scheduler.logStat();
        Helper.sleep(eSysAdmServer.SLEEPTIME);
        if (!Files.exists(SystemContext.config.lockfile) || !SystemContext.isLicenseValid()) {
          break;
        }
      }
    }
    catch (final Exception e) {
      SystemContext.logger.error("Fatal error: " + e.getMessage(), e);
    }
    finally {
      for (final ISysAdmManager manager : SystemContext.managers) {
        if (manager.isStarted()) {
          try {
            manager.stop();
          }
          catch (final Exception e) {
            SystemContext.logger.warn(Helper.getExceptionAsString(e));
          }
        }
      }
      SystemContext.done();
    }
  }

  private static void listClassPath() {
    final List<URI> filesList = new ArrayList<>();
    LibFile.getClassPathFiles(filesList);
    for (final URI file : filesList) {
      SystemContext.logger.debug(file.toString());
    }
  }

  private static String getConfigPath(final String[] args) {
    String confPath;
    final String sep = FileSystems.getDefault().getSeparator();
    if (args.length > 0) {
      confPath = args[0];
      if (!confPath.endsWith(sep)) {
        confPath = confPath + sep;
      }
    }
    else {
      confPath = "";
    }
    return confPath;
  }
}
