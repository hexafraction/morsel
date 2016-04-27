package io.github.hexafraction.morsel.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by Andrey Akhmetov on 3/26/2016.
 */
public class ByteQueue {
    private ArrayBlockingQueue<Integer> queue;
    private volatile Thread readerThread;
    public ByteQueue(int cap){
        queue = new ArrayBlockingQueue<Integer>(cap);
    }
    public InputStream getInputStream(){
        return new InputStream() {
            @Override
            public int read() throws IOException {
                try {
                    readerThread = Thread.currentThread();
                    return queue.take();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new InterruptedIOException();

                }
            }
        };
    }

    public void interruptReader(){
        readerThread.interrupt();
    }
    public OutputStream getOutputStream(){
        return new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                try {
                    //noinspection UnnecessaryBoxing
                    queue.put(Integer.valueOf(b));
                } catch (InterruptedException e) {
                    readerThread.interrupt();
                    throw new InterruptedIOException();
                }
            }
        };
    }
}
