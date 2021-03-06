package fi.helsinki.cs.tituomin.nativebenchmark;
import android.util.Pair;
import android.content.res.Resources;

public interface ApplicationState {
    public void updateState(State state);
    public void updateState(State state, String message);
    public boolean userWantsToRetry(Exception exception);
    public DetailedState getState();
    public Resources getResources();

    public static enum State {
        INITIALISED        ( R.string.app_name ),
        MEASURING_STARTED  ( R.string.measuring_started ),
        INTERRUPTING       ( R.string.interrupting ),
        INTERRUPTED        ( R.string.interrupted ),
        MILESTONE          ( R.string.measuring_milestone ),
        ERROR              ( R.string.error ),
        INIT_FAIL          ( R.string.error ),
        MEASURING_FINISHED ( R.string.measuring_finished );

        public final int stringId;
        State(int stringId) {
            this.stringId = stringId;
        }
    }

    public class DetailedState {
        public State state;
        public String message;
        private ApplicationState parent;
        public DetailedState(ApplicationState parent) {
            this.parent = parent;
            this.state = null;
            this.message = null;
        }
        public DetailedState(ApplicationState parent, DetailedState d) {
            this.parent = parent;
            this.state = d.state;
            this.message = d.message;
        }
        public String toString() {
            Resources resources = parent.getResources();
            if (this.state == null) {
                return "<unknown state>";
            }
            String type = resources.getString(this.state.stringId);
            return String.format(
                "%s%s", type,
                (this.message != null) ? " " + this.message : "");
        }
    }
}
