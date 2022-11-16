package com.blazemeter.jmeter.rte.extractor;

import static javax.swing.GroupLayout.DEFAULT_SIZE;

import com.blazemeter.jmeter.rte.extractor.ExtractionType.ExtractionAction;
import com.blazemeter.jmeter.rte.sampler.gui.SwingUtils;
import java.awt.Component;
import java.awt.event.ItemEvent;
import java.util.Arrays;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.GroupLayout.ParallelGroup;
import javax.swing.GroupLayout.SequentialGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

public class RTEExtractorPanel extends JPanel {

  private static final int PREFERRED_SIZE = GroupLayout.PREFERRED_SIZE;
  private final JTextField row = SwingUtils.createComponent("fieldRow", new JTextField());
  private final JTextField column = SwingUtils.createComponent("fieldColumn", new JTextField());
  private final JTextField offset = SwingUtils.createComponent("fieldOffset", new JTextField());
  private final JTextField variablePrefix = SwingUtils.createComponent("variablePrefix",
      new JTextField());
  private final ButtonGroup typesGroup = new ButtonGroup();
  private final ButtonGroup positionGroup = new ButtonGroup();
  private final JLabel rowLabel = new JLabel("Row:");
  private final JLabel columnLabel = new JLabel("Column:");
  private final JLabel offsetLabel = new JLabel("Offset:");

  private final JPanel coordinatesPanel;
  private final JPanel positionsPanel;

  private final JRadioButton positionExtractor = SwingUtils.createComponent(
      "positionExtractorRadioButton", new JRadioButton(ExtractionAction.POSITIONS.getLabel()));
  private final JRadioButton attributeExtractor = SwingUtils.createComponent(
      "colorExtractorRadioButton", new JRadioButton(ExtractionType.COLOR.getLabel()));

  private final JRadioButton cursorPosition = SwingUtils
      .createComponent("cursorPositionRadioButton",
          new JRadioButton(ExtractionType.CURSOR_POSITION.getLabel()));
  private final JRadioButton nextFieldPosition = SwingUtils
      .createComponent("nextFieldPositionRadioButton",
          new JRadioButton(ExtractionType.NEXT_FIELD_POSITION.getLabel()));

