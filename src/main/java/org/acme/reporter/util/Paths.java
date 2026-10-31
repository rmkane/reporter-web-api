package org.acme.reporter.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Paths {

  public String ensureLeadingSlash(String path) {
    if (path.startsWith("/")) {
      return path;
    }
    return "/" + path;
  }
}
