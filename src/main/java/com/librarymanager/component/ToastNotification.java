package com.librarymanager.component;

import com.librarymanager.util.AnimationUtil;
import com.librarymanager.util.IconUtil;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

/**
 * Modern non-intrusive floating toast notification that slides/fades in and disappears automatically.
 */
public class ToastNotification extends HBox {

    public enum ToastType {
        SUCCESS("toast-success", IconUtil.IconType.CHECK),
        INFO("toast-info", IconUtil.IconType.INFO),
        WARNING("toast-warning", IconUtil.IconType.INFO),
        ERROR("toast-error", IconUtil.IconType.CLOSE);

        private final String styleClass;
        private final IconUtil.IconType iconType;

        ToastType(String styleClass, IconUtil.IconType iconType) {
            this.styleClass = styleClass;
            this.iconType = iconType;
        }

        public String getStyleClass() {
            return styleClass;
        }

        public IconUtil.IconType getIconType() {
            return iconType;
        }
    }

    public static void show(Pane container, String message, ToastType type) {
        if (container == null || message == null) return;

        ToastNotification toast = new ToastNotification(message, type);
        container.getChildren().add(toast);

        // Slide in and fade in
        AnimationUtil.scaleIn(toast, Duration.millis(200));

        // Auto dismiss after 3 seconds
        Timeline dismissTimeline = new Timeline(new KeyFrame(Duration.millis(3200), e -> {
            AnimationUtil.fadeOut(toast, Duration.millis(250), () -> {
                container.getChildren().remove(toast);
            });
        }));
        dismissTimeline.play();
    }

    public ToastNotification(String message, ToastType type) {
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(12);
        setPadding(new Insets(12, 18, 12, 16));
        setMaxWidth(380);
        setMinWidth(260);
        setNodeOrientation(com.librarymanager.util.I18n.isRTL() ? NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT);

        getStyleClass().addAll("toast-card", type.getStyleClass());

        SVGPath icon = IconUtil.createIcon(type.getIconType(), 18);
        icon.getStyleClass().add("toast-icon");

        Label label = new Label(message);
        label.getStyleClass().add("toast-message");
        label.setWrapText(true);
        HBox.setHgrow(label, Priority.ALWAYS);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        getChildren().addAll(icon, label);
    }
}
