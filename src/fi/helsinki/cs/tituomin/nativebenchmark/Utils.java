package fi.helsinki.cs.tituomin.nativebenchmark;

import java.util.UUID;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.MappedByteBuffer;
import java.nio.charset.Charset;


public class Utils {

    public static String getUUID() {
        return UUID.randomUUID().toString();
    }

    public static String humanTime(long millis) {
        String time;
        long seconds = millis  / 1000;
        long minutes = seconds / 60;
        long hours   = minutes / 60;
        long seconds_total = seconds;
        seconds %= 60;
        minutes %= 60;
        return (
            hours   + "h " +
            minutes + "m " +
            seconds + "s " +
            "(" + seconds_total + " s tot.)");
    }

    public static void copyStream(InputStream in, OutputStream out) throws IOException {
        int count;
        while ((count = in.read(buffer, 0, BUFFERSIZE)) != -1) {
            out.write(buffer, 0, count);
        }
        out.flush();
    }

    public static PrintWriter
    makeWriter(File dir, String filename, boolean append)
    throws FileNotFoundException
    {
        return new PrintWriter(makeOutputStream(dir, filename, append));
    }

    public static PrintWriter
    makeWriter(File file, boolean append)
    throws FileNotFoundException
    {
        return new PrintWriter(makeOutputStream(file, append));
    }

    public static OutputStream
    makeOutputStream(File dir, String filename, boolean append)
    throws FileNotFoundException
    {
        return makeOutputStream(new File(dir, filename), append);
    }

    public static OutputStream
    makeOutputStream(File file, boolean append)
    throws FileNotFoundException
    {
        return new BufferedOutputStream(
            new FileOutputStream(
                file, append));
    }

    public static InputStream
    makeInputStream(File file)
    throws FileNotFoundException
    {
        return new BufferedInputStream(new FileInputStream(file));
    }


    public static InputStream
    makeInputStream(String filename)
    throws FileNotFoundException
    {
        return makeInputStream(new File(filename));
    }

    public static String readFileToString(File file) throws IOException {
        FileInputStream stream = new FileInputStream(file);
        try {
            FileChannel fc = stream.getChannel();
            MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
            return Charset.defaultCharset().decode(bb).toString();
        }
        finally {
            stream.close();
        }
    }

    private static final int BUFFERSIZE = 128 * 1024;
    private static byte[] buffer = new byte[BUFFERSIZE];
}
