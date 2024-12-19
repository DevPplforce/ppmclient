import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import com.github.kwhat.jnativehook.mouse.NativeMouseEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseListener;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Idletime implements NativeKeyListener, NativeMouseListener {
    private static long lastActivityTime = System.currentTimeMillis();
    private static String line = "", oldline = "";
    private static final long IDLE_THRESHOLD = 1 * 60 * 1000; // 1 minutes in milliseconds

    public static void main(String[] args) {
        try {
            // Register native hooks for keyboard and mouse events
            GlobalScreen.registerNativeHook();
            GlobalScreen.addNativeKeyListener(new Idletime());
            GlobalScreen.addNativeMouseListener(new Idletime());

            while (true) {
                // Periodically check idle time
                checkSystemIdleTime();

                // Sleep for 1 minute before checking again
                Thread.sleep(60000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        resetIdleTimer();
    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent e) {}

    @Override
    public void nativeMouseClicked(NativeMouseEvent e) {
        resetIdleTimer();
    }

    @Override
    public void nativeMousePressed(NativeMouseEvent e) {}

    @Override
    public void nativeMouseReleased(NativeMouseEvent e) {}

    private static void resetIdleTimer() {
        lastActivityTime = System.currentTimeMillis(); // Reset idle timer
        System.out.printf("Last activity time: %d%n", lastActivityTime);
    }

    private static void checkSystemIdleTime() {
        boolean isMediaPlaying = isAudioPlaying();
        long currentTime = System.currentTimeMillis();
        long idleTimeMillis = currentTime - lastActivityTime;
        long idleTimeMinutes = idleTimeMillis / (60 * 1000);

        if (!isMediaPlaying) {
            System.out.printf("System idle time: %d minutes%n", idleTimeMinutes);
            if (idleTimeMillis > IDLE_THRESHOLD) {
                System.out.println("System has been idle for more than 1 minute.");
            }
        } else {
            System.out.printf("System idle time: %d minutes%n", idleTimeMinutes);
            System.out.println("Media or audio is playing. Ignoring idle time.");
        }
    }

private static boolean checkKernelModeTime() {
    try {
        Process currentKernelTime = new ProcessBuilder(
            "C:\\Windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe",
            "-Command",
            "(Get-WmiObject -Class Win32_Process | Where-Object {$.Name -like 'audiodg' -or $.Name -like 'wmplayer'}).KernelModeTime"
        ).start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(currentKernelTime.getInputStream()));
        
        String curLine = reader.readLine(); // Only reads the first line, ensure it's correct
        System.out.println("Output from PowerShell: " + curLine);

        if (curLine == null || curLine.isEmpty()) {
            System.err.println("Error: PowerShell command returned no output.");
            return false;
        }

        // Parse current kernel time
        int curTime = Integer.parseInt(curLine.trim());

        int prevTime = (oldline != null && !oldline.equalsIgnoreCase("")) ? Integer.parseInt(oldline) : 0; // Use a default value if oldline is null
        // Compare kernel times
        System.out.println("Previous Kernel Time: " + oldline + ", PrevTime: " + prevTime);

        // Update oldline for the next check
        oldline = curLine;
        System.out.println("Current Kernel Time: " + curLine + ", curTime: "+ curTime);

        if (curTime > prevTime && prevTime > 0) {
            resetIdleTimer();
            System.out.println("Audio is playing. and idletime restarted");
        } else {
            System.out.println("Audio is paused or not playing.");
        }

        currentKernelTime.waitFor(); // Wait for process to complete
        return true;
    } catch (Exception e) {
        System.err.println("Error parsing kernel mode time: " + e.getMessage());
        e.printStackTrace();
    }
    return false;
}


    // private static boolean isMediaPlaying() {
    //     String os = System.getProperty("os.name").toLowerCase();
    //     if (os.contains("win")) {
    //         return checkMediaPlaybackWindows();
    //     } else if (os.contains("mac")) {
    //         return checkMediaPlaybackMac();
    //     } else if (os.contains("nux") || os.contains("nix")) {
    //         return checkMediaPlaybackLinux();
    //     }
    //     return false;
    // }

    private static boolean checkMediaPlaybackWindows() {
        
          // 
        try {
             Process process = new ProcessBuilder("C:\\Windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe","-Command","(Get-WmiObject -Class Win32_Process | Where-Object {$.Name -like 'audiodg' -or $.Name -like 'wmplayer'}).KernelModeTime").start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
           String currline = reader.readLine();
           if (currline == null || currline.isEmpty()) {
            System.err.println("Error: PowerShell command returned no output.");
           // return;
        }
            System.out.println("Windows audio device info: "+currline);
            int curtime = Integer.parseInt(currline);
            int prevtime = Integer.parseInt(oldline);
            System.out.println("Windows audio device info: "+oldline);
            if(curtime>prevtime){
               System.out.println("Audio is playing" + oldline);
            }else{
                System.out.println("Audio is in Pause state or not playing");
            } 
           // return line != null && line.contains("True");
        } catch (Exception e) {
            System.err.println("Error checking media playback on Windows: " + e.getMessage());
        }
        return false;
    }

    private static boolean checkMediaPlaybackMac() {
        try {
            Process process = new ProcessBuilder("osascript", "-e",
                    "tell application \"System Events\" to get the name of every process whose name contains \"QuickTime Player\"").start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            return line != null && !line.isEmpty();
        } catch (Exception e) {
            System.err.println("Error checking media playback on macOS: " + e.getMessage());
        }
        return false;
    }

    private static boolean checkMediaPlaybackLinux() {
        try {
            Process process = new ProcessBuilder("playerctl", "status").start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            return line != null && line.equalsIgnoreCase("Playing");
        } catch (Exception e) {
            System.err.println("Error checking media playback on Linux: " + e.getMessage());
        }
        return false;
    }
    private static boolean isAudioPlaying() {
        String os = System.getProperty("os.name").toLowerCase();
        try {
            if (os.contains("win")) {
                boolean isaudioPlaying = checkKernelModeTime();
                return isaudioPlaying;
            } else if (os.contains("mac")) {
                // Check for audio device presence
                Process deviceCheck = new ProcessBuilder("osascript", "-e",
                        "output volume of (get volume settings)").start();
                BufferedReader deviceReader = new BufferedReader(new InputStreamReader(deviceCheck.getInputStream()));
                String deviceOutput = deviceReader.readLine();
                System.out.println("macOS audio playback device output: " + deviceOutput); // Debug log
                
                if (deviceOutput == null || deviceOutput.trim().equalsIgnoreCase("missing value")) {
                    System.err.println("No active audio device detected.");
                    return false;
                }
                
                // Check volume and mute status
                int volume = Integer.parseInt(deviceOutput.trim());
                Process muteCheck = new ProcessBuilder("osascript", "-e",
                        "get output muted of (get volume settings)").start();
                BufferedReader muteReader = new BufferedReader(new InputStreamReader(muteCheck.getInputStream()));
                String muteOutput = muteReader.readLine();
                boolean isMuted = muteOutput != null && Boolean.parseBoolean(muteOutput.trim());

                return volume > 0 && !isMuted; // Return true if audio is playing
            } else if (os.contains("nux") || os.contains("nix")) {
                // Linux: Check for running audio processes
                Process process = new ProcessBuilder("pactl", "list", "sink-inputs").start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                StringBuilder output = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
                System.out.println("Linux audio playback output: " + output); // Debug log
                return output.toString().contains("RUNNING");
            }
        } catch (Exception e) {
            System.err.println("Error checking audio playback: " + e.getMessage());
        }
        return false;
    }
    
    // private static boolean isAudioPlaying() {
    //     String os = System.getProperty("os.name").toLowerCase();
    //     try {
    //         if (os.contains("win")) {
    //             Process process = new ProcessBuilder("powershell",
    //                     "(Get-Volume | Where-Object { $_.AudioOutputEnabled -eq $true }).Volume").start();
    //             BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
    //             String line = reader.readLine();
    //             return line != null && !line.trim().isEmpty();
    //         } else if (os.contains("mac")) {
    //             Process process = new ProcessBuilder("osascript", "-e",
    //                     "output volume of (get volume settings)").start();
    //             BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
    //             String line = reader.readLine();
    //             return line != null && Integer.parseInt(line.trim()) > 0;
    //         } else if (os.contains("nux") || os.contains("nix")) {
    //             Process process = new ProcessBuilder("pactl", "list", "sink-inputs").start();
    //             BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
    //             StringBuilder output = new StringBuilder();
    //             String line;
    //             while ((line = reader.readLine()) != null) {
    //                 output.append(line);
    //             }
    //             return output.toString().contains("RUNNING");
    //         }
    //     } catch (Exception e) {
    //         System.err.println("Error checking audio playback: " + e.getMessage());
    //     }
    //     return false;
    // }
}

// import com.github.kwhat.jnativehook.GlobalScreen;
// import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
// import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
// import com.github.kwhat.jnativehook.mouse.NativeMouseEvent;
// import com.github.kwhat.jnativehook.mouse.NativeMouseListener;

// import java.io.BufferedReader;
// import java.io.InputStreamReader;

// public class Idletime implements NativeKeyListener, NativeMouseListener {
//     private static long lastActivityTime = System.currentTimeMillis();
//     private static final long IDLE_THRESHOLD = 1 * 60 * 1000; // 5 minutes in milliseconds

//     public static void main(String[] args) {
//         try {
//             // Register native hooks for keyboard and mouse events
//             GlobalScreen.registerNativeHook();
//             GlobalScreen.addNativeKeyListener(new Idletime());
//             GlobalScreen.addNativeMouseListener(new Idletime());

//             while (true) {
//                 // Periodically check idle time
//                 checkSystemIdleTime();

//                 // Sleep for 1 minute before checking again
//                 Thread.sleep(60000);
//             }
//         } catch (Exception e) {
//             e.printStackTrace();
//         }
//     }

//     @Override
//     public void nativeKeyPressed(NativeKeyEvent e) {
//         resetIdleTimer();
//     }

//     @Override
//     public void nativeKeyReleased(NativeKeyEvent e) {}

//     @Override
//     public void nativeMouseClicked(NativeMouseEvent e) {
//         resetIdleTimer();
//     }

//     @Override
//     public void nativeMousePressed(NativeMouseEvent e) {}

//     @Override
//     public void nativeMouseReleased(NativeMouseEvent e) {}

//     private static void resetIdleTimer() {
//         lastActivityTime = System.currentTimeMillis(); // Reset idle timer
//     }

//     private static void checkSystemIdleTime() {
//         long currentTime = System.currentTimeMillis();
//         long idleTimeMillis = currentTime - lastActivityTime;
//         long idleTimeMinutes = idleTimeMillis / (60 * 1000);

//         boolean isMediaPlaying = isMediaPlaying() || isAudioPlaying();

//         if (!isMediaPlaying) {
//             System.out.printf("System idle time: %d minutes%n", idleTimeMinutes);
//             if (idleTimeMillis > IDLE_THRESHOLD) {
//                 System.out.println("System has been idle for more than 5 minutes.");
//             }
//         } else {
//             System.out.println("Media or audio is playing. Ignoring idle time.");
//         }
//     }

//     private static boolean isMediaPlaying() {
//         String os = System.getProperty("os.name").toLowerCase();
//         if (os.contains("win")) {
//             return checkMediaPlaybackWindows();
//         } else if (os.contains("mac")) {
//             return checkMediaPlaybackMac();
//         } else if (os.contains("nux") || os.contains("nix")) {
//             return checkMediaPlaybackLinux();
//         }
//         return false;
//     }

//     private static boolean checkMediaPlaybackWindows() {
//         try {
//             Process process = new ProcessBuilder("powershell",
//                     "(Get-CimInstance -Namespace Root\\cimv2 -ClassName Win32_PerfFormattedData_PerfMediaPlayer_MediaPlayer).Playing").start();
//             BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
//             String line = reader.readLine();
//             return line != null && line.contains("True");
//         } catch (Exception e) {
//             System.err.println("Error checking media playback on Windows: " + e.getMessage());
//         }
//         return false;
//     }

//     private static boolean checkMediaPlaybackMac() {
//         try {
//             Process process = new ProcessBuilder("osascript", "-e",
//                     "tell application \"System Events\" to get the name of every process whose name contains \"QuickTime Player\"").start();
//             BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
//             String line = reader.readLine();
//             return line != null && !line.isEmpty();
//         } catch (Exception e) {
//             System.err.println("Error checking media playback on macOS: " + e.getMessage());
//         }
//         return false;
//     }

//     private static boolean checkMediaPlaybackLinux() {
//         try {
//             Process process = new ProcessBuilder("playerctl", "status").start();
//             BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
//             String line = reader.readLine();
//             return line != null && line.equalsIgnoreCase("Playing");
//         } catch (Exception e) {
//             System.err.println("Error checking media playback on Linux: " + e.getMessage());
//         }
//         return false;
//     }
//     private static boolean isAudioPlaying() {
//         String os = System.getProperty("os.name").toLowerCase();
//         try {
//             if (os.contains("win")) {
//             // Query the Win32_SoundDevice class for audio device status
//             Process process = new ProcessBuilder("powershell", "-Command",
//                     "Get-CimInstance -ClassName Win32_SoundDevice | Select-Object ProductName, Status").start();
//             BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
//             String line;
//             boolean isPlaying = false;
//             System.out.println("Windows audio device info: " + line);
//             while ((line = reader.readLine()) != null) {
//                 System.out.println("Windows audio device info: " + line); // Debug log
//                 // Look for the product name and check if the status indicates an active device
//                 if (line.contains("ProductName") && line.contains("Status")) {
//                     String status = line.split("Status")[1].trim();
//                     if ("OK".equalsIgnoreCase(status)) {
//                         isPlaying = true; // If status is OK, assume audio is playing
//                     }
//                 }
//             }
            
//             // Return whether an active audio device is present and playing
//             return isPlaying;
//         }else if (os.contains("mac")) {
//                 // Check for audio device presence
//                 Process deviceCheck = new ProcessBuilder("osascript", "-e",
//                         "output volume of (get volume settings)").start();
//                 BufferedReader deviceReader = new BufferedReader(new InputStreamReader(deviceCheck.getInputStream()));
//                 String deviceOutput = deviceReader.readLine();
//                 System.out.println("macOS audio playback device output: " + deviceOutput); // Debug log
                
//                 if (deviceOutput == null || deviceOutput.trim().equalsIgnoreCase("missing value")) {
//                     System.err.println("No active audio device detected.");
//                     return false;
//                 }
                
//                 // Check volume and mute status
//                 int volume = Integer.parseInt(deviceOutput.trim());
//                 Process muteCheck = new ProcessBuilder("osascript", "-e",
//                         "get output muted of (get volume settings)").start();
//                 BufferedReader muteReader = new BufferedReader(new InputStreamReader(muteCheck.getInputStream()));
//                 String muteOutput = muteReader.readLine();
//                 boolean isMuted = muteOutput != null && Boolean.parseBoolean(muteOutput.trim());
    
//                 return volume > 0 && !isMuted; // Return true if audio is playing
//             }else if (os.contains("nux") || os.contains("nix")) {
//                 // Linux: Check for running audio processes
//                 Process process = new ProcessBuilder("pactl", "list", "sink-inputs").start();
//                 BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
//                 StringBuilder output = new StringBuilder();
//                 String line;
//                 while ((line = reader.readLine()) != null) {
//                     output.append(line).append("\n");
//                 }
//                 System.out.println("Linux audio playback output: " + output); // Debug log
//                 return output.toString().contains("RUNNING");
//             }
//         } catch (Exception e) {
//             System.err.println("Error checking audio playback: " + e.getMessage());
//         }
//         return false;
//     }
    
//     // private static boolean isAudioPlaying() {
//     //     String os = System.getProperty("os.name").toLowerCase();
//     //     try {
//     //         if (os.contains("win")) {
//     //             Process process = new ProcessBuilder("powershell",
//     //                     "(Get-Volume | Where-Object { $_.AudioOutputEnabled -eq $true }).Volume").start();
//     //             BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
//     //             String line = reader.readLine();
//     //             return line != null && !line.trim().isEmpty();
//     //         } else if (os.contains("mac")) {
//     //             Process process = new ProcessBuilder("osascript", "-e",
//     //                     "output volume of (get volume settings)").start();
//     //             BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
//     //             String line = reader.readLine();
//     //             return line != null && Integer.parseInt(line.trim()) > 0;
//     //         } else if (os.contains("nux") || os.contains("nix")) {
//     //             Process process = new ProcessBuilder("pactl", "list", "sink-inputs").start();
//     //             BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
//     //             StringBuilder output = new StringBuilder();
//     //             String line;
//     //             while ((line = reader.readLine()) != null) {
//     //                 output.append(line);
//     //             }
//     //             return output.toString().contains("RUNNING");
//     //         }
//     //     } catch (Exception e) {
//     //         System.err.println("Error checking audio playback: " + e.getMessage());
//     //     }
//     //     return false;
//     // }
// }



// import com.github.kwhat.jnativehook.GlobalScreen;
// import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
// import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
// import com.github.kwhat.jnativehook.mouse.NativeMouseEvent;
// import com.github.kwhat.jnativehook.mouse.NativeMouseListener;

// import java.io.BufferedReader;
// import java.io.InputStreamReader;

// public class Idletime implements NativeKeyListener, NativeMouseListener {
//     private static long lastActivityTime = System.currentTimeMillis();
//     private static final long IDLE_THRESHOLD = 5 * 60 * 1000; // 5 minutes in milliseconds

//     public static void main(String[] args) {
//         try {
//             // Register native hook for keyboard and mouse events
//             GlobalScreen.registerNativeHook();
//             GlobalScreen.addNativeKeyListener(new Idletime());
//             GlobalScreen.addNativeMouseListener(new Idletime());

//             while (true) {
//                 // Periodically check system-wide idle time
//                 checkSystemIdleTime();

//                 // Sleep for a second before checking again
//                 Thread.sleep(60000);
//             }
//         } catch (Exception e) {
//             e.printStackTrace();
//         }
//     }

//     // Keyboard activity listener
//     @Override
//     public void nativeKeyPressed(NativeKeyEvent e) {
//         lastActivityTime = System.currentTimeMillis(); // Reset idle timer
//     }

//     @Override
//     public void nativeKeyReleased(NativeKeyEvent e) {}

//     // Mouse activity listener
//     @Override
//     public void nativeMouseClicked(NativeMouseEvent e) {
//         lastActivityTime = System.currentTimeMillis(); // Reset idle timer
//     }

//     @Override
//     public void nativeMousePressed(NativeMouseEvent e) {}

//     @Override
//     public void nativeMouseReleased(NativeMouseEvent e) {}

//     // System-wide idle time checker (platform-specific)
//     private static void checkSystemIdleTime() {
//         String os = System.getProperty("os.name").toLowerCase();
//         System.out.println(os);
//         long currentTime = System.currentTimeMillis();

//         try {
//             if (os.contains("win")) {
//                 // Windows: Using PowerShell to get the last user input time
//                 Process process = new ProcessBuilder("powershell",
//                         "(Get-CimInstance Win32_OperatingSystem).LastBootUpTime").start();
//                 BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
//                 String line;
//                 while ((line = reader.readLine()) != null) {
//                     System.out.println("Windows system idle check: " + line);
//                 }
                
//                 // If you want more detailed idle time tracking, use the GetLastInputInfo API, not shown here (use JNI or JNativeHook).
//             } else if (os.contains("mac")) {
//                 // macOS: Using ioreg to get system idle time
//                 Process process = new ProcessBuilder("ioreg", "-c", "IOHIDSystem").start();
//                 BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
//                 String line;
//                 while ((line = reader.readLine()) != null) {
//                     if (line.contains("HIDIdleTime")) {
//                         long idleTimeNano = Long.parseLong(line.split("=")[1].trim());
//                         long idleTimeMillis = idleTimeNano / 1_000_000; // Convert to milliseconds
//                         System.out.println("macOS system idle time: " + (idleTimeMillis/60000) + " minutes");
//                         if (idleTimeMillis > IDLE_THRESHOLD) {
//                             System.out.println("System idle for more than 5 minutes.");
//                         }
//                     }
//                 }
//             } else if (os.contains("nix") || os.contains("nux")) {
//                 // Linux: Using xprintidle to get system idle time
//                 Process process = new ProcessBuilder("xprintidle").start();
//                 BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
//                 String idleTimeMs = reader.readLine();
//                 long idleTimeMillis = Long.parseLong(idleTimeMs);
//                 System.out.println("Linux system idle time: " + idleTimeMillis + " ms");
//                 if (idleTimeMillis > IDLE_THRESHOLD) {
//                     System.out.println("System idle for more than 5 minutes.");
//                 }
//             }
//         } catch (Exception e) {
//             System.err.println("Error checking system idle time: " + e.getMessage());
//         }
//     }
// }





// import com.sun.jna.*;
// import com.sun.jna.platform.win32.WinDef;
// import java.util.Arrays; // Import Arrays
// import java.util.List;

// public class Idletime {

//     // Define the GetLastInputInfo function from the User32.dll
//     public interface User32 extends Library {
//         User32 INSTANCE = Native.load("user32", User32.class);
//       //  System.out.println('daata from java class '+INSTANCE);
//         boolean GetLastInputInfo(LASTINPUTINFO lastInputInfo);
//     }

//     // Define the LASTINPUTINFO structure
//     public static class LASTINPUTINFO extends Structure {
//         public WinDef.DWORD cbSize = new WinDef.DWORD(this.size());
//       //  System.out.println('daata from cbsize class '+cbSize);
//      //   System.out.println('daata from dwTime class '+dwTime);
//         public WinDef.DWORD dwTime;

//         @Override
//         protected List<String> getFieldOrder() {
//             return Arrays.asList("cbSize", "dwTime");
//         }
//     }

//     public static void main(String[] args) {
//         LASTINPUTINFO lastInputInfo = new LASTINPUTINFO();

//         // Get the last input information
//         boolean result = User32.INSTANCE.GetLastInputInfo(lastInputInfo);

//        // System.out.println('result '+result);
//         if (result) {
//             int lastInputTime = lastInputInfo.dwTime.intValue();
//             int currentTime = (int) (System.currentTimeMillis() / 10 & 0xFFFFFFFF);
            
//             // Calculate idle time in milliseconds
//             int idleTime = (currentTime - lastInputTime) * 10;

//             System.out.println("System Idle Time: " + idleTime + " milliseconds");

//             // Example: Trigger an action in Flutter if idle time > 10 minutes
//             if (idleTime > 3000) {
//                 System.out.println("Triggering Flutter action due to idle time.");
//             }
//         } else {
//             System.err.println("Failed to retrieve idle time.");
//         }
//     }

//     public int getSystemIdleTime() {
//        // System.out.println('Into getsystemIdleetime');
//         LASTINPUTINFO lastInputInfo = new LASTINPUTINFO();
//       //  System.out.println('Into getsystemIdleetime'+lastInputInfo);
//         if (!User32.INSTANCE.GetLastInputInfo(lastInputInfo)) {
//             throw new RuntimeException("Failed to get last input info");
//         }

//         int currentTime = (int) (System.currentTimeMillis() / 10 & 0xFFFFFFFF); // Convert to tick count
//         int lastInputTime = lastInputInfo.dwTime.intValue();
//         return (currentTime - lastInputTime) * 10; // Convert back to milliseconds
//     }
// }
