package com.cocoafish.sdk;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.message.BasicHeader;

/*
 * Rewrite the MultipartEntity class to send a request with self-constructed body content.
 */
public class CCMultipartEntity implements HttpEntity {

	private final char[] MULTIPART_CHARS = "-_1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

	private String boundary = null;

	ByteArrayOutputStream out = new ByteArrayOutputStream();
	boolean isSetLast = false;
	boolean isSetFirst = false;

	public CCMultipartEntity() {
		final StringBuffer buf = new StringBuffer();
		final Random rand = new Random();
		for (int i = 0; i < 30; i++) {
			buf.append(MULTIPART_CHARS[rand.nextInt(MULTIPART_CHARS.length)]);
		}
		this.boundary = buf.toString();
	}

	public void addPart(String name, FileBody fileBody) {
		try {
			this.addPart(name, fileBody.getFile().getName(),
					new FileInputStream(fileBody.getFile()),
					fileBody.getMimeType());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public void writeFirstBoundaryIfNeeds() {
		if (!isSetFirst) {
			try {
				out.write(("--" + boundary + "\r\n").getBytes());
			} catch (final IOException e) {
				System.out.println(e.getLocalizedMessage());
				// Log.e(Constants.TAG, e.getMessage(), e);
			}
		}
		isSetFirst = true;
	}

	public void writeLastBoundaryIfNeeds() {
		if (isSetLast) {
			return;
		}
		try {
			out.write(("\r\n--" + boundary + "--\r\n").getBytes());
		} catch (final IOException e) {
			System.out.println(e.getLocalizedMessage());
		}
		isSetLast = true;
	}

	public void addPart(final String key, final String value) {
		writeFirstBoundaryIfNeeds();
		try {
			out.write(("Content-Disposition: form-data; name=\"" + key + "\"\r\n\r\n").getBytes());
			out.write(value.getBytes());
			out.write(("\r\n--" + boundary + "\r\n").getBytes());
		} catch (final IOException e) {
			System.out.println(e.getLocalizedMessage());
		}
	}

	public void addPart(final String key, final String fileName, final InputStream fin) {
		addPart(key, fileName, fin, "application/octet-stream");
	}

	public void addPart(final String key, final String fileName, final InputStream fin, String type) {
		writeFirstBoundaryIfNeeds();
		try {
			type = "Content-Type: " + type + "\r\n";
			out.write(("Content-Disposition: form-data; name=\"" + key + "\"; filename=\"" + fileName + "\"\r\n").getBytes());
			out.write(type.getBytes());
			out.write("Content-Transfer-Encoding: binary\r\n\r\n".getBytes());

			final byte[] tmp = new byte[4096];
			int l = 0;
			while ((l = fin.read(tmp)) != -1) {
				out.write(tmp, 0, l);
			}
			out.flush();
		} catch (final IOException e) {
			System.out.println(e.getLocalizedMessage());
		} finally {
			try {
				fin.close();
			} catch (final IOException e) {
				System.out.println(e.getLocalizedMessage());
			}
		}
	}

	public void addPart(final String key, final File value) {
		try {
			addPart(key, value.getName(), new FileInputStream(value));
		} catch (final FileNotFoundException e) {
			System.out.println(e.getLocalizedMessage());
		}
	}

	public long getContentLength() {
		writeLastBoundaryIfNeeds();
		return out.toByteArray().length;
	}

	public Header getContentType() {
		return new BasicHeader("Content-Type", "multipart/form-data; boundary=" + boundary);
	}

	public boolean isChunked() {
		return false;
	}

	public boolean isRepeatable() {
		return false;
	}

	public boolean isStreaming() {
		return false;
	}

	public void writeTo(final OutputStream outstream) throws IOException {
		outstream.write(out.toByteArray());
	}

	public Header getContentEncoding() {
		return null;
	}

	public void consumeContent() throws IOException,
			UnsupportedOperationException {
		if (isStreaming()) {
			throw new UnsupportedOperationException("Streaming entity does not implement #consumeContent()");
		}
	}

	public InputStream getContent() throws IOException, UnsupportedOperationException {
		return new ByteArrayInputStream(out.toByteArray());
	}
}
