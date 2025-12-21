package com.autoglm.android.shizuku;

interface IShellService {
    void destroy() = 16777114;
    
    String executeCommand(String command) = 1;
    
    int getExitCode() = 2;
}
