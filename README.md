# Java TCP over UDP

A file transfer program in Java that builds TCP-style reliability on top of UDP. The client and the server exchange their own packets over `DatagramSocket`. The program does the handshake, the sequence numbers, the acknowledgments, the retransmissions, and the congestion window by itself.

The program calls itself **TCPow**. It is a computer networks course project from 2017. Its messages are in Brazilian Portuguese.

## Features

- A 3-way handshake (SYN, SYN-ACK, ACK) before the data, and a FIN at the end
- Sequence and acknowledgment numbers, with 12345 as the first client sequence number and 4321 as the first server sequence number
- A sliding window controlled by a congestion window (`CWND`) and a slow-start threshold (`SS_THRESH`)
- A 1.5-second retransmission timer for each packet
- Simulated packet loss: the client drops 20% of its data packets on purpose, to exercise the retransmissions
- A server that accepts several clients at the same time and gives each connection an ID
- A connection timeout: the server closes a connection after 10 seconds without packets

## Packet format

Each packet has 524 bytes: a 12-byte header and up to 512 bytes of file data.

| Field | Size | Description |
| --- | --- | --- |
| Sequence number | 4 bytes | Position of the packet in the stream. |
| Acknowledgment number | 4 bytes | Next sequence number that the sender expects. |
| Connection ID | 2 bytes | ID that the server gives to the connection. |
| Flags | 2 bytes | ACK, SYN, and FIN. |
| Data | 512 bytes | Part of the file. |

The last packet of a file carries only the remaining bytes. The receiver uses the length of the datagram to know how much data a packet has.

## Congestion control

The window starts at 512 bytes, which is one packet. While the window is below `SS_THRESH`, which starts at 10,000 bytes, each acknowledged packet adds 512 bytes (slow start). Above `SS_THRESH`, each ACK adds `512 × 512 / CWND` bytes, which is about one packet per round trip (congestion avoidance). When a packet timer expires, the client sets `SS_THRESH` to the current window and the window back to 512 bytes. Then it sends again from the lost packet.

## Requirements

You need a JDK 11 or later.

## Getting started

1. Compile the code.

   ```bash
   javac -encoding UTF-8 -d out src/*.java
   ```

2. Start the server.

   ```bash
   java -cp out Servidor
   ```

   The server asks for a port and an output folder. Type them on one line, for example `5555 recebidos`. The folder name must not contain `/`, `\`, `|`, `:`, `"`, `<`, `>`, `?`, or `*`. If the folder does not exist, the server creates it in the current directory.

3. In another terminal, in the same directory, start the client.

   ```bash
   java -cp out Cliente
   ```

   The client asks for the server address, the port, and the file to send. For example, type `localhost 5555 photo.jpg`. The repository includes `photo.jpg` as a sample file.

4. When the transfer ends, the server saves the file as `recebidos/1.file`. The number is the connection ID.

The port must be between 1023 and 65535.

## Example output

The client and the server log each step. The beginning of a transfer looks like this:

```text
[CLIENTE]-->Iniciando cliente TCPow.
[CLIENTE]-->Dados do cliente definidos: IP = 'localhost' Porta = '5555' Pacote = 'photo.jpg'.
[CLIENTE]-->[Handshake 1° via] SEQ = '12345', ACK = '0', ID = '0', SYN.
[SERVIDOR]-->[Handshake 2° via] SEQ = '4321', ACK = '12345', ID = '1', SYN, ACK.
[CLIENTE]-->[Handshake 3° via] SEQ = '12346', ACK = '4322', ID = '1', ACK.
[SERVIDOR]-->Conexão criada ID = '1'.
[CLIENTE]-->|- CWND =512 SS_THRESH =10000
```

## Configuration

You can change the loss rates and the timers with Java system properties. For example, this command runs the client with 50% loss:

```bash
java -Dtcpow.perda.cliente=0.5 -cp out Cliente
```

| Property | Default | Description |
| --- | --- | --- |
| `tcpow.perda.cliente` | `0.2` | Share of client data packets that are dropped on purpose, from 0 to 1. |
| `tcpow.perda.servidor` | `0` | Share of server packets that are dropped on purpose, from 0 to 1. |
| `tcpow.tempo.ack` | `2000` | Time in milliseconds that the client waits for an ACK. |
| `tcpow.tempo.estouro` | `1500` | Retransmission timer of each packet, in milliseconds. |
| `tcpow.tempo.finack` | `2000` | Time in milliseconds that the client waits for the FIN-ACK. |
| `tcpow.tempo.servidor` | `10000` | Time in milliseconds before a silent connection closes. |
| `tcpow.intervalo.envio` | `100` | Pause in milliseconds after each packet that the client sends. |

The other constants, such as the first window (`CWND_inicial`, 512 bytes) and the first threshold (`SS_THRESH_inicial`, 10,000 bytes), are in `src/Utils.java`.

## Tests

The tests use no external libraries. A small runner in `test/TestRunner.java` finds the methods with the `@Test` annotation. To compile everything and run all the tests, use this command:

```bash
./run-tests.sh
```

To run only some tests, pass part of their name, for example `./run-tests.sh EndToEnd`.

| Test class | What it checks |
| --- | --- |
| `PacketCodecTest` | The bytes on the wire: header layout, flags, every content size, and round trips of random packets. |
| `PacoteTest` | The packet fields. |
| `SplitFileTest` | The split of a file into packets, for every size up to 3,000 bytes. |
| `CongestionWindowTest` | Slow start, congestion avoidance, and that an ACK never shrinks the window. |
| `SequenceNumberTest` | The wrap-around of the sequence numbers after 102,400 bytes. |
| `SettingsTest` | The system properties and their limits. |
| `LossSimulationTest` | The simulated loss rates, also from many threads. |
| `DirectoryNameTest` | The check of the output folder name. |
| `MessagesTest` | The log messages. |
| `EstadoTest`, `ConexaoTest` | The packet timer and the connection timeout. |
| `InputValidationTest` | The error messages for wrong ports, folders, files, and hosts. It starts the real programs. |
| `EndToEndTest` | Real transfers between `Servidor` and `Cliente` on localhost, byte by byte: empty, small, and large files, up to 50% loss, loss on the server, and several clients at the same time. |

The end-to-end tests use short timers, so a full run takes about one minute. GitHub Actions runs the tests with JDK 11, 17, and 21 on every push.

## Project structure

| File | Description |
| --- | --- |
| `src/Cliente.java` | Client: handshake, sliding window, congestion window, retransmissions, and FIN. |
| `src/Servidor.java` | Server: accepts connections, sends ACKs, and saves the files. |
| `src/conexao.java` | State and timeout of one server connection. |
| `src/Estado.java` | State and retransmission timer of one packet in the window. |
| `src/Pacote.java` | Packet fields and flags. |
| `src/Utils.java` | Constants, settings, log messages, the conversion between packets and bytes, and the congestion window rule. |
| `test/` | Tests and the test runner. |
| `run-tests.sh` | Compiles the code and runs the tests. |

## Known limitations

- The server accepts only a folder name, not a path, for the output folder.
- A retransmitted SYN can create a second connection on the server. The second connection closes after the connection timeout.

## Author

[rahelmartim](https://github.com/rahelmartim)
