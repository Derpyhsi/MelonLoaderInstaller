﻿using SharpAdbClient;
using System.Diagnostics;
using System.Net;
using Websocket.Client;

namespace LemonADBBridge
{
    internal static class UninstallationHandler
    {
        private static WebsocketClient wsClient;
        private static AdbClient adbClient;
        private static DeviceData deviceData;

        private static string? packageToUninstall;

        public static async Task Run(AdbClient client, DeviceData data, MainForm mainForm)
        {
            deviceData = data;
            adbClient = client;

            adbClient.RemoveAllForwards(deviceData);
            adbClient.CreateForward(deviceData, "tcp:9000", "tcp:9000", true);

            mainForm.statusText.Text = "WAITING FOR CONNECTION...";

            wsClient = new WebsocketClient(new Uri("ws://localhost:9000"));
            wsClient.MessageReceived.Subscribe(e => packageToUninstall = e.Text);

            wsClient.ErrorReconnectTimeout = TimeSpan.FromSeconds(3);
            wsClient.ReconnectTimeout = TimeSpan.FromSeconds(3);
            wsClient.IsReconnectionEnabled = true;
            await wsClient.Start();

            mainForm.statusText.Text = "CONNECTED";

            while (packageToUninstall == null)
            {
                await Task.Delay(500);
            }

            var receiver = new ConsoleOutputReceiver();

            if (mainForm.copyLocal.Checked)
            {
                Process proc = new()
                {
                    StartInfo = new ProcessStartInfo()
                    {
                        FileName = StaticStuff.ADBPath,
                        Arguments = $"pull /sdcard/Android/data/{packageToUninstall} \"{Directory.GetCurrentDirectory()}\"",
                        CreateNoWindow = true
                    }
                };
                proc.Start();
                proc.WaitForExit();
                adbClient.ExecuteRemoteCommand($"mv /sdcard/Android/obb/{packageToUninstall} /sdcard/Android/obb/{packageToUninstall}_BACKUP", deviceData, receiver);

                adbClient.ExecuteRemoteCommand("pm uninstall " + packageToUninstall, deviceData, receiver);

                proc.StartInfo.Arguments = $"push \"{Directory.GetCurrentDirectory()}\\{packageToUninstall}\" /sdcard/Android/data/";
                proc.Start();
                proc.WaitForExit();
                adbClient.ExecuteRemoteCommand($"mv /sdcard/Android/obb/{packageToUninstall}_BACKUP /sdcard/Android/obb/{packageToUninstall}", deviceData, receiver);
            }
            else
            {
                adbClient.ExecuteRemoteCommand($"mv /sdcard/Android/data/{packageToUninstall} /sdcard/Android/data/{packageToUninstall}_BACKUP", deviceData, receiver);
                adbClient.ExecuteRemoteCommand($"mv /sdcard/Android/obb/{packageToUninstall} /sdcard/Android/obb/{packageToUninstall}_BACKUP", deviceData, receiver);

                // Barely handles permission conflicts when reinstalling MelonLoader
                adbClient.ExecuteRemoteCommand($"rm -rf /sdcard/Android/data/{packageToUninstall}_BACKUP/cache", deviceData, receiver);
                adbClient.ExecuteRemoteCommand($"rm -rf /sdcard/Android/data/{packageToUninstall}_BACKUP/files/melonloader", deviceData, receiver);
                adbClient.ExecuteRemoteCommand($"rm -rf /sdcard/Android/data/{packageToUninstall}_BACKUP/files/il2cpp", deviceData, receiver);
                adbClient.ExecuteRemoteCommand($"rm /sdcard/Android/data/{packageToUninstall}_BACKUP/files/funchook.log", deviceData, receiver);

                adbClient.ExecuteRemoteCommand("pm uninstall " + packageToUninstall, deviceData, receiver);

                adbClient.ExecuteRemoteCommand($"mv /sdcard/Android/data/{packageToUninstall}_BACKUP /sdcard/Android/data/{packageToUninstall}", deviceData, receiver);
                adbClient.ExecuteRemoteCommand($"mv /sdcard/Android/obb/{packageToUninstall}_BACKUP /sdcard/Android/obb/{packageToUninstall}", deviceData, receiver);
            }

            await wsClient.NativeClient.SendAsync(new byte[] { 1 }, System.Net.WebSockets.WebSocketMessageType.Binary, true, default).ConfigureAwait(false);

            adbClient.RemoveForward(deviceData, 9000);

            mainForm.statusText.Text = "COMPLETE+DISCONNECTED";

            Dispose();
        }

        public static void Dispose()
        {
            try
            {
                adbClient?.KillAdb();
            }
            catch { }
            wsClient?.Dispose();
        }
    }
}
