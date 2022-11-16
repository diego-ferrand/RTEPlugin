package com.blazemeter.jmeter.rte.core.assertions;

import static org.assertj.swing.fixture.Containers.showInFrame;
import static org.assertj.core.api.Assertions.assertThat;

import com.blazemeter.jmeter.rte.core.ColorUtils;
import com.bytezone.dm3270.attributes.ColorAttribute;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jorphan.gui.JLabeledChoice;
import org.assertj.core.api.JUnitSoftAssertions;
import org.assertj.swing.fixture.FrameFixture;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RTEAssertionPanelIT {

  private final String hexStringFormat = "^#(([0-9a-fA-F]{2}){3}|([0-9a-fA-F]){3})$";
  private final String rgbStringFormat = "rgb *\\( *([0-9]+), *([0-9]+), *([0-9]+)*\\)";

  @Rule
  public JUnitSoftAssertions softly = new JUnitSoftAssertions();
  private FrameFixture frame;
  public JMeterContext context;

  @Before
  public void setup() {
    RTEAssertionPanel rteAssertionPanel = new RTEAssertionPanel();
    JPanel container = new JPanel();
    container.setName("container");
    container.add(rteAssertionPanel);

    frame = showInFrame(container);
    frame.resizeTo(rteAssertionPanel.getSize());
    frame.target().pack();
  }

  @After
  public void tearDown() {
    frame.target().removeAll();
    frame.cleanUp();
  }

  @Test
  public void shouldLoadAllowedColors() {
    List<String> expected = new ArrayList<>();
    expected.add("Custom");
    expected.addAll(Arrays.asList(ColorAttribute.ALLOWED_COLORS_NAMES));

    assertThat(getColorChooser().getItems()).isEqualTo(expected.toArray());
  }

  private JLabeledChoice getColorChooser() {
    return frame.robot().finder().findByName("colorChooser", JLabeledChoice.class);
  }

  @Test
  public void shouldPaintTheRightColorWhenAColorIsSpecified() {
    getColorChooser().setText("#ff5733");
    String displayedColor = ColorUtils.getHex(getDisplayColorPanel().getBackground());

    assertThat("#ff5733").isEqualTo(displayedColor);
  }

  private JPanel getDisplayColorPanel() {
    return frame.robot().finder().findByName("displayColorPanel", JPanel.class);
  }

  @Test
  public void shouldValidateHexFormatWhenHexValueIsAddedWithValidFormat() {
    getColorChooser().setText("#FF5733");
    cursorFocusChange();

    softly.assertThat(getColorChooser().getText()).matches(hexStringFormat);
    softly.assertThat(getErrorLabel().isVisible()).isFalse();
  }

  private void cursorFocusChange() {
    frame.textBox("fieldRow").click();
  }

  private JLabel getErrorLabel() {
    return frame.robot().finder().findByName("errorLabel", JLabel.class, false);
  }

  @Test
  public void shouldValidateHexFormatWhenHexValueIsAddedWithInvalidFormat() {
    getColorChooser().setText("#gggggg");
    cursorFocusChange();

    softly.assertThat(getColorChooser().getText()).doesNotMatch(hexStringFormat);
    softly.assertThat(getErrorLabel().isVisible()).isTrue();
  }

  @Test
  public void shouldValidateRGBFormatWhenRGBValueIsAddedWithValidFormat() {
    getColorChooser().setText(getRandomRGBColor());
    cursorFocusChange();

    softly.assertThat(getColorChooser().getText()).matches(rgbStringFormat);
    softly.assertThat(getErrorLabel().isVisible()).isFalse();
  }

  private String getRandomRGBColor() {
    Random rand = new Random();

    int r = rand.nextInt(255);
    int g = rand.nextInt(255);
    int b = rand.nextInt(255);

    return "rgb(" +r +", "+g +", "+b+")";
  }

  @Test
  public void shouldValidateRGBFormatWhenRGBValueIsAddedWithInvalidFormat() {
    getColorChooser().setText("invalid" + getRandomRGBColor());
    cursorFocusChange();

    softly.assertThat(getColorChooser().getText()).doesNotMatch(rgbStringFormat);
    softly.assertThat(getErrorLabel().isVisible()).isTrue();
  }

  @Test
  public void shouldSelectAPreloadedOptionWhenAColorNameIsAddedAndIsOneOfTheAllowed () {
    Random randomNumber = new Random();
    int rand = randomNumber.nextInt(8);

    getColorChooser().setText(ColorAttribute.ALLOWED_COLORS_NAMES[rand]);
    String inputColor = ColorUtils.getHex(ColorAttribute.ALLOWED_COLORS[rand]);

    softly.assertThat(ColorUtils.getHex(getDisplayColorPanel().getBackground())).isEqualTo(inputColor);
    softly.assertThat(getErrorLabel().isVisible()).isFalse();
  }

  @Test
  public void shouldShowInvalidLabelWhenAColorNameIsAddedAndIsNotOneOfTheAllowed () {
    getColorChooser().setText("purple");
    cursorFocusChange();

    Color expectedPanelColor = frame.panel("rteAssertionPanel").target().getBackground();

    softly.assertThat(getDisplayColorPanel().getBackground()).isEqualTo(expectedPanelColor);
    softly.assertThat(getErrorLabel().isVisible()).isTrue();
  }

}
