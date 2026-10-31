package org.acme.reporter.util;

import lombok.experimental.UtilityClass;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

@UtilityClass
public class Envs {

  public String resolve(Environment environment, String fallback, String... names) {
    for (String name : names) {
      String value = environment.getProperty(name);
      if (StringUtils.hasText(value)) {
        return value;
      }
    }
    return fallback;
  }
}
