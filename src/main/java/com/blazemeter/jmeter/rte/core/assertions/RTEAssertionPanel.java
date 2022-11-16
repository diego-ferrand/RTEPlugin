package com.blazemeter.jmeter.rte.core.assertions;

import com.blazemeter.jmeter.rte.core.ColorUtils;
import com.blazemeter.jmeter.rte.sampler.gui.SwingUtils;
import com.bytezone.dm3270.attributes.ColorAttribute;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.LayoutStyle;
import org.apache.jorphan.gui.JLabeledChoice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RTEAssertionPanel extends JPanel {
  private static final Logger LOG = LoggerFactory.getLogger(RTEAssertionPanel.class);

  private static final int PREFERRED_SIZE = GroupLayout.PREFERRED_SIZE;
  private static final int DEFAULT_SIZE = GroupLayout.DEFAULT_SIZE;

  private JLabeledChoice colorChooser;
  private String hexColor;

  private final JPanel displayColorPanel = SwingUtils.createComponent("displayColorPanel",
      new JPanel());
  private final JTextField row = SwingUtils.createComponent("fieldRow", new JTextField());
  private final JTextField column = SwingUtils.createComponent("fieldColumn", new JTextField());
  private final JLabel rowLabel = new JLabel("Row: ");
  private final JLabel columnLabel = new JLabel("Column: ");
  private final JLabel exampleLabel = new JLabel();
  private final JLabel errorLabel = SwingUtils.createComponent("errorLabel",
      new JLabel("Invalid color"));

  public RTEAssertionPanel() {
    GroupLayout layout = new GroupLayout(this);
    setLayout(layout);
    setName("rteAssertionPanel");

    JPanel attributePanel = buildAttributePanel();
    JPanel positionPanel = buildPositionPanel();

    layout.setHorizontalGroup(layout.createParallelGroup()
        .addComponent(attributePanel)
        .addComponent(positionPanel)
    );

    layout.setVerticalGroup(layout.createSequentialGroup()
        .addComponent(attributePanel, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
        .addComponent(positionPanel, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
    );
  }

  private JPanel buildAttributePanel() {
    setupColorChooser();
    setupErrorLabel();
    setupExampleLabel();
    displayColorPanel.setPreferredSize(new Dimension(24, 24));

    JPanel colorPickPanel = buildColorPickPanel();

    JPanel panel = SwingUtils.createComponent("attributePanel", new JPanel());
    panel.setBorder(BorderFactory.createTitledBorder("Attribute"));
    panel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
    panel.add(colorPickPanel);

    return panel;
  }

  private void setupErrorLabel() {
    errorLabel.setVisible(false);
    errorLabel.setForeground(Color.RED);
  }

  private void setupExampleLabel() {
    exampleLabel.setText("E.g.: red is #ff0000");
    exampleLabel.setFont(exampleLabel.getFont().deriveFont(Font.ITALIC, 11));
  }

  private JPanel buildColorPickPanel() {
    JPanel panel = SwingUtils.createComponent("colorPickPanel", new JPanel());

    GroupLayout layout = new GroupLayout(panel);
    panel.setLayout(layout);
    layout.setAutoCreateGaps(true);
    layout.setAutoCreateContainerGaps(true);

    JLabel colorLabel = SwingUtils.createComponent("colorLabel", new JLabel("Color:"));

    layout.setHorizontalGroup(layout.createSequentialGroup()
        .addComponent(colorLabel)
        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(colorChooser)
            .addComponent(errorLabel)
            .addComponent(exampleLabel)
        )
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(displayColorPanel)
        )
    );

    layout.setVerticalGroup(layout.createSequentialGroup()
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
            .addComponent(colorLabel)
            .addComponent(colorChooser)
            .addComponent(displayColorPanel, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
        )
        .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(errorLabel)
                .addComponent(exampleLabel)
            )
        )
    );

    return panel;
  }

  private void setupColorChooser() {
    colorChooser = SwingUtils.createComponent("colorChooser",
        new JLabeledChoice("", true));
    colorChooser.setPreferredSize(new Dimension(100, 25));
    colorChooser.setToolTipText("This field is editable");

    String customItem = "Custom";
    colorChooser.addValue(customItem);
    colorChooser.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
    for (String color : ColorAttribute.ALLOWED_COLORS_NAMES) {
      colorChooser.addValue(color);
    }

    colorChooser.addChangeListener(e -> {
      errorLabel.setVisible(false);
      displayColorPanel.setBackground(this.getBackground());
      JLabeledChoice source = (JLabeledChoice) e.getSource();
      String text = source.getText();
      if (text.isEmpty()) {
        return;
      }

      if (customItem.equals(text)) {
        source.setText("");
        return;
      }

      if (text.startsWith("${") && text.endsWith("}")) {
        hexColor = text;
        return;
      }

      Color color = ColorUtils.parseColor(text);
      if (color != null) {
        hexColor = ColorUtils.getHex(color);
        displayColorPanel.setBackground(color);
      } else {
        hexColor = text;
        errorLabel.setVisible(true);
        LOG.error("The expected color {} couldn't be parsed.", hexColor);
      }
    });
  }

  private JPanel buildPositionPanel() {
    JPanel panel = SwingUtils.createComponent("positionPanel", new JPanel());
    panel.setBorder(BorderFactory.createTitledBorder("Position"));
    GroupLayout layout = new GroupLayout(panel);
    panel.setLayout(layout);
    layout.setAutoCreateContainerGaps(true);

    layout.setHorizontalGroup(layout.createSequentialGroup()
        .addGroup(layout.createParallelGroup()
            .addComponent(rowLabel)
            .addComponent(columnLabel)
        )
        .addGroup(layout.createParallelGroup()
            .addComponent(row)
            .addComponent(column)
        )
    );

    layout.setVerticalGroup(layout.createSequentialGroup()
        .addGroup(layout.createParallelGroup()
            .addComponent(rowLabel, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
            .addComponent(row, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
        )
        .addGroup(layout.createParallelGroup()
            .addComponent(columnLabel, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
            .addComponent(column, PREFERRED_SIZE, DEFAULT_SIZE, PREFERRED_SIZE)
        )
    );

    return panel;
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

  public String getColorValue() {
    return hexColor;
  }

  public void setColorLabel(String colorLabel) {
    this.colorChooser.setText(colorLabel);
  }

  public void setHexColor(String hexColor) {
    this.hexColor = hexColor;
  }

  public void updateParsedSelectedColor() {
    if (this.hexColor == null || this.hexColor.isEmpty()) {
      return;
    }

    String hexValue = this.hexColor;
    String[] colors = colorChooser.getItems();
    for (int i = 1; i < colors.length; i++) {
      if (hexValue.equals(ColorUtils.getHex(ColorAttribute.ALLOWED_COLORS[i - 1]))) {
        colorChooser.setSelectedIndex(i);
        return;
      }
    }

    colorChooser.setText(hexValue);
  }
}
