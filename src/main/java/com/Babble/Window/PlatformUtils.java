package com.Babble.Window;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.GDI32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.stage.Stage;

public class PlatformUtils {
    private static final int GWL_EXSTYLE = -20;
    private static final int WS_EX_LAYERED = 0x80000;
    private static final int WS_EX_TRANSPARENT = 0x20;
    private static final int WS_EX_NOACTIVATE = 0x08000000;
    private static final int WDA_EXCLUDEFROMCAPTURE = 0x00000011;

    public interface User32Ext extends User32 {
        User32Ext INSTANCE = Native.load(
            "user32",
            User32Ext.class,
            com.sun.jna.win32.W32APIOptions.DEFAULT_OPTIONS
        );

        boolean SetWindowDisplayAffinity(WinDef.HWND hWnd, int dwAffinity);
    }

    public static void makeWindowTransparentRaw(Stage stage) {
        if (!com.sun.jna.Platform.isWindows())
            return;

        String title = stage.getTitle();
        WinDef.HWND hwnd = User32.INSTANCE.FindWindow(null, title);
        if (hwnd != null) {
            int exStyle = User32.INSTANCE.GetWindowLong(hwnd, GWL_EXSTYLE);
            User32.INSTANCE.SetWindowLong(
                hwnd, GWL_EXSTYLE,
                exStyle | WS_EX_LAYERED | WS_EX_TRANSPARENT | WS_EX_NOACTIVATE
            );
        }
    }

    public static void setWindowDisplayAffinity(Stage stage, boolean enable) {
        if (!com.sun.jna.Platform.isWindows())
            return;

        String title = stage.getTitle();
        WinDef.HWND hwnd = User32.INSTANCE.FindWindow(null, title);
        if (hwnd != null) {
            int affinity = enable ? WDA_EXCLUDEFROMCAPTURE : 0;
            boolean affinitySet = User32Ext.INSTANCE.SetWindowDisplayAffinity(hwnd, affinity);
            if (!affinitySet) {
                System.err.println("Failed to set WindowDisplayAffinity to " + affinity);
            } else {
                System.out.println("WDA set to " + (enable ? "EXCLUDE" : "NONE"));
            }
        }
    }

    public static void updateWindowRegion(Stage stage, Node holeNode) {
        if (!com.sun.jna.Platform.isWindows())
            return;

        String title = stage.getTitle();
        WinDef.HWND hwnd = User32.INSTANCE.FindWindow(null, title);

        if (hwnd == null)
            return;

        double scaleX = stage.getOutputScaleX(), scaleY = stage.getOutputScaleY();
        int
            w = (int) (stage.getWidth() * scaleX),
            h = (int) (stage.getHeight() * scaleY)
        ;

        WinDef.HRGN fullRgn = null, holeRgn = null, combinRgn = null;
        try {
            fullRgn = GDI32.INSTANCE.CreateRectRgn(0, 0, w, h);

            Point2D panePos = holeNode.localToScene(0, 0);
            int
                paneX = (int) (panePos.getX() * scaleX),
                paneY = (int) (panePos.getY() * scaleY),
                paneW = (int) (holeNode.getLayoutBounds().getWidth() * scaleX),
                paneH = (int) (holeNode.getLayoutBounds().getHeight() * scaleY),
                borderInset = (int) (5 * scaleX),
                holeL = paneX + borderInset,
                holeR = paneX + paneW - borderInset,
                holeT = paneY + borderInset,
                holeB = paneY + paneH - borderInset
            ;

            if (holeR > holeL && holeB > holeT) {
                holeRgn = GDI32.INSTANCE.CreateRectRgn(holeL, holeT, holeR, holeB);
            }

            combinRgn = GDI32.INSTANCE.CreateRectRgn(0, 0, 0, 0);

            if (holeRgn != null) {
                GDI32.INSTANCE.CombineRgn(combinRgn, fullRgn, holeRgn, 4); // RGN_DIFF
            } else {
                GDI32.INSTANCE.CombineRgn(combinRgn, fullRgn, null, 2); // RGN_COPY
            }

            User32.INSTANCE.SetWindowRgn(hwnd, combinRgn, true);
        } finally {
            if (fullRgn != null)
                GDI32.INSTANCE.DeleteObject(fullRgn);
            if (holeRgn != null)
                GDI32.INSTANCE.DeleteObject(holeRgn);
            if (combinRgn != null)
                GDI32.INSTANCE.DeleteObject(combinRgn);
        }
    }
}
