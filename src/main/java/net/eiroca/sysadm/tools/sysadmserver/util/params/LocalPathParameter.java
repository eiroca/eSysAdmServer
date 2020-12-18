package net.eiroca.sysadm.tools.sysadmserver.util.params;

import java.nio.file.Path;
import java.nio.file.Paths;
import net.eiroca.library.config.Parameters;
import net.eiroca.library.config.parameter.PathParameter;
import net.eiroca.sysadm.tools.sysadmserver.SystemConfig;

public final class LocalPathParameter extends PathParameter {

  String defPath;

  public LocalPathParameter(final Parameters owner, final String paramName, final String defPathStr) {
    super(owner, paramName, null);
    defPath = defPathStr;
  }

  @Override
  public Path getDefault() {
    return Paths.get(SystemConfig.basePath + defPath);

  }
}
