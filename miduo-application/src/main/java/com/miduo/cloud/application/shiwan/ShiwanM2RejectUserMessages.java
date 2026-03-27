package com.miduo.cloud.application.shiwan;

/**
 * 石湾 2 号机剔除/失败提示文案，与产品文档「异常类型 + 界面显示格式」一致（10.1.x）。
 */
public final class ShiwanM2RejectUserMessages {

    private ShiwanM2RejectUserMessages() {}

    /**
     * 将 CodeRelationUpload 中 Status=5（重码）记录的 Msg 与瓶/盒码转为操作工可读文案。
     */
    public static String formatStatus5Row(String boxCode, String bottleCode, String dbMsg) {
        String box = trimOrEmpty(boxCode);
        String bot = trimOrEmpty(bottleCode);
        if (!bot.isEmpty()) {
            return "瓶码 " + bot + " 重复出现（重码）";
        }
        if (!box.isEmpty()) {
            return "盒码 " + box + " 重复出现（重码）";
        }
        return "重码";
    }

    /**
     * 将 CodeRelationUpload 中 Status=4 记录的 Msg 与瓶/盒码转为操作工可读文案。
     */
    public static String formatStatus4Row(String boxCode, String bottleCode, String dbMsg) {
        String box = trimOrEmpty(boxCode);
        String bot = trimOrEmpty(bottleCode);
        String m = dbMsg == null ? "" : dbMsg.trim();

        if (m.contains("同步重复码")) {
            if (!bot.isEmpty()) {
                return "瓶码 " + bot + " 重复出现";
            }
            return "盒码 " + box + " 重复出现";
        }
        if (m.contains("小标位数不匹配")) {
            String c = !bot.isEmpty() ? bot : box;
            return "瓶码 " + c + " 格式错误";
        }
        if (m.contains("中标位数不匹配")) {
            return "盒码 " + box + " 格式错误";
        }
        if (m.contains("瓶码不在码包热表") || m.contains("小标PackageType=1")) {
            String c = !bot.isEmpty() ? bot : box;
            return "瓶码 " + c + " 不在小标码包范围内";
        }
        if (m.contains("盒码不在码包热表") || m.contains("中标PackageType=2")) {
            return "盒码 " + box + " 不在中标码包范围内";
        }
        if (!m.isEmpty()) {
            return m;
        }
        if (!box.isEmpty()) {
            return "盒码 " + box + " 不在中标码包范围内";
        }
        return "待剔除校验不通过";
    }

    /**
     * 将盒箱关联接口等业务错误信息转为与文档一致的短句。
     */
    public static String formatAssociateApiError(String errMsg) {
        if (errMsg == null || errMsg.isBlank()) {
            return "盒箱校验不通过";
        }
        String e = errMsg.trim();

        if ("盒码不能为空".equals(e)) {
            return "盒码格式错误";
        }

        String tail = afterPrefix(e, "箱码不在大标码包内：");
        if (tail != null) {
            return "箱码 " + tail + " 不在大标码包范围内";
        }
        tail = afterPrefix(e, "箱码已使用（重码）：");
        if (tail != null) {
            return "箱码 " + tail + " 重复出现";
        }
        tail = afterPrefix(e, "盒码不在中标码包内：");
        if (tail != null) {
            return "盒码 " + tail + " 不在中标码包范围内";
        }
        tail = afterPrefix(e, "盒码不在中标码包热表中：");
        if (tail != null) {
            return "盒码 " + tail + " 不在中标码包范围内";
        }
        tail = afterPrefix(e, "盒码已使用（重码）：");
        if (tail != null) {
            return "盒码 " + tail + " 重复出现";
        }
        tail = afterPrefix(e, "盒码重复：");
        if (tail != null) {
            return "盒码 " + tail + " 重复出现";
        }
        if (e.startsWith("盒箱关联失败：") || e.startsWith("盒码校验失败：")) {
            String inner = e.contains("：") ? e.substring(e.indexOf('：') + 1).trim() : "";
            if (!inner.isEmpty()) {
                return formatAssociateApiError(inner);
            }
        }
        return e;
    }

    private static String afterPrefix(String full, String prefix) {
        if (full.startsWith(prefix)) {
            return full.substring(prefix.length()).trim();
        }
        return null;
    }

    private static String trimOrEmpty(String s) {
        if (s == null) {
            return "";
        }
        return s.trim();
    }
}
