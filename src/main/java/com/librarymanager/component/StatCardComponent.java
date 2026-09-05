package com.librarymanager.component;

import com.librarymanager.util.AnimationUtil;
import com.librarymanager.util.IconUtil;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;

/**
 * Modern dashboard statistics card displaying a metric, icon, and animated value.
 */
public class StatCardComponent extends VBox {
    private final Label valueLabel;
    private final Label subtextLabel;

    public StatCardComponent(String title, String initialValue, String subtext, IconUtil.IconType iconType, String accentClass) {
        getStyleClass().addAll("stat-card", accentClass != null ? accentClass : "stat-accent-indigo");
        setNodeOrientation(com.librarymanager.util.I18n.isRTL() ? NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT);
        setPadding(new Insets(18, 20, 18, 20));
        setSpacing(10);
        setAlignment(com.librarymanager.util.I18n.isRTL() ? Pos.TOP_RIGHT : Pos.TOP_LEFT);
        HBox.setHgrow(this, Priority.ALWAYS);
        setMinWidth(180);

        // Header with Title and Icon
        HBox topRow = new HBox();
        topRow.setAlignment(com.librarymanager.util.I18n.isRTL() ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        topRow.setSpacing(10);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("stat-title");
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        StackPane iconContainer = new StackPane();
        iconContainer.getStyleClass().add("stat-icon-container");
        iconContainer.setMinSize(36, 36);
        iconContainer.setMaxSize(36, 36);
        SVGPath icon = IconUtil.createIcon(iconType, 16);
        icon.getStyleClass().add("stat-icon");
        iconContainer.getChildren().add(icon);

        topRow.getChildren().addAll(titleLabel, iconContainer);

        // Value Label
        valueLabel = new Label(initialValue);
        valueLabel.getStyleClass().add("stat-value");

        // Subtext
        subtextLabel = new Label(subtext);
        subtextLabel.getStyleClass().add("stat-subtext");

        getChildren().addAll(topRow, valueLabel, subtextLabel);

        AnimationUtil.addCardHover(this);
    }

    public void updateNumericValue(int targetValue, String subtext) {
        AnimationUtil.animateNumber(valueLabel, targetValue);
        if (subtext != null) {
            subtextLabel.setText(subtext);
        }
    }

    public void updateTextValue(String value, String subtext) {
        valueLabel.setText(value);
        if (subtext != null) {
            subtextLabel.setText(subtext);
        }
    }
}
