package dev.pocketpanel.elevated;

/**
 * Runs in Shizuku's user service process with shell or root identity.
 * Only raw file reads are exposed; all parsing stays in the app process.
 */
interface IHardwareService {
    // Required by Shizuku to stop the user service.
    void destroy() = 16777114;

    String readTextFile(String path);

    String[] listNames(String path);
}
