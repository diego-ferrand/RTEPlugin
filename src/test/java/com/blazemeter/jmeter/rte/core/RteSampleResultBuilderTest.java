package com.blazemeter.jmeter.rte.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.blazemeter.jmeter.rte.core.ssl.SSLType;
import com.blazemeter.jmeter.rte.sampler.Action;
import java.awt.Dimension;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RteSampleResultBuilderTest {

  public static final String FIELD_POSITION = "Field-positions: [(1,1)-(1,20)]";
  public static final String EMPTY_FIELD_POSITION = "Field-positions: ";
  public static final String SOUNDED_ALARM = "Sound-Alarm: true";
  public static final String NOT_SOUNDED_ALARM = "Sound-Alarm: false";
  public static final Dimension SCREEN_SIZE = new Dimension(30, 1);
  private static final Position CURSOR_POSITION = new Position(1, 1);
  private static final String CURSOR_POSITION_HEADER = "Cursor-position: (1,1)";
  private static final String INPUT_INHIBITED = "Input-inhibited: true";
  private static final String EXPECTED_HEADERS = "Server: Test Server\n" +
      "Port: 2123\n" +
      "Protocol: TN5250\n" +
      "Terminal-type: IBM-3179-2: 24x80\n" +
      "Security: NONE\n" +
      "Action: CONNECT\n";
  private static final String SCREEN_TEXT = "Testing screen text";
  private static final Screen SCREEN = buildScreen();
  private static final List<Input> CUSTOM_INPUTS = Collections
      .singletonList(new CoordInput(new Position(3, 2), "input"));
  private static final String SEGMENT_HEADER = "Segments: [{\n  \"range\" : \"[(1,1)-(1,20)]\",\n"
      + "  \"editable\" : true,\n  \"color\" : \"#00ff00\"\n}]";
  private static final String DEPRECATED_HEADERS = "Deprecated-headers: Field-positions";
  private static final String LINE_BREAK = "\n";

  @Mock
  private RteProtocolClient client;

  private static Screen buildScreen() {
    Screen screen = new Screen(SCREEN_SIZE);
    screen.addSegment(new Segment.SegmentBuilder().withLinealPosition(0)
        .withText(SCREEN_TEXT)
        .withColor(Screen.DEFAULT_COLOR)
        .withEditable());
    return screen;
  }

  @Before
  public void setUp() {
    when(client.getScreen()).thenReturn(SCREEN);
    when(client.isAlarmOn()).thenReturn(true);
    when(client.isInputInhibited()).thenReturn(Optional.of(true));
    when(client.getCursorPosition()).thenReturn(Optional.of(CURSOR_POSITION));
  }

  @Test
  public void shouldGetConnectionInfoAndActionWhenGetRequestHeaders() {
    RteSampleResultBuilder resultBuilder = buildBasicResultBuilder()
        .withInputInhibitedRequest(true);

    String expectedHeaders = EXPECTED_HEADERS +
        "Input-inhibited: true\n";
    assertThat(resultBuilder.build().getRequestHeaders()).isEqualTo(expectedHeaders);
  }

  private RteSampleResultBuilder buildBasicResultBuilder() {
    return new RteSampleResultBuilder()
        .withAction(Action.CONNECT)
        .withProtocol(Protocol.TN5250)
        .withTerminalType(new TerminalType("IBM-3179-2", new Dimension(80, 24)))
        .withServer("Test Server")
        .withPort(2123)
        .withSslType(SSLType.NONE);
  }

  @Test
  public void shouldGetNoInputInhibitedHeaderWhenGetRequestHeadersWithNotSetInputInhibitedRequest() {
    RteSampleResultBuilder resultBuilder = buildBasicResultBuilder();
    assertThat(resultBuilder.build().getRequestHeaders()).isEqualTo(EXPECTED_HEADERS);
  }

  @Test
  public void shouldGetEmptyStringWhenGetSamplerDataWithNoAttentionKey() {
    RteSampleResultBuilder resultBuilder = buildBasicResultBuilder();
    assertThat(resultBuilder.build().getSamplerData()).isEqualTo("");
  }

  @Test
  public void shouldGetAttentionKeysAndInputsWhenGetSamplerDataWithAttentionKey() {
    RteSampleResultBuilder resultBuilder = buildBasicResultBuilder()
        .withInputs(CUSTOM_INPUTS)
        .withAttentionKey(AttentionKey.ENTER);
    String expectedSamplerData = "AttentionKey: ENTER\n" +
        "Inputs:\n" +
        "3,2,input\n";
    assertThat(resultBuilder.build().getSamplerData()).isEqualTo(expectedSamplerData);
  }

  @Test
  public void shouldGetTerminalStatusHeadersWhenGetResponseHeadersWithSuccessResponse() {
    RteSampleResultBuilder resultBuilder = buildBasicResultBuilder()
        .withSuccessResponse(client);

    assertThat(resultBuilder.build().getResponseHeaders()).isEqualTo(String.join(LINE_BREAK,
        DEPRECATED_HEADERS, SOUNDED_ALARM, CURSOR_POSITION_HEADER, SEGMENT_HEADER, INPUT_INHIBITED,
        FIELD_POSITION));
  }

  @Test
  public void shouldGetEmptyStringWhenGetResponseHeadersWithSuccessDisconnectAction() {
    RteSampleResultBuilder resultBuilder = buildBasicResultBuilder()
        .withAction(Action.DISCONNECT)
        .withSuccessResponse(null);
    assertThat(resultBuilder.build().getResponseHeaders()).isEqualTo("");
  }

  @Test
  public void shouldGetNoSoundAlarmHeaderWhenGetResponseHeadersAndNoSoundAlarm() {
    when(client.isAlarmOn()).thenReturn(false);
    RteSampleResultBuilder resultBuilder = buildBasicResultBuilder()
        .withSuccessResponse(client);

    assertThat(resultBuilder.build().getResponseHeaders()).isEqualTo(String.join(LINE_BREAK,
        DEPRECATED_HEADERS, NOT_SOUNDED_ALARM, CURSOR_POSITION_HEADER, SEGMENT_HEADER,
        INPUT_INHIBITED,
        FIELD_POSITION));
  }

  @Test
  public void shouldGetScreenTextWhenGetResponseData() {
    RteSampleResultBuilder resultBuilder = buildBasicResultBuilder()
        .withSuccessResponse(client);
    assertThat(resultBuilder.build().getResponseDataAsString())
        .isEqualTo(StringUtils.rightPad(SCREEN_TEXT, SCREEN.getSize().width) + "\n");
  }

  @Test
  public void shouldGetEmptyStringWhenGetResponseDataWithoutScreen() {
    RteSampleResultBuilder resultBuilder = buildBasicResultBuilder()
        .withSuccessResponse(null);
    assertThat(resultBuilder.build().getResponseDataAsString()).isEqualTo("");
  }

  @Test
  public void shouldGetEmptyScreenFieldsWhenNoScreenFields() {
    RteSampleResultBuilder resultBuilder = new RteSampleResultBuilder(new Position(1, 1), null,
        null, null);
    when(client.getScreen()).thenReturn(null);
    resultBuilder.withInputInhibitedRequest(true)
        .withSuccessResponse(client);

    assertThat(resultBuilder.build().getResponseHeaders()).isEqualTo(String.join(LINE_BREAK,
        DEPRECATED_HEADERS, SOUNDED_ALARM, CURSOR_POSITION_HEADER, "Segments: ", INPUT_INHIBITED,
        EMPTY_FIELD_POSITION));
  }

  @Test
  public void shouldGetHeadersWithFieldWhenScreenContainFields() {
    RteSampleResultBuilder resultBuilder = new RteSampleResultBuilder(new Position(1, 1),
        buildScreen(), null, new TerminalType("IBM-3179-2", new Dimension(24, 80)));
    resultBuilder.withSuccessResponse(client);

    assertThat(resultBuilder.build().getResponseHeaders())
        .isEqualTo(
            String.join(LINE_BREAK, DEPRECATED_HEADERS, SOUNDED_ALARM, CURSOR_POSITION_HEADER,
                SEGMENT_HEADER, INPUT_INHIBITED, FIELD_POSITION));
  }
}
