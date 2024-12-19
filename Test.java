
import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import com.github.kwhat.jnativehook.mouse.NativeMouseEvent;
import com.github.kwhat.jnativehook.mouse.NativeMouseListener;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Test implements NativeKeyListener, NativeMouseListener {
    private static long lastActivityTime = System.currentTimeMillis();
    private static final long IDLE_THRESHOLD = 5 * 60 * 1000; // 5 minutes in milliseconds

    public static void main(String[] args) {
        try {
            // Register native hook for keyboard and mouse events
            GlobalScreen.registerNativeHook();
            GlobalScreen.addNativeKeyListener(new Test());
            GlobalScreen.addNativeMouseListener(new Test());

            while (true) {
                // Periodically check system-wide idle time
                checkSystemIdleTime();

                // Sleep for a second before checking again
                Thread.sleep(60000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Keyboard activity listener
    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        lastActivityTime = System.currentTimeMillis(); // Reset idle timer
    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent e) {}

    // Mouse activity listener
    @Override
    public void nativeMouseClicked(NativeMouseEvent e) {
        lastActivityTime = System.currentTimeMillis(); // Reset idle timer
    }

    @Override
    public void nativeMousePressed(NativeMouseEvent e) {}

    @Override
    public void nativeMouseReleased(NativeMouseEvent e) {}

    // System-wide idle time checker (platform-specific)
    private static void checkSystemIdleTime() {
        String os = System.getProperty("os.name").toLowerCase();
        long currentTime = System.currentTimeMillis();

        try {
            if (os.contains("win")) {
                // Windows: Using PowerShell to get the last user input time
                Process process = new ProcessBuilder("powershell",
                        "(Get-CimInstance Win32_OperatingSystem).LastBootUpTime").start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("Windows system idle check: " + line);
                }

                // Check if there is an active audio or video process
                if (isMediaPlaying("wmplayer.exe") || isMediaPlaying("vlc.exe")) {
                    System.out.println("Media is playing (audio/video detected).");
                } else {
                    checkIdleTime(currentTime);
                }
            } else if (os.contains("mac")) {
                // macOS: Using ioreg to get system idle time
                Process process = new ProcessBuilder("ioreg", "-c", "IOHIDSystem").start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("HIDIdleTime")) {
                        long idleTimeNano = Long.parseLong(line.split("=")[1].trim());
                        long idleTimeMillis = idleTimeNano / 1_000_000; // Convert to milliseconds
                        System.out.println("macOS system idle time: " + (idleTimeMillis / 60000) + " minutes");
                        if (idleTimeMillis > IDLE_THRESHOLD) {
                            System.out.println("System idle for more than 5 minutes.");
                        }
                    }
                }
            } else if (os.contains("nix") || os.contains("nux")) {
                // Linux: Using xprintidle to get system idle time
                Process process = new ProcessBuilder("xprintidle").start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String idleTimeMs = reader.readLine();
                long idleTimeMillis = Long.parseLong(idleTimeMs);
                System.out.println("Linux system idle time: " + idleTimeMillis + " ms");
                if (idleTimeMillis > IDLE_THRESHOLD) {
                    System.out.println("System idle for more than 5 minutes.");
                }
            }
        } catch (Exception e) {
            System.err.println("Error checking system idle time: " + e.getMessage());
        }
    }

    // Helper method to check if media (audio/video) is playing
    private static boolean isMediaPlaying(String processName) {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder processBuilder = null;

            if (os.contains("win")) {
                processBuilder = new ProcessBuilder("tasklist");
            } else if (os.contains("mac") || os.contains("nix") || os.contains("nux")) {
                processBuilder = new ProcessBuilder("ps", "-e");
            }

            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.toLowerCase().contains(processName.toLowerCase())) {
                    return true; // Process is running
                }
            }
        } catch (Exception e) {
            System.err.println("Error checking media processes: " + e.getMessage());
        }
        return false; // No media playing
    }

    // Method to check idle time based on last activity
    private static void checkIdleTime(long currentTime) {
        if (currentTime - lastActivityTime > IDLE_THRESHOLD) {
            System.out.println("System has been idle for more than 5 minutes.");
        }
    }
}

