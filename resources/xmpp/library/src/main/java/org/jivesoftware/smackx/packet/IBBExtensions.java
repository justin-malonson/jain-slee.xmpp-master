/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jivesoftware.smackx.packet;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.PacketExtension;

/**
 * The different extensions used throughtout the negotiation and transfer
 * process.
 * 
 * @author Alexander Wenckus
 * 
 */
public class IBBExtensions {

	public static final String NAMESPACE = "http://jabber.org/protocol/ibb";

	private abstract static class IBB extends IQ {
		final String sid;

		private IBB(final String sid) {
			this.sid = sid;
		}

		/**
		 * Returns the unique stream ID for this file transfer.
		 * 
		 * @return Returns the unique stream ID for this file transfer.
		 */
		public String getSessionID() {
			return sid;
		}

		public String getNamespace() {
			return NAMESPACE;
		}
	}

	/**
	 * Represents a request to open the file transfer.
	 * 
	 * @author Alexander Wenckus
	 * 
	 */
	public static class Open extends IBB {

		public static final String ELEMENT_NAME = "open";

		private final int blockSize;

		/**
		 * Constructs an open packet.
		 * 
		 * @param sid
		 *            The streamID of the file transfer.
		 * @param blockSize
		 *            The block size of the file transfer.
		 */
		public Open(final String sid, final int blockSize) {
			super(sid);
			this.blockSize = blockSize;
		}

		/**
		 * The size blocks in which the data will be sent.
		 * 
		 * @return The size blocks in which the data will be sent.
		 */
		public int getBlockSize() {
			return blockSize;
		}

		public String getElementName() {
			return ELEMENT_NAME;
		}

		public String getChildElementXML() {
			StringBuffer buf = new StringBuffer();
            buf.append("<").append(getElementName()).append(" xmlns=\"").append(getNamespace()).append("\" ");
            buf.append("sid=\"").append(getSessionID()).append("\" ");
            buf.append("block-size=\"").append(getBlockSize()).append("\"");
			buf.append("/>");
			return buf.toString();
		}
	}

	/**
	 * A data packet containing a portion of the file being sent encoded in
	 * base64.
	 * 
	 * @author Alexander Wenckus
	 * 
	 */
	public static class Data implements PacketExtension {

		private long seq;

		private String data;

		public static final String ELEMENT_NAME = "data";

		final String sid;

		/**
		 * Returns the unique stream ID identifying this file transfer.
		 * 
		 * @return Returns the unique stream ID identifying this file transfer.
		 */
		public String getSessionID() {
			return sid;
		}

		public String getNamespace() {
			return NAMESPACE;
		}

		/**
		 * A constructor.
		 * 
		 * @param sid
		 *            The stream ID.
		 */
		public Data(final String sid) {
			this.sid = sid;
		}

		public Data(final String sid, final long seq, final String data) {
			this(sid);
			this.seq = seq;
			this.data = data;
		}

		public String getElementName() {
			return ELEMENT_NAME;
		}

		/**
		 * Returns the data contained in this packet.
		 * 
		 * @return Returns the data contained in this packet.
		 */
		public String getData() {
			return data;
		}

		/**
		 * Sets the data contained in this packet.
		 * 
		 * @param data
		 *            The data encoded in base65
		 */
		public void setData(final String data) {
			this.data = data;
		}

		/**
		 * Returns the sequence of this packet in regard to the other data
		 * packets.
		 * 
		 * @return Returns the sequence of this packet in regard to the other
		 *         data packets.
		 */
		public long getSeq() {
			return seq;
		}

		/**
		 * Sets the sequence of this packet.
		 * 
		 * @param seq
		 *            A number between 0 and 65535
		 */
		public void setSeq(final long seq) {
			this.seq = seq;
		}

		public String toXML() {
			StringBuffer buf = new StringBuffer();
            buf.append("<").append(getElementName()).append(" xmlns=\"").append(getNamespace())
                    .append("\" ");
            buf.append("sid=\"").append(getSessionID()).append("\" ");
            buf.append("seq=\"").append(getSeq()).append("\"");
			buf.append(">");
			buf.append(getData());
            buf.append("</").append(getElementName()).append(">");
			return buf.toString();
		}
	}

	/**
	 * Represents the closing of the file transfer.
	 * 
	 * 
	 * @author Alexander Wenckus
	 * 
	 */
	public static class Close extends IBB {
		public static final String ELEMENT_NAME = "close";

		/**
		 * The constructor.
		 * 
		 * @param sid
		 *            The unique stream ID identifying this file transfer.
		 */
		public Close(String sid) {
			super(sid);
		}

		public String getElementName() {
			return ELEMENT_NAME;
		}

		public String getChildElementXML() {
			StringBuffer buf = new StringBuffer();
            buf.append("<").append(getElementName()).append(" xmlns=\"").append(getNamespace()).append("\" ");
            buf.append("sid=\"").append(getSessionID()).append("\"");
			buf.append("/>");
			return buf.toString();
		}

	}
}
