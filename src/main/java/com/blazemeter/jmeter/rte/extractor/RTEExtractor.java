package com.blazemeter.jmeter.rte.extractor;

import com.blazemeter.jmeter.rte.core.Position;
import com.blazemeter.jmeter.rte.core.RteSampleResultBuilder;
import com.blazemeter.jmeter.rte.core.Screen;
import com.blazemeter.jmeter.rte.core.Segment;
import com.blazemeter.jmeter.rte.core.TerminalType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.helger.commons.annotation.VisibleForTesting;
import java.awt.Dimension;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.jmeter.processor.PostProcessor;
import org.apache.jmeter.testelement.AbstractScopedTestElement;
import org.apache.jmeter.testelement.TestElement;
import org.apache.jmeter.threads.JMeterContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RTEExtractor extends AbstractScopedTestElement implements PostProcessor {

  private static final Logger LOG = LoggerFactory.getLogger(RTEExtractor.class);

  private static final String ROW_PROPERTY = "RTEExtractor.row";
  private static final String COLUMN_PROPERTY = "RTEExtractor.column";
  private static final String OFFSET_PROPERTY = "RTEExtractor.offset";
  private static final String VARIABLE_PREFIX_PROPERTY = "RTEExtractor.variablePrefix";
  private static final String EXTRACTION_TYPE_PROPERTY = "RTEExtractor.extractionType";
  private static final String COLOR_PROPERTY = "RTEExtractor.color";
  private static final ExtractionType DEFAULT_POSITION_TYPE = ExtractionType.NEXT_FIELD_POSITION;
  private static final int UNSPECIFIED_COORDS = 1;
  private static final int UNSPECIFIED_OFFSET = 0;
  private JMeterContext context;

  public RTEExtractor() {

  }

  @Override
  public void process() {
    LOG.info("RTE-Extractor {}: processing result", getProperty(TestElement.NAME));
    JMeterContext context = this.context != null ? this.context : getThreadContext();
    this.context = context;
    String variablePrefix = getVariablePrefix();

    if (variablePrefix.isEmpty()) {
      LOG.error("The variable name in extractor is essential for later usage");
      return;
    }

    String responseHeaders = context.getPreviousResult().getResponseHeaders();
    ExtractionType type = getExtractionType();

    if (type.equals(ExtractionType.CURSOR_POSITION)) {
      storeCursorPosition(variablePrefix, responseHeaders);
      return;
    }

    List<Segment> segments;
    try {
      segments = Segment.fromHeaders(responseHeaders);
    } catch (JsonProcessingException e) {
      LOG.error("Error parsing response headers", e);
      return;
    }

    if (type.equals(ExtractionType.NEXT_FIELD_POSITION)) {
      storeNextFieldPosition(variablePrefix, segments);
      return;
    }

    if (type.equals(ExtractionType.COLOR)) {
      storeColorAttribute(segments);
    }
  }

  private void storeCursorPosition(String variablePrefix, String responseHeaders) {
    storePosition(variablePrefix, extractCursorPosition(responseHeaders));
  }

  private void storePosition(String variablePrefix, Position position) {
    context.getVariables().put(variablePrefix + "_COLUMN", String.valueOf(position.getColumn()));
    context.getVariables().put(variablePrefix + "_ROW", String.valueOf(position.getRow()));
  }

  private void storeNextFieldPosition(String variablePrefix, List<Segment> segments) {
    String requestHeaders = context.getPreviousResult().getRequestHeaders();
    if (!isGivenFieldPositionValid(requestHeaders)) {
      LOG.error("Inserted values for row and column {} in extractor do not match with " +
          "the screen size {}.", getBasePosition(), getScreenDimensions(requestHeaders));
      return;
    }

    List<PositionRange> editablePositions = segments.stream()
        .filter(Segment::isEditable)
        .map(Segment::getPositionRange)
        .collect(Collectors.toList());

    Position position = findField(getBasePosition(), editablePositions, getOffsetAsInt());
    if (position == null) {
      LOG.error("No field found in for row and column {} with offset {}",
          getColumn(), getOffset());
      return;
    }
    storePosition(variablePrefix, position);
  }

  private void storeColorAttribute(List<Segment> segments) {
    int width = (int) getScreenDimensions(context.getPreviousResult().getRequestHeaders())
        .getWidth();

    Optional<Segment> coloredSegment = findAnySegmentInPosition(segments,
        width, getBasePosition());

    if (!coloredSegment.isPresent()) {
      LOG.error("No segment found with color at position {}", getBasePosition());
      return;
    }

    Segment segment = coloredSegment.get();
    context.getVariables().put(getVariablePrefix(), Segment.getColorAsHex(segment.getColor()));
  }

  public static Optional<Segment> findColorSegmentByPosition(List<Segment> segments,
                                                             Position position) {
    Segment foundSegment = null;
    for (Segment segment : segments) {
      if (segment.getPositionRange().contains(position)) {
        foundSegment = segment;
      }
    }
    return Optional.ofNullable(foundSegment);
  }

  public static Optional<Segment> findAnySegmentInPosition(List<Segment> segments, int width,
      Position position) {
    return segments.stream()
        .filter(segment -> Screen.buildLinealPosition(segment.getStartPosition(), width)
            <= Screen.buildLinealPosition(position, width)
            && Screen.buildLinealPosition(segment.getEndPosition(), width)
            >= Screen.buildLinealPosition(position, width)
            && segment.getColor() != null)
        .findAny();
  }

  private Position extractCursorPosition(String responseHeaders) {
    return Position.fromString(
        extractHeaderValue(RteSampleResultBuilder.CURSOR_POSITION_HEADER, responseHeaders));
  }

  private static String extractHeaderValue(String headerName, String responseHeaders) {
    int startPosition = responseHeaders.indexOf(headerName) + headerName.length();
    int endPosition = responseHeaders
        .indexOf(RteSampleResultBuilder.HEADERS_SEPARATOR, startPosition);
    return responseHeaders.substring(startPosition, endPosition == -1
            ? responseHeaders.length() : endPosition);
  }

  private int getOffsetAsInt() {
    return getOffset().isEmpty() ? UNSPECIFIED_OFFSET : Integer.parseInt(getOffset());
  }

  private boolean isGivenFieldPositionValid(String requestHeaders) {
    return getBasePosition().isInside(getScreenDimensions(requestHeaders));
  }

  private Position getBasePosition() {
    return new Position(getRowAsInt(), getColumnAsInt());
  }

  private Dimension getScreenDimensions(String requestHeaders) {
    return TerminalType.fromString(extractTerminalType(requestHeaders)).getScreenSize();
  }

  private Position findField(Position basePosition, List<PositionRange> fieldPositionRanges,
      int offset) {
    int basePositionIndex = findBasePositionIndex(basePosition, fieldPositionRanges, offset);
    try {
      return fieldPositionRanges.get(basePositionIndex + offset).getStart();
    } catch (IndexOutOfBoundsException e) {
      LOG.error(
          "Couldn't find a field from {} with offset {} in screen fields {}",
          basePositionIndex, getOffsetAsInt(),
          fieldPositionRanges.stream().map(PositionRange::toString));
      return null;
    }
  }

  private int findBasePositionIndex(Position basePosition, List<PositionRange> fieldPositionRanges,
      int offset) {
    int index = 0;
    for (PositionRange fieldRange : fieldPositionRanges) {
      if (fieldRange.contains(basePosition)) {
        return index;
      } else if (basePosition.compare(fieldRange.getStart()) < 0) {
        return offset > 0 ? index - 1 : index;
      }
      index++;
    }
    return offset > 0 ? index - 1 : index;
  }

  public static String extractTerminalType(String requestHeaders) {
    return extractHeaderValue(RteSampleResultBuilder.HEADERS_TERMINAL_TYPE, requestHeaders);
  }

  private int getRowAsInt() {
    return getRow().isEmpty() ? UNSPECIFIED_COORDS : Integer.parseInt(getRow());
  }

  private int getColumnAsInt() {
    return getColumn().isEmpty() ? UNSPECIFIED_COORDS : Integer.parseInt(getColumn());
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

  public String getOffset() {
    return getPropertyAsString(OFFSET_PROPERTY);
  }

  public void setOffset(String offset) {
    setProperty(OFFSET_PROPERTY, offset, "0");
  }

  public String getVariablePrefix() {
    return getPropertyAsString(VARIABLE_PREFIX_PROPERTY);
  }

  public void setVariablePrefix(String prefix) {
    setProperty(VARIABLE_PREFIX_PROPERTY, prefix);
  }

  public ExtractionType getExtractionType() {
    return ExtractionType.valueOf(getPropertyAsString(EXTRACTION_TYPE_PROPERTY,
        DEFAULT_POSITION_TYPE.name()));
  }

  public void setExtractionType(ExtractionType extractionType) {
    setProperty(EXTRACTION_TYPE_PROPERTY, extractionType.name());
  }

  @VisibleForTesting
  public void setContext(JMeterContext context) {
    this.context = context;
  }

}
