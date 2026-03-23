package com.miduo.cloud.common.config;

/**
 * 「关于系统」弹窗内容。
 * <p>
 * 各产线的 FrontendApplication.main() 通过
 * {@code System.setProperty("app.about.text", "...内容...")} 设置展示文本，
 * 用 \n 分隔多行。无需读文件，无 classpath 依赖，任何启动方式均可用。
 * </p>
 */
public class AppVersion {

    private static final String PROP     = "app.about.text";
    private static final String FALLBACK = "米多赋码采集关联系统\n版权所有 © 米多科技";

    private AppVersion() {}

    /** 读取「关于系统」文本，未设置时返回兜底文本。 */
    public static String readAboutText() {
        return System.getProperty(PROP, FALLBACK);
    }
}
