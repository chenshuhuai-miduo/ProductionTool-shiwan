package com.miduo.cloud.variant.zhimeizhai;

import com.miduo.cloud.MiduoApplicationLauncher;

/**
 * 致美斋产线软件版本 - 启动入口。
 * 构建时使用 -Pvariant-zhimeizhai 时，将此类设为 mainClass，并将 miduo-variant-zhimeizhai 加入 classpath。
 * 行为与统一启动器一致，便于按软件版本 ID 打出独立安装包。
 */
public class ZhimeizhaiLauncher {
    // 「关于系统」内容在 MiduoFrontendApplication.main() 中配置
    public static void main(String[] args) {
        MiduoApplicationLauncher.main(args);
    }
}
