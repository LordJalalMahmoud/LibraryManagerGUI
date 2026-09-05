package com.librarymanager.util;

import javafx.animation.*;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.util.Duration;

/**
 * Utility for smooth, professional JavaFX UI transitions and micro-interactions.
 */
public class AnimationUtil {

    private static volatile boolean reduceMotion = false;

    public static void setReduceMotion(boolean reduce) {
        reduceMotion = reduce;
    }

    public static boolean isReduceMotion() {
        return reduceMotion;
    }

    public static void fadeIn(Node node, Duration duration, Runnable onFinished) {
        if (node == null) return;
        if (reduceMotion) {
            node.setOpacity(1.0);
            if (onFinished != null) onFinished.run();
            return;
        }
        node.setOpacity(0.0);
        FadeTransition ft = new FadeTransition(duration != null ? duration : Duration.millis(250), node);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        if (onFinished != null) {
            ft.setOnFinished(e -> onFinished.run());
        }
        ft.play();
    }

    public static void fadeOut(Node node, Duration duration, Runnable onFinished) {
        if (node == null) return;
        if (reduceMotion) {
            node.setOpacity(0.0);
            if (onFinished != null) onFinished.run();
            return;
        }
        FadeTransition ft = new FadeTransition(duration != null ? duration : Duration.millis(200), node);
        ft.setFromValue(node.getOpacity());
        ft.setToValue(0.0);
        if (onFinished != null) {
            ft.setOnFinished(e -> onFinished.run());
        }
        ft.play();
    }

    public static void slideFadeIn(Node node, Duration duration, double offsetY, Runnable onFinished) {
        if (node == null) return;
        if (reduceMotion) {
            node.setOpacity(1.0);
            node.setTranslateY(0.0);
            if (onFinished != null) onFinished.run();
            return;
        }
        node.setOpacity(0.0);
        node.setTranslateY(offsetY);

        FadeTransition ft = new FadeTransition(duration != null ? duration : Duration.millis(240), node);
        ft.setToValue(1.0);

        TranslateTransition tt = new TranslateTransition(duration != null ? duration : Duration.millis(240), node);
        tt.setToY(0.0);
        tt.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition pt = new ParallelTransition(ft, tt);
        if (onFinished != null) {
            pt.setOnFinished(e -> onFinished.run());
        }
        pt.play();
    }

    public static void staggerFadeIn(java.util.List<? extends Node> nodes, Duration baseDelay) {
        if (nodes == null || nodes.isEmpty()) return;
        if (reduceMotion) {
            for (Node n : nodes) {
                if (n != null) {
                    n.setOpacity(1.0);
                    n.setTranslateY(0.0);
                }
            }
            return;
        }

        Duration delay = baseDelay != null ? baseDelay : Duration.millis(40);
        for (int i = 0; i < nodes.size(); i++) {
            Node n = nodes.get(i);
            if (n == null) continue;
            n.setOpacity(0.0);
            n.setTranslateY(12.0);

            FadeTransition ft = new FadeTransition(Duration.millis(220), n);
            ft.setToValue(1.0);

            TranslateTransition tt = new TranslateTransition(Duration.millis(220), n);
            tt.setToY(0.0);
            tt.setInterpolator(Interpolator.EASE_OUT);

            ParallelTransition pt = new ParallelTransition(ft, tt);
            pt.setDelay(delay.multiply(i));
            pt.play();
        }
    }

    public static void scaleIn(Node node, Duration duration) {
        if (node == null) return;
        if (reduceMotion) {
            node.setScaleX(1.0);
            node.setScaleY(1.0);
            node.setOpacity(1.0);
            return;
        }
        node.setScaleX(0.85);
        node.setScaleY(0.85);
        node.setOpacity(0.0);

        ScaleTransition st = new ScaleTransition(duration != null ? duration : Duration.millis(220), node);
        st.setToX(1.0);
        st.setToY(1.0);
        st.setInterpolator(Interpolator.EASE_OUT);

        FadeTransition ft = new FadeTransition(duration != null ? duration : Duration.millis(220), node);
        ft.setToValue(1.0);

        ParallelTransition pt = new ParallelTransition(st, ft);
        pt.play();
    }

    public static void animateCardRemoval(Node card, Runnable onFinished) {
        if (card == null) {
            if (onFinished != null) onFinished.run();
            return;
        }
        if (reduceMotion) {
            if (onFinished != null) onFinished.run();
            return;
        }

        ScaleTransition st = new ScaleTransition(Duration.millis(200), card);
        st.setToX(0.8);
        st.setToY(0.8);
        st.setInterpolator(Interpolator.EASE_IN);

        FadeTransition ft = new FadeTransition(Duration.millis(200), card);
        ft.setToValue(0.0);

        ParallelTransition pt = new ParallelTransition(st, ft);
        pt.setOnFinished(e -> {
            if (onFinished != null) onFinished.run();
        });
        pt.play();
    }

