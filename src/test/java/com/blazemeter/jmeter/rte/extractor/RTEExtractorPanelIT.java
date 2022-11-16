package com.blazemeter.jmeter.rte.extractor;

import static org.assertj.swing.fixture.Containers.showInFrame;

import java.awt.Dimension;
import org.assertj.core.api.JUnitSoftAssertions;
import org.assertj.swing.core.ComponentLookupScope;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RTEExtractorPanelIT {

  private static final String ROW_FIELD = "fieldRow";
  private static final String COLUMN_FIELD = "fieldColumn";
  private static final String OFFSET_FIELD = "fieldOffset";

  @Rule
  public JUnitSoftAssertions softly = new JUnitSoftAssertions();
  private FrameFixture frame;

  @Before
  public void setup() {
    RTEExtractorPanel panel = new RTEExtractorPanel();
    frame = showInFrame(panel);
    frame.resizeTo(new Dimension(panel.getWidth() * 2, panel.getWidth() * 2));
    //Allows the frame to find elements that are not visible
    frame.robot().settings().componentLookupScope(ComponentLookupScope.ALL);
  }

  @After
  public void tearDown() {
    frame.cleanUp();
  }

  @Test
  public void shouldDisplayRequiredFieldsWhenColorExtractorSelected() {
    selectColor();
    softly.assertThat(frame.textBox(ROW_FIELD).target().isVisible()).isTrue();
    softly.assertThat(frame.textBox(COLUMN_FIELD).target().isVisible()).isTrue();
  }

  private void selectColor() {
    frame.radioButton("colorExtractorRadioButton").click();
  }

  @Test
  public void shouldDisplayRequiredFieldsWhenNextFieldPositionExtractorSelected() {
    selectNextFieldPosition();
    softly.assertThat(frame.textBox(ROW_FIELD).target().isVisible()).isTrue();
    softly.assertThat(frame.textBox(COLUMN_FIELD).target().isVisible()).isTrue();
    softly.assertThat(frame.textBox(OFFSET_FIELD).target().isVisible()).isTrue();
  }

  private void selectNextFieldPosition() {
    selectPositionsExtractor();
    frame.radioButton("nextFieldPositionRadioButton").click();
  }

  @Test
  public void shouldHideOffsetWhenColorExtraction() {
    selectColor();
    frame.textBox(OFFSET_FIELD).requireNotVisible();
  }

  @Test
  public void shouldHideOffsetAndCoordsPanelWhenCursorSelected() {
    selectColor();
    selectCursor();
    softly.assertThat(frame.textBox(OFFSET_FIELD).target().isShowing()).isFalse();
    frame.panel("coordinatesPanel").requireNotVisible();
  }

  @Test
  public void shouldSelectPositionsAndDeselectColor() {
    selectColor();
    selectPositionsExtractor();
    frame.radioButton("colorExtractorRadioButton").requireNotSelected();
  }

  private void selectCursor() {
    selectPositionsExtractor();
    frame.radioButton("cursorPositionRadioButton").click();
  }

  private void selectPositionsExtractor() {
    frame.radioButton("positionExtractorRadioButton").click();
  }
}
