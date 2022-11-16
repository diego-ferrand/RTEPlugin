package com.blazemeter.jmeter.rte.core;

import com.blazemeter.jmeter.rte.extractor.PositionRange;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.util.StdConverter;
import java.awt.Color;
import java.awt.Dimension;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Segment {
  
  private static final Logger LOG = LoggerFactory.getLogger(Segment.class);
  private static final ObjectMapper MAPPER = new ObjectMapper();
  private final boolean editable;
  private final boolean secret;
  private final String text;
  private final PositionRange positionRange;
  private final Color color;

  private Segment(PositionRange positionRange, String text, boolean editable, boolean secret,
                  Color color) {
    this.positionRange = positionRange;
    this.text = text;
    this.editable = editable;
    this.secret = secret;
    this.color = color;
  }

  @JsonIgnore
  public Position getStartPosition() {
    return positionRange.getStart();
  }

  @JsonIgnore
  public String getText() {
    return text;
  }

  @JsonProperty(value = "range", index = 1)
  public PositionRange getPositionRange() {
    return positionRange;
  }

  @JsonProperty(index = 2)
  @JsonInclude(JsonInclude.Include.NON_DEFAULT)
  public boolean isEditable() {
    return editable;
  }

  @JsonIgnore
  public Position getEndPosition() {
    return positionRange.getEnd();
  }

  @JsonProperty(index = 3)
  @JsonInclude(JsonInclude.Include.NON_DEFAULT)
  public boolean isSecret() {
    return secret;
  }

  @JsonProperty(index = 4)
  @JsonSerialize(converter = ColorConverter.class)
  public Color getColor() {
    return this.color;
  }

  public static String getColorAsHex(Color color) {
    return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
  }

  protected String getWrappedText(int width) {
    int offset =
        (positionRange.getStart().getColumn() > 0 ? positionRange.getStart().getColumn() : width)
            - 1;
    int pos = 0;
    StringBuilder ret = new StringBuilder();
    while (offset + text.length() - pos >= width) {
      ret.append(text, pos, pos + width - offset);
      ret.append("\n");
      pos += width - offset;
      offset = 0;
    }
    if (pos < text.length()) {
      ret.append(text, pos, text.length());
    }
    // in tn5250 and potentially other protocols, the screen contains non visible characters which
    // are used as markers of no data or additional info. We replace them with spaces for better
    // visualization in text representation.
    return convertInvisibleCharsToSpaces(ret.toString());
  }

  private String convertInvisibleCharsToSpaces(String str) {
    return str.replace('\u0000', ' ');
  }

  protected Segment withInvisibleCharsToSpaces() {
    return new Segment(positionRange, convertInvisibleCharsToSpaces(text), editable, secret,
        color);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Segment segment = (Segment) o;
    return positionRange.getStart().equals(((Segment) o).positionRange.getStart()) &&
        text.equals(segment.text) &&
        editable == segment.editable &&
        secret == segment.secret &&
        color.equals(segment.color);
  }

  @Override
  public int hashCode() {
    return Objects.hash(positionRange.getStart(), text);
  }

  @Override
  public String toString() {
    return "Segment{" +
        "Start position=" + positionRange.getStart().toString() +
        "End position=" + positionRange.getEnd() +
        ", text='" + text + '\'' +
        ", editable=" + editable +
        ", secret=" + secret +
        ", color=" + (color != null ? getColorAsHex(color) : "N/A") +
        '}';
  }

  public String toJSON() {
    try {
      return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(Segment.this);
    } catch (JsonProcessingException e) {
      LOG.error("There was an error while parsing the segments. ", e);
      return "{}";
    }
  }

  public static List<Segment> fromJSON(String json) throws JsonProcessingException {
    SimpleModule module = new SimpleModule();
    module.addDeserializer(Segment.class, buildSegmentDeserializer());
    MAPPER.registerModule(module);
    return MAPPER.readValue(json, new TypeReference<List<Segment>>() {});
  }

  public static List<Segment> fromHeaders(String responseHeaders)
      throws JsonProcessingException {
    String segmentsAsText = extractHeaderSegmentsValue(responseHeaders);
    return Segment.fromJSON(segmentsAsText);
  }

  private static String extractHeaderSegmentsValue(String responseHeaders) {
    int segmentIndex = responseHeaders.indexOf("Segments: ");
    int startPosition = segmentIndex + "Segments: ".length();
    String segmentsValue = responseHeaders.substring(startPosition);
    if (segmentsValue.matches("^\\w+-?\\w+:.")) {
      String[] headers = segmentsValue.split("^\\w+-?\\w+:.");
      return headers[0];
    } else {
      return segmentsValue;
    }
  }

  public static StdDeserializer<Segment> buildSegmentDeserializer() {
    return new StdDeserializer<Segment>(Segment.class) {
      @Override
      public Segment deserialize(JsonParser parser, DeserializationContext context)
          throws IOException {
        JsonNode node = parser.getCodec().readTree(parser);
        PositionRange range = PositionRange.fromStrings(node.get("range").asText());
        SegmentBuilder builder = new SegmentBuilder()
            .withPosition(range.getStart().getRow(), range.getStart().getColumn());
        JsonNode color = node.get("color");
        if (color != null) {
          builder.withColor(Color.decode(color.asText()));
        }
        JsonNode editable = node.get("editable");
        if (editable != null && editable.asText().equals("true")) {
          builder.withEditable();
        }
        JsonNode secret = node.get("secret");
        if (secret != null && secret.asText().equals("true")) {
          builder.withSecret();
        }
        return builder.build(range);
      }
    };
  }

  public static class ColorConverter extends StdConverter<Color, String> {

    @Override
    public String convert(Color color) {
      return "#" + Integer.toHexString(color.getRGB()).substring(2);
    }
  }

  public static class SegmentBuilder {

    private boolean editable = false;
    private boolean secret = false;
    private String text;
    private int firstLinealPosition;
    private Position startPosition;
    private Color color;

    public Segment.SegmentBuilder withEditable() {
      editable = true;
      return this;
    }

    public Segment.SegmentBuilder withSecret() {
      secret = true;
      return this;
    }

    public Segment.SegmentBuilder withText(String text) {
      this.text = text;
      return this;
    }

    public Segment.SegmentBuilder withLinealPosition(int linealPosition) {
      this.firstLinealPosition = linealPosition;
      return this;
    }

    public Segment.SegmentBuilder withPosition(int row, int col) {
      this.startPosition = new Position(row, col);
      return this;
    }

    public Segment.SegmentBuilder withColor(Color color) {
      this.color = color;
      return this;
    }

    private Position calculateEndPosition(Dimension screenSize, Position startPosition) {
      int startLinealPosition = Screen.buildLinealPosition(startPosition, screenSize.width);
      int endLinealPosition = startLinealPosition + text.length();
      int maxLinealPos = screenSize.width * screenSize.height;
      //circular field use case
      if (maxLinealPos < endLinealPosition) {
        return Screen.buildPositionFromLinearPosition(Math.abs(endLinealPosition - maxLinealPos),
            screenSize.width);
      }
      endLinealPosition = startPosition.getColumn() + text.length();
      return new Position(startPosition.getRow() + (endLinealPosition - 1) / screenSize.width,
          (endLinealPosition - 1) % screenSize.width + 1);
    }

    public Segment build(Dimension screenSize) {
      Position position = startPosition == null
          ? Screen.buildPositionFromLinearPosition(firstLinealPosition,
          screenSize.width) : startPosition;
      PositionRange positionRange = new PositionRange(position,
          calculateEndPosition(screenSize, position));
      return new Segment(positionRange, text, editable, secret, color);
    }

    //Used for deserialization from Response's headers
    public Segment build(PositionRange range) {
      return new Segment(range, text, editable, secret, color);
    }
  }
}
