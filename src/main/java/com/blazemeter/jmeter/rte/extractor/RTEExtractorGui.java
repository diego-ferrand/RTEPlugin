package com.blazemeter.jmeter.rte.extractor;

import com.blazemeter.jmeter.rte.sampler.gui.BlazemeterLabsLogo;
import java.awt.BorderLayout;
import org.apache.jmeter.processor.gui.AbstractPostProcessorGui;
import org.apache.jmeter.testelement.TestElement;

public class RTEExtractorGui extends AbstractPostProcessorGui {

  private final RTEExtractorPanel extractorPanel;

  public RTEExtractorGui() {
    extractorPanel = new RTEExtractorPanel();
    setLayout(new BorderLayout(0, 5));
    setBorder(makeBorder());
    add(makeTitlePanel(), BorderLayout.NORTH);
    add(extractorPanel, BorderLayout.CENTER);
    add(new BlazemeterLabsLogo(), BorderLayout.AFTER_LAST_LINE);
  }

  @Override
  public String getStaticLabel() {
    return "bzm - RTE Extractor";
  }

  @Override
  public String getLabelResource() {
    return null;
  }

  @Override
  public void configure(TestElement testElement) {
    super.configure(testElement);
    if (testElement instanceof RTEExtractor) {
      RTEExtractor rteExtractor = (RTEExtractor) testElement;
      extractorPanel.setVariablePrefix(rteExtractor.getVariablePrefix());
      extractorPanel.setRow(String.valueOf(rteExtractor.getRow()));
      extractorPanel.setColumn(String.valueOf(rteExtractor.getColumn()));
      extractorPanel.setOffset(String.valueOf(rteExtractor.getOffset()));
      extractorPanel.setExtractionType(rteExtractor.getExtractionType());
    }
  }

  @Override
  public TestElement createTestElement() {
    RTEExtractor rteExtractor = new RTEExtractor();
    configureTestElement(rteExtractor);
    return rteExtractor;
  }

  @Override
  public void modifyTestElement(TestElement testElement) {
    configureTestElement(testElement);
    if (testElement instanceof RTEExtractor) {
      RTEExtractor rteExtractor = (RTEExtractor) testElement;
      rteExtractor.setVariablePrefix(extractorPanel.getVariablePrefix());
      rteExtractor.setRow(extractorPanel.getRow());
      rteExtractor.setColumn(extractorPanel.getColumn());
      rteExtractor.setOffset(extractorPanel.getOffset());
      rteExtractor.setExtractionType(extractorPanel.getExtractionType());
    }

  }

  @Override
  public void clearGui() {
    super.clearGui();
    extractorPanel.setExtractionType(ExtractionType.NEXT_FIELD_POSITION);
    extractorPanel.setVariablePrefix("");
    extractorPanel.setRow("");
    extractorPanel.setColumn("");
    extractorPanel.setOffset("");
  }

}