  public RTEExtractorPanel() {
    GroupLayout layout = new GroupLayout(this);
    layout.setAutoCreateGaps(true);
    setLayout(layout);

    coordinatesPanel = buildPanelWithComponentsInline("coordinatesPanel", rowLabel, row,
        columnLabel, column, offsetLabel, offset);

    coordinatesPanel.setBorder(BorderFactory.createTitledBorder("Position"));

    positionsPanel = buildPositionsPanel();
    JPanel positionsAndCoordsPanelWithBorder = buildPanelWithComponentsInColumn(positionsPanel,
        coordinatesPanel);
    positionsAndCoordsPanelWithBorder.setBorder(BorderFactory.createTitledBorder(""));

    JPanel prefixTypePanel = buildPrefixTypePanel();

    layout.setHorizontalGroup(layout.createParallelGroup()
        .addComponent(prefixTypePanel)
        .addComponent(positionsAndCoordsPanelWithBorder)
    );

    layout.setVerticalGroup(layout.createSequentialGroup()
        .addGap(10)
        .addComponent(prefixTypePanel, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
        .addComponent(positionsAndCoordsPanelWithBorder)
    );

    setOffsetVisible(true);
    positionExtractor.setSelected(true);
    cursorPosition.setSelected(true);
    clearPositionPanel();
  }

  private JPanel buildPanelWithComponentsInline(String name, Component... components) {
    JPanel panel = SwingUtils.createComponent(name, new JPanel());
    GroupLayout layout = new GroupLayout(panel);
    panel.setLayout(layout);
    layout.setAutoCreateContainerGaps(true);
    layout.setAutoCreateGaps(true);
    SequentialGroup sequentialGroup = layout.createSequentialGroup();
    Arrays.stream(components).forEach(sequentialGroup::addComponent);
    layout.setHorizontalGroup(sequentialGroup);
    ParallelGroup parallelGroup = layout.createParallelGroup();
    Arrays.stream(components).forEach(component -> {
      if (component instanceof JLabel) {
        parallelGroup.addComponent(component, Alignment.CENTER, PREFERRED_SIZE, DEFAULT_SIZE,
            PREFERRED_SIZE);
      }
      parallelGroup.addComponent(component, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE);
    });
    layout.setVerticalGroup(parallelGroup);
    return panel;
  }

  private JPanel buildPositionsPanel() {
    JPanel panel = SwingUtils.createComponent("positionPanels", new JPanel());
    panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
    panel.add(cursorPosition);
    panel.add(nextFieldPosition);
    positionGroup.add(cursorPosition);
    positionGroup.add(nextFieldPosition);
    cursorPosition.addItemListener(l -> {
      if (l.getStateChange() == ItemEvent.SELECTED) {
        updateExtractionType(ExtractionAction.POSITIONS);
      }
    });
    nextFieldPosition.addItemListener(l -> {
      if (l.getStateChange() == ItemEvent.SELECTED) {
        updateExtractionType(ExtractionAction.POSITIONS);
      }
    });
    return panel;
  }

  private void setOffsetVisible(boolean isVisible) {
    offsetLabel.setVisible(isVisible);
    offset.setVisible(isVisible);
  }

  private JPanel buildPanelWithComponentsInColumn(Component... components) {
    JPanel panel = new JPanel();
    GroupLayout layout = new GroupLayout(panel);
    panel.setLayout(layout);
    layout.setAutoCreateContainerGaps(true);
    layout.setAutoCreateGaps(true);
    ParallelGroup parallelGroup = layout.createParallelGroup();
    Arrays.stream(components).forEach(parallelGroup::addComponent);
    layout.setHorizontalGroup(parallelGroup);
    SequentialGroup sequentialGroup = layout.createSequentialGroup();
    Arrays.stream(components)
        .forEach(component -> sequentialGroup.addComponent(component, PREFERRED_SIZE,
            DEFAULT_SIZE, PREFERRED_SIZE));
    layout.setVerticalGroup(sequentialGroup);
    return panel;
  }

  private JPanel buildPrefixTypePanel() {
    JPanel panel = SwingUtils.createComponent("typePrefixPanel", new JPanel());
    GroupLayout layout = new GroupLayout(panel);
    panel.setLayout(layout);

    JPanel typePanel = buildTypePanel();
    JPanel variablePrefixPanel = buildPanelWithComponentsInline("variablePrefixPanel",
        new JLabel("Variable's prefix: "), variablePrefix);
    layout.setHorizontalGroup(layout.createParallelGroup()
        .addComponent(variablePrefixPanel)
        .addComponent(typePanel));

    layout.setVerticalGroup(layout.createSequentialGroup()
        .addGroup(layout.createParallelGroup()
            .addComponent(variablePrefixPanel))
        .addGroup(layout.createParallelGroup()
            .addComponent(typePanel)));

    positionExtractor.addItemListener(e -> {
      if (e.getStateChange() == ItemEvent.SELECTED) {
        updateExtractionType(ExtractionAction.POSITIONS);
      }
    });

    attributeExtractor.addItemListener(e -> {
      if (e.getStateChange() == ItemEvent.SELECTED) {
        updateExtractionType(ExtractionAction.ATTRIBUTES);
      }
    });
    return panel;
  }

  private JPanel buildTypePanel() {
    JPanel typePanel = buildPanelWithComponentsInline("typePanel", positionExtractor,
        attributeExtractor);
    typePanel.setBorder(BorderFactory.createTitledBorder("Type"));
    typesGroup.add(positionExtractor);
    typesGroup.add(attributeExtractor);
    positionExtractor.setActionCommand(ExtractionAction.POSITIONS.name());
    attributeExtractor.setActionCommand(ExtractionAction.ATTRIBUTES.name());
    positionExtractor.setSelected(true);
    return typePanel;
  }

  private void updateExtractionType(ExtractionAction type) {
    switch (type) {
      case POSITIONS:
        setOffsetVisible(nextFieldPosition.isVisible());
        coordinatesPanel.setVisible(nextFieldPosition.isSelected());
        positionsPanel.setVisible(true);
        ((JPanel) coordinatesPanel.getParent()).setBorder(BorderFactory.createTitledBorder(""));
        break;
      case ATTRIBUTES:
        ((JPanel) coordinatesPanel.getParent()).setBorder(BorderFactory.createEmptyBorder());
        positionsPanel.setVisible(false);
        coordinatesPanel.setVisible(true);
        setOffsetVisible(false);
        break;
      default:
        throw new UnsupportedOperationException("Option [" + type + "] is not supported");
    }
    validate();
    repaint();
  }

  public ExtractionType getExtractionType() {
    ExtractionAction mode = ExtractionAction.valueOf(typesGroup.getSelection().getActionCommand());
    if (mode.equals(ExtractionAction.POSITIONS)) {
      ButtonModel selection = positionGroup.getSelection();
      return selection != null && selection.getActionCommand() != null
          ? ExtractionType.from(selection.getActionCommand())
          : ExtractionType.NEXT_FIELD_POSITION;
    }
    return ExtractionType.COLOR;
  }

  public void setExtractionType(ExtractionType extractionType) {
    boolean isPositionExtraction = extractionType.extractionAction.equals(
        ExtractionAction.POSITIONS);
    positionExtractor.setSelected(isPositionExtraction);
    attributeExtractor.setSelected(!isPositionExtraction);
  }

  public String getRow() {
    return row.getText();
  }

  public void setRow(String text) {
    row.setText(text);
  }

  public String getColumn() {
    return column.getText();
  }

  public void setColumn(String column) {
    this.column.setText(column);
  }

  public String getOffset() {
    return offset.getText();
  }

  public void setOffset(String offset) {
    this.offset.setText(offset);
  }

  public String getVariablePrefix() {
    return variablePrefix.getText();
  }

  public void setVariablePrefix(String prefix) {
    variablePrefix.setText(prefix);
  }

  private void clearPositionPanel() {
    SwingUtils.clearRecursively(coordinatesPanel);
  }
}
