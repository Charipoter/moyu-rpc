import com.moyu.rpc.timer.support.TimeWheelTimer;
import com.moyu.rpc.timer.Timer;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

@Slf4j
public class MainTest {

    @Test
    void timer() {

        Timer timer = new TimeWheelTimer(1, TimeUnit.MILLISECONDS);

        Runnable cmd = () -> log.debug("1111");

        Runnable timedTask = () -> {
            cmd.run();
            timer.schedule(cmd, 2, TimeUnit.SECONDS);
        };

        timer.schedule(timedTask, 2, TimeUnit.SECONDS);

    }

}
