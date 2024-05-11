package davobr.document.converter;

import java.io.ByteArrayOutputStream;

import com.sun.star.io.IOException;
import com.sun.star.io.XOutputStream;

public class XOutputStreamImpl implements XOutputStream {
    private final ByteArrayOutputStream stream = new ByteArrayOutputStream();

    @Override
    public void closeOutput() throws IOException  {
        try {
            stream.close();
        } catch (java.io.IOException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void flush() throws IOException {
        try {
            stream.flush();
        } catch (java.io.IOException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void writeBytes(byte[] data) {
        stream.write(data, 0, data.length);
    }

    public byte[] toByteArray() {
        return stream.toByteArray();
    }
}
