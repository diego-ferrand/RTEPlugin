package com.blazemeter.jmeter.rte.core.assertions;

import static org.assertj.core.api.Assertions.assertThat;

import com.blazemeter.jmeter.rte.JMeterTestUtils;
import com.blazemeter.jmeter.rte.core.Protocol;
import com.blazemeter.jmeter.rte.core.RteSampleResultBuilder;
import com.blazemeter.jmeter.rte.core.TerminalType;
import com.blazemeter.jmeter.rte.core.ssl.SSLType;
import com.blazemeter.jmeter.rte.sampler.Action;
import java.awt.Dimension;
import org.apache.jmeter.samplers.SampleResult;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RTEAssertionTest {

  private static final String SEGMENTS =
      "{\"range\":\"[(2,25)-(2,30)]\",\"editable\":true, \"color\":\"#00ff00\"}," +
          "{\"range\":\"[(4,25)-(4,30)]\",\"editable\":true, \"color\":\"#FF0002\"}";

  private static final String SEGMENT_HEADER = "Segments: [" + SEGMENTS + "]";
  private static final String RESPONSE_HEADERS = "Input-inhibited: true\n" +
      "Cursor-position: (1,1)" + '\n' +
      "Field-positions: [(2,25)-(2,30)], [(4,25)-(4,30)]\n" +
      SEGMENT_HEADER;

  private RTEAssertion rteAssertion;

  @Rule
  public JUnitSoftAssertions softly = new JUnitSoftAssertions();

  @BeforeClass
  public static void setupClass() {
    JMeterTestUtils.setupJmeterEnv();
  }

  @Before
  public void setup() {
    rteAssertion = new RTEAssertion();
  }

  private static SampleResult getCustomizedResult() {
    TerminalType terminalType = new TerminalType("IBM-3179-2", new Dimension(80, 24));
    RteSampleResultBuilder ret = new RteSampleResultBuilder(null, null,
        RESPONSE_HEADERS, terminalType)
        .withLabel("bzm-Connect")
        .withServer("localhost")
        .withPort(2526)
        .withProtocol(Protocol.TN3270)
        .withTerminalType(terminalType)
        .withSslType(SSLType.NONE)
        .withAction(Action.SEND_INPUT)
        .withConnectEndNow()
        .withLatencyEndNow();

    return ret.build();
  }

  @Test
  public void shouldValidateScreenSizeWhenWrongRowOrColumn() {
    rteAssertion.setRow("24");
    rteAssertion.setColumn("90");

    softly.assertThat(rteAssertion.getResult(getCustomizedResult()).isFailure()).isTrue();
    softly.assertThat(rteAssertion.getResult(getCustomizedResult()).getFailureMessage())
        .isEqualTo("No segment found at position (24,90)");
  }

  @Test
  public void shouldReturnSuccessWhenExpectedColorMatchesColoredSegment() {
    rteAssertion.setRow("4");
    rteAssertion.setColumn("27");
    rteAssertion.setColor("#FF0002");

    assertThat(rteAssertion.getResult(getCustomizedResult()).isFailure()).isFalse();
  }

  @Test
  public void shouldReturnFailureWhenExpectedColorDoesNotMatchColoredSegment() {
    rteAssertion.setRow("4");
    rteAssertion.setColumn("27");
    rteAssertion.setColor("#00ff00");

    softly.assertThat(rteAssertion.getResult(getCustomizedResult()).isFailure()).isTrue();
    softly.assertThat(rteAssertion.getResult(getCustomizedResult()).getFailureMessage())
        .isEqualTo("The expected color (#00ff00) is different from the actual (#ff0002)"
            + " in the position (4,27)");
  }

}
