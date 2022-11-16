package com.blazemeter.jmeter.rte.core.assertions;

import com.blazemeter.jmeter.rte.core.ColorUtils;
import com.blazemeter.jmeter.rte.core.Position;
import com.blazemeter.jmeter.rte.core.Segment;
import com.blazemeter.jmeter.rte.core.TerminalType;
import com.blazemeter.jmeter.rte.extractor.RTEExtractor;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.awt.Color;
import java.util.List;
import java.util.Optional;
import org.apache.jmeter.assertions.Assertion;
import org.apache.jmeter.assertions.AssertionResult;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.testelement.AbstractScopedAssertion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RTEAssertion extends AbstractScopedAssertion implements Assertion {

  private static final Logger LOG = LoggerFactory.getLogger(RTEAssertion.class);

  private static final String ROW_PROPERTY = "RTEAssertion.row";
  private static final String COLUMN_PROPERTY = "RTEAssertion.column";
  private static final String COLOR_PROPERTY = "RTEAssertion.colorChooser";

  @Override
  public AssertionResult getResult(SampleResult response) {
    List<Segment> segments = this.getSegments(response);

    AssertionResult assertion = new AssertionResult("colorAssertion");
    Optional<Segment> anySegmentInPosition =
        RTEExtractor.findColorSegmentByPosition(segments, getPosition());

    if (!anySegmentInPosition.isPresent()) {
      assertion.setResultForFailure("No segment found at position " + getPosition());
      return assertion;
    }

    Color color = anySegmentInPosition.get().getColor();
    String actualHexColor = ColorUtils.getHex(color);

    String expectedHexColor = getColor();
    if (!expectedHexColor.equalsIgnoreCase(actualHexColor)) {
      assertion.setResultForFailure("The expected color (" + expectedHexColor + ") is different "
          + "from the actual (" + actualHexColor + ") in the position " + getPosition());

      return assertion;
    }

    return assertion;
  }

  private Position getPosition() {
    return new Position(Integer.parseInt(getRow()), Integer.parseInt(getColumn()));
  }

  private List<Segment> getSegments(SampleResult response) {
    List<Segment> segments = null;

    try {
      segments = Segment.fromHeaders(response.getResponseHeaders());
    } catch (JsonProcessingException e) {
      LOG.error("Error parsing response headers", e);
    }

    return segments;
  }

  private int size(String requestHeaders) {
    return TerminalType.fromString(RTEExtractor.extractTerminalType(requestHeaders))
        .getScreenSize().width;
  }

  public String getRow() {
    return getPropertyAsString(ROW_PROPERTY);
  }

  public void setRow(String row) {
    setProperty(ROW_PROPERTY, row);
  }

  public String getColumn() {
    return getPropertyAsString(COLUMN_PROPERTY);
  }

  public void setColumn(String column) {
    setProperty(COLUMN_PROPERTY, column);
  }

  public String getColor() {
    return getPropertyAsString(COLOR_PROPERTY);
  }

  public void setColor(String color) {
    setProperty(COLOR_PROPERTY, color);
  }

}

