package spring.boot.elasticsearch.common;

import org.joda.time.Instant;

import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * Time summary stats millisecond.
 *
 * @author OAK
 * @since 2019/06/25 16:17:00 PM.
 * @version 1.0
 *
 */
public class TimeLength {

    private static volatile TimeLength instance = null;

    /**
     * Automic millis stats.
     */
    private AtomicLong timeInMillis = new AtomicLong(0);

    /**
     * Double Lock lazy instance.
     * @return time length.
     */
    public static TimeLength getInstance(){
        if(instance == null){
            synchronized(TimeLength.class){
                if(instance == null){
                    instance = new TimeLength();
                }
            }
        }
        return instance;
    }

    /**
     * Started Timing automic to add current time value.
     * @return After add current time in millis value.
     */
    public Long started(){
        Instant instant = Instant.now();
        timeInMillis.set(0);
        return timeInMillis.addAndGet(instant.getMillis());
    }

    /**
     * Stop Timing automic to minus current time value.
     * @return After Set current time in millis value.
     */
    public Long stop(){
        Instant instant = Instant.now();
        Long millis = instant.minus(timeInMillis.get()).getMillis();
        return millis;
    }

    /**
     * Automic to add delta value.
     * @param delta add delta value
     * @return After add millis value.
     */
    public Long addAndGet(Long delta){
      return timeInMillis.addAndGet(delta);
    }

    /**
     * Get a automic to add delta millis value.
     * @return millis value.
     */
    public Long getMillis(){
        return timeInMillis.get();
    }

}
