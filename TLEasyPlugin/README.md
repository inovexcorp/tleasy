# TLEasy Plugin for STK

This plugin integrates TLE analysis directly into Systems Tool Kit (STK), allowing users to load satellite data, visualize access, and generate reports efficiently.

## Prerequisites

- **AGI Systems Tool Kit (STK) 12** (or compatible version)
- **.NET Framework 4.8** (Installed by default on most modern Windows systems)

---

## Automated Installation (Recommended)

The easiest way to install the plugin is to use the provided automated installer script. This method does **not** require Administrative privileges as it registers the plugin for the current user only.

1.  Locate the folder containing `TLEasyPlugin.dll` and `install_plugin.bat`.
2.  Double-click **`install_plugin.bat`**.
3.  A command window will appear. If successful, it will display `[SUCCESS] TLEasy installed successfully!`.
4.  Launch STK and the **TLEasy** toolbar should be visible.

---

## Manual Installation

If the automated script cannot be used, you can install the plugin manually using one of the methods below.

### Method 1: Pre-Built Binaries

Use this method if you have the `TLEasyPlugin.dll` file and need to install it manually. Note that all icons and assets are embedded inside the DLL, so no separate image files are required.

1.  **Place the File:**
    *   Copy `TLEasyPlugin.dll` to a permanent location on your machine (e.g., `C:\Program Files\AGI\STK 12\Plugins\TLEasyPlugin\`).
2.  **Register the DLL:**
    *   Open a **Command Prompt** as **Administrator**.
    *   Navigate to the .NET framework tool directory:
        `cd C:\Windows\Microsoft.NET\Framework64\v4.0.30319`
    *   Run the registration command (replace `[PATH]` with the actual path to your DLL):
        `regasm.exe /codebase "[PATH]\TLEasyPlugin.dll"`
    *   *Note: To register without Admin rights, add the `/user` flag to the command above.

### Method 2: Build from Source

Use this method if you need to compile the plugin from the source code.

1.  **Prerequisites for Building:**
    *   Visual Studio 2019 or 2022 with **.NET Desktop Development** workload.
    *   STK 12 installed on the build machine.
2.  **Build and Register:**
    *   Open `TLEasyPlugin.slnx` (or `.sln`) in Visual Studio.
    *   Set the build configuration to **Release**.
    *   Right-click the **TLEasyPlugin** project -> **Properties** -> **Build** tab.
    *   Ensure **"Register for COM interop"** is CHECKED. (Requires running Visual Studio as Admin).
    *   Build the solution (**Ctrl+Shift+B**). This automatically registers the DLL on the build machine.

---

## Post-Installation Verification

1.  Launch STK.
2.  Go to **Edit** -> **Preferences** -> **UI Plugins**.
3.  Verify that **TLEasy** appears in the list and is checked.
4.  If the toolbar is not visible, go to **View** -> **Toolbars** and check **TLEasy**.
5.  If icons appear missing or the toolbar is bugged, click **Reset Toolbars** in the UI Plugins preference page.
6.  After you enable the TLEasy toolbar button, you can save you scenario to ensure the toolbar will be enabled already next time you load it up.

---

## Troubleshooting

- **Toolbar not showing?** Check **View -> Toolbars** or perform a **Reset Toolbars** in Preferences.
- **"Class not registered" error?** Ensure the registration step (automated or manual) completed without errors.
- **Plugin crashes on load?** Verify that the user has valid write permissions to the Registry key: `HKEY_CURRENT_USER\Software\RealmOne\TLEasyPlugin`.