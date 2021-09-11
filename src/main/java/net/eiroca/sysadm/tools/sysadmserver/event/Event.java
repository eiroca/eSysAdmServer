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
package net.eiroca.sysadm.tools.sysadmserver.event;

import java.util.Date;
import java.util.UUID;
import net.eiroca.ext.library.gson.GsonUtil;
import net.eiroca.library.data.Tags;

public class Event implements Comparable<Event> {

  public String id;
  public Date start;
  public EventSeverity severity;
  public Tags tag = new Tags();

  public Event(final String id) {
    this.id = id;
    start = new Date();
  }

  public Event() {
    newId();
    start = new Date();
  }

  @Override
  public String toString() {
    return GsonUtil.toJSON(this);
  }

  @Override
  public int compareTo(final Event o) {
    if (severity.ordinal() > o.severity.ordinal()) {
      return -1;
    }
    else if (severity.ordinal() < o.severity.ordinal()) {
      return 1;
    }
    else {
      return (start != null) ? -start.compareTo(o.start) : -1;
    }
  }

  public void newId() {
    id = "$" + UUID.randomUUID().toString();
  }

}
