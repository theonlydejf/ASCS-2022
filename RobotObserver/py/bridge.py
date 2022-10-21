import socket
import asyncio
import atexit
import threading
from asyncio import IncompleteReadError  # only import the exception class

class _SocketStreamReader:
    def __init__(self, sock: socket.socket):
        self._sock = sock
        self._recv_buffer = bytearray()

    def readExactly(self, num_bytes: int) -> bytes:
        buf = bytearray(num_bytes)
        pos = 0
        while pos < num_bytes:
            n = self._recv_into(memoryview(buf)[pos:])
            if n == 0:
                raise IncompleteReadError(bytes(buf[:pos]), num_bytes)
            pos += n
        return bytes(buf)

    def readLine(self) -> bytes:
        return self.readUntil(b"\n")

    def readUntil(self, separator: bytes = b"\n") -> bytes:
        if len(separator) != 1:
            raise ValueError("Only separators of length 1 are supported.")

        chunk = bytearray(4096)
        start = 0
        buf = bytearray(len(self._recv_buffer))
        bytes_read = self._recv_into(memoryview(buf))
        assert bytes_read == len(buf)

        while True:
            idx = buf.find(separator, start)
            if idx != -1:
                break

            start = len(self._recv_buffer)
            bytes_read = self._recv_into(memoryview(chunk))
            buf += memoryview(chunk)[:bytes_read]

        result = bytes(buf[: idx + 1])
        self._recv_buffer = b"".join(
            (memoryview(buf)[idx + 1 :], self._recv_buffer)
        )
        return result

    def _recv_into(self, view: memoryview) -> int:
        bytes_read = min(len(view), len(self._recv_buffer))
        view[:bytes_read] = self._recv_buffer[:bytes_read]
        self._recv_buffer = self._recv_buffer[bytes_read:]
        if bytes_read == len(view):
            return bytes_read
        bytes_read += self._sock.recv_into(view[bytes_read:])
        return bytes_read

class _BridgeListener(threading.Thread):
    def __init__(self, bridge, onLineReceived):
        super(_BridgeListener, self).__init__()
        self.setDaemon(True)
        self._bridge = bridge
        self._listener = onLineReceived

    def run(self):
        while True:
            while self._bridge.client is None:
                pass
            self._reader: _SocketStreamReader = _SocketStreamReader(self._bridge.client)
            while self._bridge.client is not None:
                try:
                    line = self._reader.readLine().decode().strip()
                    self._listener(line)
                except socket.error:
                    self._bridge._clientDisconnected()

class Bridge:
    def __init__(self, host: str, port: int, verbose: bool = False, exit_on_close: bool = False) -> None:
        self._verbose: bool = verbose
        self.server: socket.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        self.client: socket.socket = None
        self._connected = False
        self._listener: _BridgeListener = None
        self._exit_on_close = exit_on_close
        atexit.register(self.close)
        self._print(f"Binding bridge to {host}:{port}")
        self.server.bind((host, port))

    def start(self) -> None:
        self.server.listen()
        self._print("Waiting for client...")
        self.client, addr = self.server.accept()
        self._listener = _BridgeListener(self, self._receivedLine)
        self._listener.start()
        self._print(f"{addr} connected!")
        self._connected = True

    def sendLine(self, msg: str) -> None:
        if(self.client is None):
            return
        try:
            self.client.send(msg.encode())
            self.client.send(b"\n")
        except socket.error:
            self._clientDisconnected()

    def isConnected(self) -> bool:
        return self._connected

    def close(self) -> None:
        if self.server is not None:
            self.server.close()
            self.server = None
            self._print("Closed server")
        if self.client is not None:
            self.client.close()
            self.client = None
            self._print("Closed client")
        self._connected = False
        if self._exit_on_close:
            import sys
            sys.exit(0)

    def _clientDisconnected(self):
        self.client = None
        self._connected = False
        self._print("Client disconnected")

    def _receivedLine(self, msg: str):
        self._print("Received: " + msg)
        if(msg == "CLOSE"):
            self.close()

    def _print(self, msg: str) -> None:
        if(self._verbose):
            print("BRIDGE: " + msg, flush=True)