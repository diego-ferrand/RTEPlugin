package com.blazemeter.jmeter.rte.extractor;

import com.blazemeter.jmeter.rte.JMeterTestUtils;
import com.blazemeter.jmeter.rte.core.Protocol;
import com.blazemeter.jmeter.rte.core.RteSampleResultBuilder;
import com.blazemeter.jmeter.rte.core.TerminalType;
import com.blazemeter.jmeter.rte.core.ssl.SSLType;
import com.blazemeter.jmeter.rte.sampler.Action;
import java.awt.Dimension;
import java.util.Arrays;
import java.util.Collection;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.threads.JMeterVariables;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class RTEExtractorTest {

  public static final String POSITION_VAR_ROW = "positionVar_ROW";
  public static final String POSITION_VAR_COLUMN = "positionVar_COLUMN";
  public static final String COLOR_VAR_TEXT = "positionVar";
  public static final String VARIABLE_PREFIX = "positionVar";
  private static final String SEGMENTS =
      "{\"range\":\"[(2,25)-(2,30)]\",\"editable\":true, \"color\":\"#FF0001\"}," +
      "{\"range\":\"[(4,25)-(4,30)]\",\"editable\":true, \"color\":\"#FF0002\"}," +
      "{\"range\":\"[(6,25)-(6,30)]\",\"editable\":true, \"color\":\"#FF0003\"}," +
      "{\"range\":\"[(7,25)-(7,30)]\",\"editable\":true}";

  private static final String SEGMENT_HEADER = "Segments: [" + SEGMENTS + "]";
  private static final String RESPONSE_HEADERS = "Input-inhibited: true\n" +
      "Cursor-position: (1,1)" + '\n' +
      "Field-positions: [(2,25)-(2,30)], [(4,25)-(4,30)], [(6,25)-(6,30)]\n" +
          SEGMENT_HEADER;
  @Rule
  public JUnitSoftAssertions softly = new JUnitSoftAssertions();
  @Parameter()
  public String variablePrefix;
  @Parameter(1)
  public ExtractionType extractionAction;
  @Parameter(2)
  public String baseRow;
  @Parameter(3)
  public String baseColumn;
  @Parameter(4)
  public String offset;
  @Parameter(5)
  public String extractedRow;
  @Parameter(6)
  public String extractedColumn;
  @Parameter(7)
  public String extractedColor;

  private JMeterContext context;
  private RTEExtractor rteExtractor;

  @BeforeClass
  public static void setupClass() {
    JMeterTestUtils.setupJmeterEnv();
    JMeterContext context = JMeterContextService.getContext();
    context.setPreviousResult(getCustomizedResult());
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

  @Parameters
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{
        {VARIABLE_PREFIX, ExtractionType.NEXT_FIELD_POSITION, "100", "200", "1", null, null, null},
        {VARIABLE_PREFIX, ExtractionType.NEXT_FIELD_POSITION, "2", "25", "10", null, null, null},
        {VARIABLE_PREFIX, ExtractionType.NEXT_FIELD_POSITION, "2", "25", "0", "2", "25", null},
        {VARIABLE_PREFIX, ExtractionType.NEXT_FIELD_POSITION, "2", "24", "1", "2", "25", null},
        {VARIABLE_PREFIX, ExtractionType.NEXT_FIELD_POSITION, "2", "25", "1", "4", "25", null},
        {VARIABLE_PREFIX, ExtractionType.NEXT_FIELD_POSITION, "2", "26", "1", "4", "25", null},
        {VARIABLE_PREFIX, ExtractionType.NEXT_FIELD_POSITION, "2", "24", "2", "4", "25", null},
        {VARIABLE_PREFIX, ExtractionType.NEXT_FIELD_POSITION, "2", "25", "2", "6", "25", null},
        {VARIABLE_PREFIX, ExtractionType.NEXT_FIELD_POSITION, "4", "25", "3", null, null, null},
        {VARIABLE_PREFIX, ExtractionType.NEXT_FIELD_POSITION, "2", "24", "-1", null, null, null},
        {VARIABLE_PREFIX, ExtractionType.NEXT_FIELD_POSITION, "2", "30", "-1", null, null, null},
        {VARIABLE_PREFIX, ExtractionType.NEXT_FIELD_POSITION, "2", "31", "-1", "2", "25", null},
        {VARIABLE_PREFIX, ExtractionType.NEXT_FIELD_POSITION, "2", "31", "-2", null, null, null},
        {VARIABLE_PREFIX, ExtractionType.NEXT_FIELD_POSITION, "4", "30", "-2", null, null, null},
        {VARIABLE_PREFIX, ExtractionType.NEXT_FIELD_POSITION, "4", "31", "-2", "2", "25", null},
        {"", ExtractionType.NEXT_FIELD_POSITION, "2", "25", "0", null, null, null},
        {VARIABLE_PREFIX, ExtractionType.CURSOR_POSITION, null, null, null, "1", "1", null},
        {VARIABLE_PREFIX, ExtractionType.COLOR, "2", "25", null, null, null, "#ff0001"},
        {VARIABLE_PREFIX, ExtractionType.COLOR, "7", "25", null, null, null, null},
        {VARIABLE_PREFIX, ExtractionType.COLOR, "2", "30", null, null, null, "#ff0001"},
    });
  }

  @Before
  public void setup() {
    context = JMeterContextService.getContext();
    rteExtractor = new RTEExtractor();
    rteExtractor.setContext(context);
    context.setVariables(new JMeterVariables());
    setUpExtractor();
  }

  @Test
  public void shouldExtractAttributesFromFieldsForCases() {
    rteExtractor.process();
    JMeterVariables vars = context.getVariables();
    softly.assertThat(vars.get(POSITION_VAR_ROW)).isEqualTo(extractedRow);
    softly.assertThat(vars.get(POSITION_VAR_COLUMN)).isEqualTo(extractedColumn);
    softly.assertThat(vars.get(COLOR_VAR_TEXT)).isEqualTo(extractedColor);
  }

  private void setUpExtractor() {
    rteExtractor.setExtractionType(extractionAction);
    rteExtractor.setVariablePrefix(variablePrefix);

    if (!extractionAction.equals(ExtractionType.CURSOR_POSITION)) {
      rteExtractor.setRow(baseRow);
      rteExtractor.setColumn(baseColumn);
    }

    if(extractionAction.equals(ExtractionType.NEXT_FIELD_POSITION)){
      rteExtractor.setOffset(offset);
    }
  }
}
