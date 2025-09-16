package dev.colbster937.scuffed;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;

public final class ProxyProtocol {
  private static final byte[] SIG = new byte[] {
    0x0D, 0x0A, 0x0D, 0x0A,0x00, 0x0D, 0x0A, 0x51, 0x55, 0x49, 0x54, 0x0A
  };

  public static final class ProxyResult {
    public final String ip;
    public final int port;
    public final String realIp;
    public final int realPort;
    public final boolean present;

    private ProxyResult(String ip, int port, String realIp, int realPort, boolean present) {
      this.ip = ip;
      this.port = port;
      this.realIp = realIp;
      this.realPort = realPort;
      this.present = present;
    }
  }

  public static ProxyResult fromSocket(Socket socket) throws IOException {
    String ip = null;
    int port = -1;
    if (socket != null && socket.getRemoteSocketAddress() instanceof InetSocketAddress isa) {
      ip = isa.getAddress() != null ? isa.getAddress().getHostAddress() : null;
      port = isa.getPort();
    }
    return parse(socket.getInputStream(), ip, port);
  }

  public static ProxyResult fromChannel(SocketChannel ch, ByteBuffer prebuffer) throws IOException {
    String ip = null;
    int port = -1;
    SocketAddress ra = ch.getRemoteAddress();
    if (ra instanceof InetSocketAddress isa) {
      ip = isa.getAddress() != null ? isa.getAddress().getHostAddress() : null;
      port = isa.getPort();
    }

    boolean blocking = ch.isBlocking();
    try {
      ch.configureBlocking(true);

      ByteBuffer probe = ByteBuffer.allocate(12);
      readFully(ch, probe);
      probe.flip();
      byte[] sig = new byte[12];
      probe.get(sig);

      if (!Arrays.equals(sig, SIG)) {
        if (prebuffer != null) prebuffer.put(sig);
        return new ProxyResult(ip, port, ip, port, false);
      }

      ByteBuffer rest = ByteBuffer.allocate(4);
      readFully(ch, rest);
      rest.flip();

      int verCmd = rest.get() & 0xFF;
      int version = (verCmd >>> 4) & 0xF;
      if (version != 2) throw new IOException("Unsupported PROXY version: " + version);
      boolean isProxy = (verCmd & 0xF) == 0x1;

      int famProto = rest.get() & 0xFF;
      int fam = (famProto >>> 4) & 0xF;
      int len = ((rest.get() & 0xFF) << 8) | (rest.get() & 0xFF);

      ByteBuffer payload = (len > 0) ? ByteBuffer.allocate(len) : ByteBuffer.allocate(0);
      if (len > 0) readFully(ch, payload);
      payload.flip();

      if (!isProxy) {
        return new ProxyResult(ip, port, ip, port, true);
      }

      String realIp = null;
      int realPort = -1;

      if (fam == 0x1) {
        byte[] s4 = new byte[4]; byte[] d4 = new byte[4];
        payload.get(s4); payload.get(d4);
        realPort = ((payload.get() & 0xFF) << 8) | (payload.get() & 0xFF);
        payload.get(); payload.get();
        realIp = InetAddress.getByAddress(s4).getHostAddress();
      } else if (fam == 0x2) {
        byte[] s6 = new byte[16]; byte[] d6 = new byte[16];
        payload.get(s6); payload.get(d6);
        realPort = ((payload.get() & 0xFF) << 8) | (payload.get() & 0xFF);
        payload.get(); payload.get();
        realIp = InetAddress.getByAddress(s6).getHostAddress();
      } else {
        realIp = ip; realPort = port;
      }

      return new ProxyResult(ip, port, realIp, realPort, true);
    } finally {
      ch.configureBlocking(blocking);
    }
  }

  private static void readFully(SocketChannel ch, ByteBuffer buf) throws IOException {
    while (buf.hasRemaining()) {
      ch.read(buf);
    }
  }

  public static ProxyResult parse(InputStream in, String ip, int port) throws IOException {
    BufferedInputStream bin = (in instanceof BufferedInputStream) ? (BufferedInputStream) in : new BufferedInputStream(in);
    bin.mark(16);
    byte[] probe = bin.readNBytes(12);
    bin.reset();

    if (!Arrays.equals(probe, SIG)) {
      return new ProxyResult(ip, port, ip, port, false);
    }

    byte[] fixed = bin.readNBytes(16);
    if (fixed.length != 16) throw new EOFException("Truncated PROXY v2 fixed header");
    int verCmd = fixed[12] & 0xFF;
    int version = (verCmd >>> 4) & 0xF;
    if (version != 2) throw new IOException("Unsupported PROXY version: " + version);
    boolean isProxy = (verCmd & 0xF) == 0x1;

    int famProto = fixed[13] & 0xFF;
    int fam = (famProto >>> 4) & 0xF;
    int len = ((fixed[14] & 0xFF) << 8) | (fixed[15] & 0xFF);

    byte[] payload = bin.readNBytes(len);
    if (payload.length != len) throw new EOFException("Truncated PROXY v2 payload");

    if (!isProxy) {
      return new ProxyResult(ip, port, ip, port, true);
    }

    String realIp = null;
    int realPort = -1;

    ByteArrayInputStream pay = new ByteArrayInputStream(payload);
    DataInputStream din = new DataInputStream(pay);

    if (fam == 0x1) {
      byte[] s4 = din.readNBytes(4);
      din.readNBytes(4);
      realPort = din.readUnsignedShort();
      din.readUnsignedShort();
      realIp = InetAddress.getByAddress(s4).getHostAddress();
    } else if (fam == 0x2) {
      byte[] s6 = din.readNBytes(16);
      din.readNBytes(16);
      realPort = din.readUnsignedShort();
      din.readUnsignedShort();
      realIp = InetAddress.getByAddress(s6).getHostAddress();
    } else {
      realIp = ip;
      realPort = port;
    }

    return new ProxyResult(ip, port, realIp, realPort, true);
  }
}