    public static void animateProgressBar(ProgressBar progressBar, double targetProgress) {
        if (progressBar == null) return;
        double current = progressBar.getProgress();
        if (current < 0) current = 0.0;
        double target = Math.max(0.0, Math.min(1.0, targetProgress));

        if (reduceMotion) {
            progressBar.setProgress(target);
            return;
        }

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(progressBar.progressProperty(), current)),
                new KeyFrame(Duration.millis(350), new KeyValue(progressBar.progressProperty(), target, Interpolator.EASE_BOTH))
        );
        timeline.play();
    }

    public static void animateNumber(Label label, int targetValue) {
        if (label == null) return;
        try {
            int currentVal = 0;
            String text = label.getText();
            if (text != null && !text.isEmpty()) {
                String digits = text.replaceAll("[^0-9]", "");
                if (!digits.isEmpty()) {
                    currentVal = Integer.parseInt(digits);
                }
            }
            int startVal = currentVal;
            int diff = targetValue - startVal;
            if (diff == 0 || reduceMotion) {
                label.setText(String.valueOf(targetValue));
                return;
            }

            IntegerProperty countProperty = new SimpleIntegerProperty(startVal);
            countProperty.addListener((obs, oldVal, newVal) -> label.setText(String.valueOf(newVal.intValue())));

            Timeline timeline = new Timeline(
                    new KeyFrame(Duration.ZERO, new KeyValue(countProperty, startVal)),
                    new KeyFrame(Duration.millis(400), new KeyValue(countProperty, targetValue, Interpolator.EASE_OUT))
            );
            timeline.play();
        } catch (Exception e) {
            label.setText(String.valueOf(targetValue));
        }
    }

    public static void pulse(Node node) {
        if (node == null || reduceMotion) return;
        ScaleTransition st = new ScaleTransition(Duration.millis(120), node);
        st.setToX(1.06);
        st.setToY(1.06);
        st.setAutoReverse(true);
        st.setCycleCount(2);
        st.setInterpolator(Interpolator.EASE_BOTH);
        st.play();
    }

    public static void shake(Node node) {
        if (node == null || reduceMotion) return;
        double origX = node.getTranslateX();
        Timeline tl = new Timeline(
                new KeyFrame(Duration.millis(0), new KeyValue(node.translateXProperty(), origX)),
                new KeyFrame(Duration.millis(50), new KeyValue(node.translateXProperty(), origX - 7)),
                new KeyFrame(Duration.millis(100), new KeyValue(node.translateXProperty(), origX + 7)),
                new KeyFrame(Duration.millis(150), new KeyValue(node.translateXProperty(), origX - 5)),
                new KeyFrame(Duration.millis(200), new KeyValue(node.translateXProperty(), origX + 5)),
                new KeyFrame(Duration.millis(250), new KeyValue(node.translateXProperty(), origX - 2)),
                new KeyFrame(Duration.millis(300), new KeyValue(node.translateXProperty(), origX))
        );
        tl.play();
    }

    public static void addCardHover(Node card) {
        if (card == null) return;
        card.setOnMouseEntered(e -> {
            if (reduceMotion) return;
            ScaleTransition st = new ScaleTransition(Duration.millis(150), card);
            st.setToX(1.015);
            st.setToY(1.015);
            st.setInterpolator(Interpolator.EASE_OUT);
            st.play();
        });
        card.setOnMouseExited(e -> {
            if (reduceMotion) return;
            ScaleTransition st = new ScaleTransition(Duration.millis(150), card);
            st.setToX(1.0);
            st.setToY(1.0);
            st.setInterpolator(Interpolator.EASE_OUT);
            st.play();
        });
    }

    public static void elevateOnHover(Node node) {
        if (node == null) return;
        node.setOnMouseEntered(e -> {
            if (reduceMotion) return;
            TranslateTransition tt = new TranslateTransition(Duration.millis(140), node);
            tt.setToY(-3.0);
            tt.setInterpolator(Interpolator.EASE_OUT);
            tt.play();
        });
        node.setOnMouseExited(e -> {
            if (reduceMotion) return;
            TranslateTransition tt = new TranslateTransition(Duration.millis(140), node);
            tt.setToY(0.0);
            tt.setInterpolator(Interpolator.EASE_OUT);
            tt.play();
        });
    }
}
