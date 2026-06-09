package com.pulseweaver.heartbeat.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import platform.UIKit.UIApplication
import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.UIWindow

@Composable
actual fun SystemAppearance(darkTheme: Boolean) {
    SideEffect {
        // Matching the window's interface style flips the status-bar icons to the
        // contrasting colour (light icons on dark, dark icons on light).
        val style =
            if (darkTheme) {
                UIUserInterfaceStyle.UIUserInterfaceStyleDark
            } else {
                UIUserInterfaceStyle.UIUserInterfaceStyleLight
            }
        UIApplication.sharedApplication.windows.forEach { window ->
            (window as? UIWindow)?.overrideUserInterfaceStyle = style
        }
    }
}
