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
package net.eiroca.sysadm.tools;

import java.net.URI;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import net.eiroca.library.core.Helper;
import net.eiroca.library.system.LibFile;
import net.eiroca.sysadm.tools.sysadmserver.CollectorManager;
import net.eiroca.sysadm.tools.sysadmserver.MonitorManager;
import net.eiroca.sysadm.tools.sysadmserver.SystemContext;

public class eSysAdmServer {

  private static final int SLEEPTIME = 15 * 1000;

  public static void main(final String[] args) {
    final String confPath = eSysAdmServer.getConfigPath(args);
    try {
      eSysAdmServer.listClassPath();
      SystemContext.init(confPath);
      MonitorManager.start();
      CollectorManager.start();
      while (true) {
        SystemContext.scheduler.logStat();
        Helper.sleep(eSysAdmServer.SLEEPTIME);
        if (!Files.exists(SystemContext.lockFile)) {
          break;
        }
        if (!SystemContext.isLicenseValid()) {
          break;
        }
      }
    }
    catch (final Exception e) {
      SystemContext.logger.error("Fatal error: " + e.getMessage(), e);
    }
    finally {
      CollectorManager.stop();
      MonitorManager.stop();
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
    if (args.length > 0) {
      confPath = args[0];
      if (!confPath.endsWith("\\")) {
        confPath = confPath + "\\";
      }
    }
    else {
      confPath = "";
    }
    return confPath;
  }
}
