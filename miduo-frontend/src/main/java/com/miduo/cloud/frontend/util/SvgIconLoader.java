package com.miduo.cloud.frontend.util;

import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 从 classpath 加载简单 SVG（path + 可选 translate）到 StackPane，避免依赖系统 Emoji 字体。
 */
public final class SvgIconLoader {

    public static final String ICON_WARN     = "/icons/警告.svg";
    public static final String ICON_SUCCESS  = "/icons/成功.svg";
    public static final String ICON_ERROR    = "/icons/错误.svg";
    public static final String ICON_LOCK     = "/icons/锁.svg";
    public static final String ICON_EYE_SHOW = "/icons/眼睛-显示.svg";
    public static final String ICON_EYE_HIDE = "/icons/眼睛-隐藏.svg";
    public static final String ICON_QUERY    = "/icons/查询码.svg";

    private static final Pattern PATH_TAG = Pattern.compile(
        "<path\\s+([^>]+)/?>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern ATTR_D = Pattern.compile("\\bd=\"([^\"]*)\"");
    private static final Pattern ATTR_TRANSFORM = Pattern.compile("\\btransform=\"([^\"]*)\"");
    private static final Pattern VIEW_BOX = Pattern.compile(
        "viewBox=\"\\s*([\\d.]+)\\s+([\\d.]+)\\s+([\\d.]+)\\s+([\\d.]+)\\s*\"",
        Pattern.CASE_INSENSITIVE);
    private static final Pattern TRANSLATE = Pattern.compile(
        "translate\\s*\\(\\s*([-\\d.]+)\\s*[, ]\\s*([-\\d.]+)\\s*\\)", Pattern.CASE_INSENSITIVE);

    private SvgIconLoader() {}

    /**
     * 将 SVG 中的 path 绘制到目标面板，缩放到约 boxSize 见方。
     */
    public static void loadInto(StackPane target, String classpathResource, double boxSize, Paint fill) {
        if (target == null) {
            return;
        }
        target.getChildren().clear();
        target.setAlignment(Pos.CENTER);
        target.setMinSize(boxSize, boxSize);
        target.setPrefSize(boxSize, boxSize);
        target.setMaxSize(boxSize, boxSize);

        String svg = readResource(classpathResource);
        if (svg == null) {
            return;
        }

        double vbW = 24, vbH = 24;
        Matcher vb = VIEW_BOX.matcher(svg);
        if (vb.find()) {
            vbW = Double.parseDouble(vb.group(3));
            vbH = Double.parseDouble(vb.group(4));
        }

        List<PathSpec> specs = extractPaths(svg);
        if (specs.isEmpty()) {
            return;
        }

        Group inner = new Group();
        for (PathSpec spec : specs) {
            SVGPath sp = new SVGPath();
            sp.setContent(spec.d);
            sp.setFill(fill);
            sp.setStrokeWidth(0);
            Group wrap = new Group(sp);
            applyTranslate(wrap, spec.transform);
            inner.getChildren().add(wrap);
        }

        double scale = Math.min(boxSize / vbW, boxSize / vbH);
        Group scaled = new Group(inner);
        scaled.setScaleX(scale);
        scaled.setScaleY(scale);

        Rectangle clip = new Rectangle(boxSize, boxSize);
        target.setClip(clip);
        target.getChildren().add(scaled);
        StackPane.setAlignment(scaled, Pos.CENTER);
    }

    private static void applyTranslate(Group g, String transform) {
        if (transform == null) {
            return;
        }
        String t = transform.trim();
        Matcher m = TRANSLATE.matcher(t);
        if (m.find()) {
            g.setTranslateX(Double.parseDouble(m.group(1)));
            g.setTranslateY(Double.parseDouble(m.group(2)));
        }
    }

    private static List<PathSpec> extractPaths(String svg) {
        List<PathSpec> out = new ArrayList<>();
        Matcher m = PATH_TAG.matcher(svg);
        while (m.find()) {
            String attrs = m.group(1);
            Matcher dm = ATTR_D.matcher(attrs);
            if (!dm.find()) {
                continue;
            }
            String d = dm.group(1);
            if (d == null || d.isEmpty()) {
                continue;
            }
            String tr = null;
            Matcher tm = ATTR_TRANSFORM.matcher(attrs);
            if (tm.find()) {
                tr = tm.group(1);
            }
            out.add(new PathSpec(d, tr));
        }
        return out;
    }

    private static String readResource(String classpathResource) {
        try (InputStream in = SvgIconLoader.class.getResourceAsStream(classpathResource)) {
            if (in == null) {
                return null;
            }
            try (Scanner s = new Scanner(in, StandardCharsets.UTF_8)) {
                return s.useDelimiter("\\A").hasNext() ? s.next() : "";
            }
        } catch (Exception e) {
            return null;
        }
    }

    private static final class PathSpec {
        final String d;
        final String transform;

        PathSpec(String d, String transform) {
            this.d = d;
            this.transform = transform;
        }
    }
}
