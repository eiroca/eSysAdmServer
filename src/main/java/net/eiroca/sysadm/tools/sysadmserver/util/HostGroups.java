/**
 *
 * Copyright (C) 1999-2019 Enrico Croce - AGPL >= 3.0
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
package net.eiroca.sysadm.tools.sysadmserver.util;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.eiroca.library.core.LibStr;
import net.eiroca.library.data.ITagsProvider;
import net.eiroca.library.data.Tags;
import net.eiroca.library.system.LibFile;

public class HostGroups implements ITagsProvider {

  private static final String REGEX_GROUP_TAGS = "tags";
  private static final String REGEX_GROUP_HOST = "host";

  Pattern regExHostGroup = Pattern.compile("\\s*(?<" + HostGroups.REGEX_GROUP_HOST + ">[^\\s]+)\\s*=\\s*(?<" + HostGroups.REGEX_GROUP_TAGS + ">.+)\\s*");
  Pattern regExGroups = Pattern.compile("([^\\s]+)");

  final private Map<String, Set<String>> hostGroups = new HashMap<>();
  final private Map<String, Tags> hostTags = new HashMap<>();

  public HostGroups(final Path defPath, final String tagPrefix) {
    if (defPath != null) {
      final List<String> defs = new ArrayList<>();
      LibFile.readStrings(defPath.toString(), defs);
      readConfiguration(defs, tagPrefix);
    }
  }

  public void readConfiguration(final List<String> defs, final String tagPrefix) {
    hostGroups.clear();
    for (final String def : defs) {
      if (LibStr.isEmptyOrNull(def) || def.startsWith("#")) {
        continue;
      }
      final Matcher m = regExHostGroup.matcher(def);
      if (m.find()) {
        final String host = m.group(HostGroups.REGEX_GROUP_HOST);
        final String tags = m.group(HostGroups.REGEX_GROUP_TAGS);
        final Matcher g = regExGroups.matcher(tags);
        while (g.find()) {
          final String tag = g.group(1);
          final Set<String> hosts = getHostByTag(tag);
          hosts.add(host);
          if ((tagPrefix == null) || tag.startsWith(tagPrefix)) {
            final Set<String> hostTags = getHostTags(host);
            final int start = (tagPrefix == null) ? 0 : tagPrefix.length();
            hostTags.add(tag.substring(start));
          }
        }
      }
    }
  }

  private synchronized Tags getHostTags(final String host) {
    Tags result = hostTags.get(host);
    if (result == null) {
      result = new Tags();
      hostTags.put(host, result);
    }
    return result;
  }

  public synchronized boolean hasTag(final String tag) {
    return hostGroups.containsKey(tag);
  }

  public synchronized Set<String> getHostByTag(final String tag) {
    Set<String> result = hostGroups.get(tag);
    if (result == null) {
      result = new TreeSet<>();
      hostGroups.put(tag, result);
    }
    return result;
  }

  @Override
  public Tags getTags(final String host) {
    return hostTags.get(host);
  }

}
