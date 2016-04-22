package org.wikimedia.html5depurate;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.ReadHandler;
import org.glassfish.grizzly.http.io.NIOInputStream;
import org.glassfish.grizzly.http.multipart.MultipartEntryHandler;
import org.glassfish.grizzly.http.multipart.MultipartEntry;
import org.glassfish.grizzly.http.multipart.ContentDisposition;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;

class MultipartBuffer implements MultipartEntryHandler {
	private HashMap<String, byte[]> m_params;
	private int m_size;
	private int m_maxSize;
	private NIOInputStream m_stream;
	private boolean m_tooBig;

	private class MultipartBufferReadHandler implements ReadHandler {
		private String m_name;
		private ByteArrayOutputStream m_largeBuffer = new ByteArrayOutputStream();
		private byte[] m_smallBuffer = new byte[8192];

		private MultipartBufferReadHandler(NIOInputStream stream, String name) {
			m_stream = stream;
			m_name = name;
		}

		@Override
		public void onDataAvailable() throws Exception {
			readAndSaveAvail();
			m_stream.notifyAvailable(this);
		}

		@Override
		public void onAllDataRead() throws Exception {
			readAndSaveAvail();
			finish();
		}

		@Override
		public void onError(Throwable t) {
			finish();
		}

		private void readAndSaveAvail() throws Exception {
			while (m_stream.isReady()) {
				int bytesRead = m_stream.read(m_smallBuffer);
				if (incrementSize(bytesRead)) {
					m_largeBuffer.write(m_smallBuffer, 0, bytesRead);
				}
			}
		}

		private void finish() {
			m_params.put(m_name, m_largeBuffer.toByteArray());
		}
	}

	public MultipartBuffer(int maxSize) {
		m_params = new HashMap<String, byte[]>();
		m_size = 0;
		m_maxSize = maxSize;
	}

	@Override
	public void handle(MultipartEntry entry) throws Exception {
		ContentDisposition disposition = entry.getContentDisposition();
		String name = disposition.getDispositionParamUnquoted("name");
		if (isTooBig()) {
			entry.skip();
		} else if (name != null) {
			NIOInputStream stream = entry.getNIOInputStream();
			MultipartBufferReadHandler rh = new MultipartBufferReadHandler(
					stream, name);
			stream.notifyAvailable(rh);
		} else {
			entry.skip();
		}
	}

	private boolean incrementSize(int size) throws Exception {
		if (m_tooBig) {
			return false;
		} else if (m_size >= m_maxSize - size) {
			m_tooBig = true;
			return false;
		} else {
			m_size += size;
			return true;
		}
	}

	public byte[] getParameter(String key) throws RuntimeException {
		if (m_tooBig) {
			throw new RuntimeException("Maximum POST size exceeded");
		}
		return m_params.get(key);
	}

	public boolean isTooBig() {
		return m_tooBig;
	}
}
