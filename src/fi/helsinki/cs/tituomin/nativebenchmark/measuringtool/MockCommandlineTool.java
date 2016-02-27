package fi.helsinki.cs.tituomin.nativebenchmark.measuringtool;

import java.io.IOException;
import java.util.List;

public class MockCommandlineTool extends CommandlineTool {

    public MockCommandlineTool(int i, long reps) throws IOException, InterruptedException {
        super(i, reps, reps, false, false);
    }

    protected List<String> initScript() {
        return null;
    }

    public boolean ignore() {
        return true;
    }

    protected String command() { 
        return "cat /dev/null";
    }

}
