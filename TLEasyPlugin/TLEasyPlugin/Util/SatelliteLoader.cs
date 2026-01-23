using AGI.STKObjects;
using AGI.STKUtil;
using System;
using System.Collections.Generic;
using System.IO;
using System.Text.RegularExpressions;

namespace TLEasyPlugin.Util
{
    public class SatelliteLoader
    {
        private readonly IAgStkObjectRoot _stkRoot;
        private readonly Action<string> _statusCallback;

        public SatelliteLoader(IAgStkObjectRoot stkRoot, Action<string> statusCallback)
        {
            _stkRoot = stkRoot;
            _statusCallback = statusCallback;
        }

        /// <summary>
        /// Loads specific satellites from a TLE file into STK.
        /// </summary>
        /// <param name="tleDataList">List of pre-parsed TLE data.</param>
        /// <param name="tleFilePath">Full path to the TLE file.</param>
        /// <param name="nameToColorMap">Map of sanitized satellite names to their hex color string.</param>
        public void LoadSatellites(List<TleData> tleDataList, string tleFilePath, Dictionary<string, string> nameToColorMap)
        {
            if (!File.Exists(tleFilePath))
            {
                _statusCallback?.Invoke("Error: TLE file not found."); return;
            }

            if (tleDataList.Count == 0)
            {
                _statusCallback?.Invoke("No matching satellites found.");
                return;
            }

            _statusCallback?.Invoke($"Loading {tleDataList.Count} satellites...");

            int current = 0;
            foreach (var sat in tleDataList)
            {
                current++;
                string satName = sat.SatName;
                string satId = sat.NoradId;

                _statusCallback?.Invoke($"Loading {current}/{tleDataList.Count}: {satName}...");

                try
                {
                    string objPath = $"/Scenario/{GetScenarioName()}/Satellite/{satName}";
                    if (ObjectExists(objPath))
                    {
                        SendCommand($"Unload / */Satellite/{satName}");
                    }

                    SendCommand($"New / */Satellite {satName}");

                    string setStateCmd = $"SetState */Satellite/{satName} SGP4 UseScenarioInterval 60.0 {satId} TLESource Automatic Source File \"{tleFilePath}\"";
                    string result = SendCommand(setStateCmd);

                    if (result.Contains("E_CommandFailed"))
                    {
                        Console.WriteLine($"Failed to set state for {satName}");
                        continue; // Skip coloring if state fails
                    }
                    
                    // --- APPLY COLOR ---
                    if (nameToColorMap.ContainsKey(satName))
                    {
                        string color = nameToColorMap[satName];
                        string satPath = $"*/Satellite/{satName}"; // Use wildcard path for consistency
                        string setColorCmd = $"Graphics {satPath} SetColor {color}";
                        SendCommand(setColorCmd);
                    }
                }
                catch (Exception ex)
                {
                    Console.WriteLine($"Error loading {satName}: {ex.Message}");
                }
            }

            _statusCallback?.Invoke("Satellite loading complete.");
        }

        private string GetScenarioName()
        {
            try
            {
                IAgStkObject scenarioObj = _stkRoot.CurrentScenario as IAgStkObject;

                if (scenarioObj != null)
                {
                    return scenarioObj.InstanceName;
                }
                return "*";
            }
            catch
            {
                return "*";
            }
        }

        private bool ObjectExists(string path)
        {
            try
            {
                IAgExecCmdResult res = _stkRoot.ExecuteCommand($"CheckExistence {path}");
                return res[0].ToString() == "1";
            }
            catch
            {
                return false;
            }
        }

        private string SendCommand(string command)
        {
            try
            {
                IAgExecCmdResult result = _stkRoot.ExecuteCommand(command);
                if (result.Count > 0) return result[0].ToString();
                return "";
            }
            catch (Exception ex)
            {
                return "E_CommandFailed: " + ex.Message;
            }
        }

        /// <summary>
        /// Gets a list of names of all Facilities currently in the scenario.
        /// </summary>
        public List<string> GetFacilityNames()
        {
            var names = new List<string>();

            // We use the Object Model here because it's cleaner than parsing Connect strings
            if (_stkRoot.CurrentScenario != null)
            {
                foreach (IAgStkObject child in _stkRoot.CurrentScenario.Children)
                {
                    if (child.ClassType == AgESTKObjectType.eFacility)
                    {
                        names.Add(child.InstanceName);
                    }
                }
            }
            return names;
        }

        /// <summary>
        /// Computes access between a satellite and a facility and returns start/stop/duration.
        /// </summary>
        public List<AccessResult> ComputeAccess(string satName, string facilityName)
        {
            var results = new List<AccessResult>();

            try
            {
                string satPath = $"*/Satellite/{satName}";
                string facPath = $"*/Facility/{facilityName}";

                if (!ObjectExists(satPath) || !ObjectExists(facPath)) return results;

                IAgStkObject sat = _stkRoot.GetObjectFromPath(satPath);
                IAgStkObject fac = _stkRoot.GetObjectFromPath(facPath);

                IAgStkAccess access = sat.GetAccessToObject(fac);
                access.ComputeAccess();

                IAgIntervalCollection intervals = access.ComputedAccessIntervalTimes;

                dynamic intervalsDyn = access.ComputedAccessIntervalTimes;

                for (int i = 0; i < intervalsDyn.Count; i++)
                {
                    var interval = intervalsDyn[i];

                    results.Add(new AccessResult
                    {
                        StartTime = interval.Start,
                        StopTime = interval.Stop,
                        Duration = (double)interval.Stop - (double)interval.Start
                    });
                }
            }
            catch (Exception ex)
            {
                System.Diagnostics.Debug.WriteLine($"Access Error: {ex.Message}");
            }

            return results;
        }
    }

    // Helper class for the results
    public class AccessResult
    {
        public object StartTime { get; set; }
        public object StopTime { get; set; }
        public double Duration { get; set; }
    }
}
