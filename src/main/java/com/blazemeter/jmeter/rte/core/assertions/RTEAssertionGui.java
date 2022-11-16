package com.blazemeter.jmeter.rte.core.assertions;

import com.blazemeter.jmeter.rte.sampler.gui.BlazemeterLabsLogo;
import java.awt.BorderLayout;
import org.apache.jmeter.assertions.gui.AbstractAssertionGui;
import org.apache.jmeter.testelement.TestElement;

public class RTEAssertionGui extends AbstractAssertionGui {

  private final RTEAssertionPanel assertionPanel;

  public RTEAssertionGui() {
    assertionPanel = new RTEAssertionPanel();
    setLayout(new BorderLayout(0, 5));
    setBorder(makeBorder());
    add(makeTitlePanel(), BorderLayout.NORTH);
    add(assertionPanel, BorderLayout.CENTER);
    add(new BlazemeterLabsLogo(), BorderLayout.AFTER_LAST_LINE);
  }

  @Override
  public String getStaticLabel() {
    return "bzm - RTE Assertion";
  }

  @Override
  public String getLabelResource() {
    return null;
  }

  @Override
  public void configure(TestElement testElement) {
    super.configure(testElement);

    if (testElement instanceof RTEAssertion) {
      RTEAssertion rteAssertion = (RTEAssertion) testElement;

      assertionPanel.setRow(String.valueOf(rteAssertion.getRow()));
      assertionPanel.setColumn(String.valueOf(rteAssertion.getColumn()));
      assertionPanel.setHexColor(rteAssertion.getColor());
      assertionPanel.updateParsedSelectedColor();
    }
  }

  @Override
  public TestElement createTestElement() {
    RTEAssertion rteAssertion = new RTEAssertion();
    modifyTestElement(rteAssertion);
    return rteAssertion;
  }

  @Override
  public void modifyTestElement(TestElement testElement) {
    configureTestElement(testElement);

    if (testElement instanceof RTEAssertion) {
      RTEAssertion rteAssertion = (RTEAssertion) testElement;

      rteAssertion.setRow(assertionPanel.getRow());
      rteAssertion.setColumn(assertionPanel.getColumn());
      rteAssertion.setColor(assertionPanel.getColorValue());
    }
  }

  @Override
  public void clearGui() {
    super.clearGui();
    assertionPanel.setRow("");
    assertionPanel.setColumn("");
    assertionPanel.setColorLabel("");
  }
}
