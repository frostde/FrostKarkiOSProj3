/**
 * Created by danielfrost on 4/26/18.
 */
public class Main {

    public static void main(String[] args) {
        Controller controller = new Controller();
        Thread t = new Thread(controller);
        t.start();
    }

}
