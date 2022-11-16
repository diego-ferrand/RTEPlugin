package com.blazemeter.jmeter.rte.core;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import java.awt.Color;
import java.awt.Dimension;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.xmlunit.assertj.XmlAssert;

public class ScreenTest {

  private static final String S1_LITERAL = "S1";
  private static final String F1_LITERAL = "F1";
  private static final String S2_LITERAL = "S2";
  private static final String F2_LITERAL = "F2";
  private final int SCREEN_WIDTH = 5;
  private final int SCREEN_HEIGHT = 2;
  private final String WHITESPACES_FILLED_ROW = StringUtils.repeat(' ', (SCREEN_WIDTH));
  private final Dimension SCREEN_DIMENSION = new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT);

  private Screen buildScreen() {
    return new Screen(new Dimension(SCREEN_DIMENSION));
  }

  @Test
  public void shouldSplitRowWhenGetTextWithSegmentWithLengthBiggerThanScreenWidth() {
    Screen screen = buildScreen();
    addSegment(screen, 0, WHITESPACES_FILLED_ROW + WHITESPACES_FILLED_ROW);
    assertThat(screen.getText())
        .isEqualTo(WHITESPACES_FILLED_ROW + "\n" + WHITESPACES_FILLED_ROW + "\n");
  }

  @Test
  public void shouldSplitRowWhenGetTextWithFinalSegmentPositionGreaterThanScreenWidth() {
    Screen screen = buildScreen();
    int offset = 3;
    addSegment(screen, 0, StringUtils.repeat(' ', offset));
    addSegment(screen, offset, WHITESPACES_FILLED_ROW);
    addSegment(screen, offset, StringUtils.repeat(' ', SCREEN_WIDTH - offset));
    assertThat(screen.getText())
        .isEqualTo(WHITESPACES_FILLED_ROW + "\n" + WHITESPACES_FILLED_ROW + "\n");
  }

  private void addSegment(Screen screen, int i, String repeat) {
    screen.addSegment(new Segment.SegmentBuilder()
        .withLinealPosition(i)
        .withText(repeat)
        .withColor(Screen.DEFAULT_COLOR));
  }

  @Test
  public void shouldGetScreenTextWithAddedFieldsAndSegmentsWhenGetText() {
    Screen screen = new Screen(new Dimension(SCREEN_WIDTH * 3, SCREEN_HEIGHT));
    String segmentText = "Name: ";
    addSegment(screen, 0, segmentText);
    String fieldText = "TESTUSR";
    addField(screen, segmentText.length(), fieldText);
    assertThat(screen.getText())
        .isEqualTo(buildExpectedString(segmentText + fieldText, SCREEN_WIDTH * 3, SCREEN_HEIGHT));

  }

  private String buildExpectedString(String text, int width, int height) {
    StringBuilder str = new StringBuilder(text);
    int begin;
    for (int j = 0; j < height; j++) {
      begin = j != 0 ? 0 : text.length();
      for (int i = begin; i < width; i++) {
        str.append(' ');
      }
      str.append('\n');
    }
    return str.toString();
  }

  @Test
  public void shouldGetScreenTextWithInvisibleCharactersAsSpacesWhenGetText() {
    Screen screen = buildScreen();
    addSegment(screen, 0, "T\u0000est");
    assertThat(screen.getText()).isEqualTo(
        buildExpectedString("T est", screen.getSize().width, screen.getSize().height));
  }

  @Test
  public void shouldGetAddedFieldsAndSegmentsWhenGetSegments() {
    Screen screen = new Screen(new Dimension(SCREEN_WIDTH * 2, SCREEN_HEIGHT));
    addSegment(screen, 0, S1_LITERAL + ": ");
    addField(screen, 4, F1_LITERAL);
    addSegment(screen, SCREEN_WIDTH * 2, S2_LITERAL + ": ");
    addField(screen, SCREEN_WIDTH * 2 + 4, F2_LITERAL);

    List<Segment> expectedSegments = new ArrayList<>(
        Arrays.asList(
            new Segment.SegmentBuilder()
                .withPosition(1, 1)
                .withText(S1_LITERAL + ": ")
                .withColor(Color.GREEN)
                .build(SCREEN_DIMENSION),
            new Segment.SegmentBuilder()
                .withPosition(1, 5)
                .withText(F1_LITERAL)
                .withColor(Color.BLACK)
                .withEditable()
                .build(SCREEN_DIMENSION),
            new Segment.SegmentBuilder()
                .withPosition(2, 1)
                .withText(S2_LITERAL + ": ")
                .withColor(Color.GREEN)
                .build(SCREEN_DIMENSION),
            new Segment.SegmentBuilder()
                .withPosition(2, 5)
                .withText(F2_LITERAL)
                .withColor(Color.BLACK)
                .withEditable()
                .build(SCREEN_DIMENSION)
        )
    );

    assertThat(screen.getSegments()).isEqualTo(expectedSegments);
  }

  private void addField(Screen screen, int linealPosition, String f1Literal) {
    screen.addSegment(new Segment.SegmentBuilder()
        .withText(f1Literal)
        .withLinealPosition(linealPosition)
        .withColor(Color.BLACK)
        .withEditable());
  }

  @Test
  public void shouldGetScreenWithInvisibleCharsAsSpacesWhenWithInvisibleCharsAsSpaces() {
    Screen screen = buildScreen();
    addSegment(screen, 0, "T\u0000est");

    Screen expectedScreen = buildScreen();
    addSegment(expectedScreen, 0, "T est");
    assertThat(screen.withInvisibleCharsToSpaces()).isEqualTo(expectedScreen);
  }

  @Test
  public void shouldGetScreenWithTwoRowsWhenValueOfWithOneEnter() {
    assertThat(screenFromUnnormalizedText("Row1\nRow2").getText()).isEqualTo("Row1\nRow2\n");
  }

  public static Screen screenFromUnnormalizedText(String screen) {
    int width = screen.indexOf('\n');
    int height = screen.length() / (width + 1);
    Screen ret = new Screen(new Dimension(width, height));
    int pos = 0;
    for (String part : screen.split("\n")) {
      ret.addSegment(new Segment.SegmentBuilder()
          .withLinealPosition(pos)
          .withText(part)
          .withColor(Screen.DEFAULT_COLOR));
      pos += width;
    }
    return ret;
  }

  @Test(expected = ArithmeticException.class)
  public void shouldThrowArithmeticExceptionWhenValueOfStringWithoutEnter() {
    screenFromUnnormalizedText("Row1");
  }

  @Test
  public void shouldGetScreenWithFieldsWhenFromHtml() throws Exception {
    assertThat(Screen.fromHtml(getHtmlTestScreenHtml())).isEqualTo(buildHtmlTestScreen());
  }

  private String getHtmlTestScreenHtml() throws IOException {
    return getFileContents("test-screen.html");
  }

  private String getFileContents(String fileName) throws IOException {
    return Resources.toString(getClass().getResource(fileName), Charsets.UTF_8);
  }

  private Screen buildHtmlTestScreen() {
    return buildHtmlTestScreenForUser("USR ");
  }

  private Screen buildHtmlTestScreenForUser(String user) {
    Screen expectedScreen = new Screen(new Dimension(10, 2));
    String initialSegmentText = "  Welcome User: ";
    addSegment(expectedScreen, 0, initialSegmentText);
    addField(expectedScreen, initialSegmentText.length(), "USR ");
    return expectedScreen;
  }

  @Test
  public void shouldGetScreenHtmlWhenGetHtml() throws Exception {
    XmlAssert.assertThat(buildHtmlTestScreen().getHtml())
        .and(getHtmlTestScreenHtml())
        .areIdentical();
  }

  @Test
  public void shouldGetScreenHtmlWithInvisibleCharsAsSpacesWhenGetHtmlWithScreenWithInvisibleChars()
      throws Exception {
    XmlAssert.assertThat(buildHtmlTestScreenForUser("USR\u0000").getHtml())
        .and(getHtmlTestScreenHtml())
        .areIdentical();
  }

}
