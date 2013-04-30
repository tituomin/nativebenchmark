package fi.helsinki.cs.tituomin.nativebenchmark;

import java.util.List;
import java.io.IOException;
import java.io.DataOutputStream;
import android.util.Log;

public class ShellEnvironment {

    // thanks http://muzikant-android.blogspot.fi/2011/02/how-to-get-root-access-and-execute.html
    public static boolean execute(List<String> commands) throws IOException {
        if (commands == null) {
            return true;
        }
        boolean retval = false;
        DataOutputStream os = null;
        Process suProcess = null;
        try {
            if (null != commands && commands.size() > 0) {
                suProcess = Runtime.getRuntime().exec("su");

                os = new DataOutputStream(suProcess.getOutputStream());

                // Execute commands that require root access
                for (String currCommand : commands) {
                    os.writeBytes(currCommand + "\n");
                    os.flush();
                }

                os.writeBytes("exit\n");
                os.flush();

                try {
                    int suProcessRetval = suProcess.waitFor();
                    if (255 != suProcessRetval) {
                        // Root access granted
                        retval = true;
                    }
                    else {
                        // Root access denied
                        retval = false;
                    }
                }
                catch (Exception ex) {
                    Log.e("ROOT", "Error executing root action", ex);
                }
            }
        }
        catch (IOException ex) {
            Log.w("ROOT", "Can't get root access", ex);
        }
        catch (SecurityException ex) {
            Log.w("ROOT", "Can't get root access", ex);
        }
        catch (Exception ex) {
            Log.w("ROOT", "Error executing internal operation", ex);
        }
        finally {
            if (suProcess != null) {
                suProcess.destroy();
            }
        }
        if (os != null) {
            os.close();
        }

        return retval;
    }

}
