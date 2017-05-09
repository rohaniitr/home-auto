package Assets;

/**
 * Created by rohan on 17/3/17.
 */
public class Util {

    public enum DatabaseType{
        EXPERIMENT_DETAILS,
        EXPERIMENT_RECORDS
    }

    public static enum StarType{
        STARRED(0),
        NOT_STARRED(1),
        HIDDEN(2);

        int value;
        private StarType(int val){
            this.value = val;
        }
        public int getInt(){
            return value;
        }
    }

}
