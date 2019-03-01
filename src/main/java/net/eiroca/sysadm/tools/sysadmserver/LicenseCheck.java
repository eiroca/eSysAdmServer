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
package net.eiroca.sysadm.tools.sysadmserver;

import net.eiroca.library.license.api.License;
import net.eiroca.library.license.api.LicenseManager;
import net.eiroca.library.server.ServerResponse;

public class LicenseCheck {

  public static final String ME = "eSysAdmServer";
  public static License license;
  public static ServerResponse LICENCE_ERROR = new ServerResponse(-9999, "License is expired, no action is taken");

  public static void init() {
    LicenseCheck.license = LicenseManager.getInstance().getLicense(LicenseCheck.ME, true);
  }

  public static boolean isValid() {
    return LicenseManager.isValidLicense(LicenseCheck.license);
  }

}
