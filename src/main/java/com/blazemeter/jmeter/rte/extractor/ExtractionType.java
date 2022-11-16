package com.blazemeter.jmeter.rte.extractor;

import java.util.Locale;

public enum ExtractionType {
  NEXT_FIELD_POSITION("Next field position", ExtractionAction.POSITIONS),
  CURSOR_POSITION("Cursor position", ExtractionAction.POSITIONS),
  COLOR("Color", ExtractionAction.ATTRIBUTES);

  public String label;
  public ExtractionAction extractionAction;

  ExtractionType(String label, ExtractionAction extractionAction) {
    this.extractionAction = extractionAction;
    this.label = label;
  }

  public String getLabel() {
    return label;
  }

  public static ExtractionType from(String name) {
    switch (name.toLowerCase(Locale.ROOT)) {
      case "Next field position":
        return NEXT_FIELD_POSITION;
      case "Cursor position":
        return CURSOR_POSITION;
      case "Color":
        return COLOR;
      default:
        throw new UnsupportedOperationException(String.format("Option {%s} is not supported",
            name));
    }
  }
  
  public enum ExtractionAction {
    POSITIONS("Position Extraction"),
    ATTRIBUTES("Color Extraction");

    private final String label;
    
    ExtractionAction(String label) {
      this.label = label;
    }

    public String getLabel() {
      return label;
    }
  }
}

