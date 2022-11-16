package com.blazemeter.jmeter.rte.core;

import com.bytezone.dm3270.attributes.ColorAttribute;
import java.awt.Color;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ColorUtils {
  private static final Logger LOG = LoggerFactory.getLogger(ColorUtils.class);

  public static String getHex(Color color) {
    return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
  }

  public static Color parseColor(String colorString) {
    if (colorString.matches("rgb *\\( *([0-9]+), *([0-9]+), *([0-9]+) *\\)")) {
      return parseRGB(colorString);
    } else if (colorString.matches("^#(([0-9a-fA-F]{2}){3}|([0-9a-fA-F]){3})$")) {
      try {
        return Color.decode(colorString);
      } catch (NumberFormatException e) {
        LOG.error("Cannot parse this color {}.", colorString, e);
      }
    } else {
      return ColorAttribute.getColor(colorString);
    }
    return null;
  }

  public static Color parseRGB(String input) {
    Pattern c = Pattern.compile("rgb *\\( *([0-9]+), *([0-9]+), *([0-9]+) *\\)");
    Matcher m = c.matcher(input);

    if (m.matches()) {
      return new Color(Integer.parseInt(m.group(1)),  // r
          Integer.parseInt(m.group(2)),  // g
          Integer.parseInt(m.group(3))); // b
    }

    return null;
  }

}
