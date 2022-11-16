package com.blazemeter.jmeter.rte.core;

import java.awt.Color;
import java.awt.Dimension;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class Screen {

  public static final Color DEFAULT_COLOR = Color.GREEN;
  public static final Color SECRET_FIELD_COLOR = Color.BLACK;
  private List<Segment> segments = new ArrayList<>();
  private Dimension size;

  // Provided for proper deserialization of sample results
  public Screen() {
  }

  public Screen(Dimension size) {
    this.size = size;
  }

  public Screen(Screen other) {
    if (other != null) {
      segments = new ArrayList<>(other.segments);
      size = other.size;
    }
  }

  public static Screen buildScreenFromText(String screenText, Dimension screenSize) {
    Screen scr = new Screen(screenSize);
    scr.addSegment(
        getSegmentBuilder(0, screenText.replace("\n", ""))
            .withColor(DEFAULT_COLOR));
    return scr;
  }

  public static Screen fromHtml(String html) {
    Document doc;
    try {
      doc = parseHtmlDocument(html);
    } catch (ParserConfigurationException | SAXException | IOException e) {
      throw new RuntimeException(e);
    }
    Element root = doc.getDocumentElement();
    Element head = (Element) root.getElementsByTagName("head").item(0);
    Element meta = (Element) head.getElementsByTagName("meta").item(0);
    String sizeStr = meta.getAttribute("content");
    int separatorIndex = sizeStr.indexOf('x');
    Dimension screenSize = new Dimension(Integer.parseInt(sizeStr.substring(separatorIndex + 1)),
        Integer.parseInt(sizeStr.substring(0, separatorIndex)));
    Screen ret = new Screen(screenSize);
    NodeList pres = root.getElementsByTagName("pre");
    int linealPosition = 0;
    for (int i = 0; i < pres.getLength(); i++) {
      Element pre = (Element) pres.item(i);
      String segmentText = pre.getTextContent().replace("\n", "");
      Optional<Color> color = getColorFromElement(pre);
      Segment.SegmentBuilder segmentBuilder = getSegmentBuilder(linealPosition, segmentText);
      if ("true".equals(pre.getAttribute("contenteditable"))) {
        segmentBuilder.withEditable();
        if ("true".equals(pre.getAttribute("secretcontent"))) {
          ret.addSegment(segmentBuilder.withSecret().withColor(color.orElse(SECRET_FIELD_COLOR)));
        } else {
          ret.addSegment(segmentBuilder.withColor(color.orElse(DEFAULT_COLOR)));
        }
      } else {
        ret.addSegment(segmentBuilder.withColor(color.orElse(DEFAULT_COLOR)));
      }
      linealPosition += segmentText.length();
    }
    return ret;
  }

  private static Optional<Color> getColorFromElement(Element pre) {
    String colorClass = pre.getAttribute("class");
    return colorClass == null || colorClass.isEmpty() ? Optional.empty()
        //remove the "pre-" prefix
        : Optional.of(Color.decode("#" + colorClass.substring(4)));
  }

  private static Document parseHtmlDocument(String html)
      throws SAXException, IOException, ParserConfigurationException {
    InputSource is = new InputSource();
    is.setCharacterStream(new StringReader(html));
    return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
  }

  public static Position buildPositionFromLinearPosition(int linealPosition, int width) {
    return new Position(linealPosition / width + 1, linealPosition % width + 1);
  }

  public Dimension getSize() {
    return size;
  }

  public List<Segment> getSegments() {
    return segments;
  }

  public void addSegment(Segment.SegmentBuilder segmentBuilder) {
    segments.add(segmentBuilder.build(size));
  }

  private static Segment.SegmentBuilder getSegmentBuilder(int linealPosition, String text) {
    return new Segment.SegmentBuilder()
        .withLinealPosition(linealPosition)
        .withText(text);
  }

  public String getText() {
    StringBuilder screen = new StringBuilder();
    int nextScreenPosition = 0;
    for (Segment segment : segments) {
      int segmentPosition = buildLinealPosition(segment.getStartPosition(), size.width);
      if (segmentPosition != nextScreenPosition) {
        Segment fillSegment = buildBlankSegmentForRange(nextScreenPosition, segmentPosition);
        screen.append(fillSegment.getWrappedText(size.width));
        nextScreenPosition += fillSegment.getText().length();

      }
      screen.append(segment.getWrappedText(size.width));
      nextScreenPosition += segment.getText().length();
    }
    int lastScreenPosition = size.width * size.height;
    if (nextScreenPosition < lastScreenPosition) {
      screen.append(buildBlankSegmentForRange(nextScreenPosition, lastScreenPosition)
          .getWrappedText(size.width));
    }

    return screen.toString();
  }

  private Segment buildBlankSegmentForRange(int firstPosition, int lastPosition) {
    return getSegmentBuilder(firstPosition,
        buildBlankString(lastPosition - firstPosition))
        .withColor(SECRET_FIELD_COLOR)
        .build(size);
  }

  private String buildBlankString(int length) {
    return StringUtils.repeat(' ', length);
  }

  public static int buildLinealPosition(Position position, int width) {
    return width * (position.getRow() - 1) + position.getColumn() - 1;
  }

  public void fillScreenWithNullFrom(int currentLinealPosition) {
    int positionsToBeFilled =
        (size.width * size.height) - currentLinealPosition - 1;
    addSegment(new Segment.SegmentBuilder()
        .withLinealPosition(currentLinealPosition)
        .withText(StringUtils.repeat('\u0000', positionsToBeFilled))
        .withColor(Screen.DEFAULT_COLOR));
  }

  public Screen withInvisibleCharsToSpaces() {
    Screen ret = new Screen(size);
    for (Segment s : segments) {
      ret.segments.add(s.withInvisibleCharsToSpaces());
    }
    return ret;
  }

  public String getHtml() {
    return buildHtmlDocumentString(buildHtmlDocument());
  }

  private Document buildHtmlDocument() {
    try {
      Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
      Element root = doc.createElement("html");
      doc.appendChild(root);
      appendHtmlHead(doc, root);
      appendHtmlBody(doc, root);
      return doc;
    } catch (ParserConfigurationException e) {
      throw new RuntimeException(e);
    }
  }

  private void appendHtmlHead(Document doc, Element root) {
    Element head = appendHtmlChild("head", root, doc);
    Element meta = appendHtmlChild("meta", head, doc);
    meta.setAttribute("name", "screen-size");
    meta.setAttribute("content", size.height + "x" + size.width);
    Element style = appendHtmlChild("style", head, doc);
    style.setTextContent(buildStyles());
  }

  private String buildStyles() {
    return segments.stream()
        .map(segment -> Integer.toHexString(segment.getColor().getRGB()).substring(2))
        .distinct()
        .map(c -> String.format(".pre-%s { display: inline; background: black; color: #%1$s; }", c))
        .sorted()
        .collect(Collectors.joining("\n"));
  }

  private Element appendHtmlChild(String childName, Element parent, Document doc) {
    Element element = doc.createElement(childName);
    parent.appendChild(element);
    return element;
  }

  private void appendHtmlBody(Document doc, Element root) {
    Element body = appendHtmlChild("body", root, doc);
    for (Segment segment : segments) {
      Element pre = appendHtmlChild("pre", body, doc);
      pre.setAttribute("class",
          "pre-" + Integer.toHexString(segment.getColor().getRGB()).substring(2));
      if (segment.isEditable()) {
        pre.setAttribute("contenteditable", "true");
      }
      if (segment.isSecret()) {
        pre.setAttribute("secretcontent", "true");
      }
      pre.setTextContent(segment.getWrappedText(size.width));
    }
  }

  private String buildHtmlDocumentString(Document doc) {
    try {
      DOMImplementationLS impl = (DOMImplementationLS) DOMImplementationRegistry.newInstance()
          .getDOMImplementation("LS");
      LSSerializer serializer = impl.createLSSerializer();
      serializer.getDomConfig().setParameter("xml-declaration", false);
      return serializer.writeToString(doc);
    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  public static String replaceTrailingSpacesByNull(String str) {
    StringBuilder nulls = new StringBuilder();
    int i = str.length() - 1;
    while (i >= 0 && str.charAt(i) == ' ') {
      nulls.append('\u0000');
      i--;
    }
    return str.substring(0, i + 1) + nulls;
  }

  @Override
  public String toString() {
    return getHtml();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Screen screen = (Screen) o;
    return segments.equals(screen.segments) &&
        size.equals(screen.size);
  }

  @Override
  public int hashCode() {
    return Objects.hash(segments, size);
  }
}
