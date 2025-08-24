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
package net.eiroca.sysadm.tools.sysadmserver.collector;

public class ActionDef {

  public enum Method {
    GET, POST
  }

  public enum Mode {
    JSON, OBJECT
  }

  private String className;
  private Method method;
  private Mode mode;

  public ActionDef(final String className, final Method method, final Mode mode) {
    super();
    this.className = className;
    this.method = method;
    this.mode = mode;
  }

  public String getClassName() {
    return className;
  }

  public void setClassName(final String className) {
    this.className = className;
  }

  public Method getMethod() {
    return method;
  }

  public void setMethod(final Method method) {
    this.method = method;
  }

  public Mode getMode() {
    return mode;
  }

  public void setMode(final Mode mode) {
    this.mode = mode;
  }

}
