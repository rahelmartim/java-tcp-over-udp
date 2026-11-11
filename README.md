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

## Congestion control

The window starts at 512 bytes, which is one packet. Each acknowledged packet adds 512 bytes while the window is below `SS_THRESH`, which starts at 10,000 bytes (slow start). When a packet timer expires, the client sets `SS_THRESH` to the current window and the window back to 512 bytes. Then it sends again from the lost packet.

## Requirements

You need a JDK 9 or later. The code uses the internal class `sun.security.util.BitArray`. For this reason, the commands below need the `--add-exports` option.

## Getting started

1. Compile the code.

   ```bash
   javac --add-exports java.base/sun.security.util=ALL-UNNAMED -d out src/*.java
   ```

2. Start the server.

   ```bash
   java --add-exports java.base/sun.security.util=ALL-UNNAMED -cp out Servidor
   ```

   The server asks for a port and an output folder. Type them on one line, for example `5555 recebidos`. The folder name must not contain `/`, `\`, `|`, `:`, `"`, `<`, `>`, `?`, or `*`. If the folder does not exist, the server creates it in the current directory.

3. In another terminal, in the same directory, start the client.

   ```bash
   java --add-exports java.base/sun.security.util=ALL-UNNAMED -cp out Cliente
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

The constants are in `src/Utils.java`. Change them and compile again.

| Constant | Default | Description |
| --- | --- | --- |
| `chance_perda_cliente` | `0.2f` | Share of client data packets that are dropped on purpose. |
| `chance_perda_servidor` | `0f` | Share of server packets that are dropped on purpose. |
| `tempo_espera_ack` | `2000` | Time in milliseconds that the client waits for an ACK. |
| `tempo_espera_servidor` | `10000` | Time in milliseconds before a silent connection closes. |
| `CWND_inicial` | `512` | First congestion window, in bytes. |
| `SS_THRESH_inicial` | `10000` | First slow-start threshold, in bytes. |

## Project structure

| File | Description |
| --- | --- |
| `src/Cliente.java` | Client: handshake, sliding window, congestion window, retransmissions, and FIN. |
| `src/Servidor.java` | Server: accepts connections, sends ACKs, and saves the files. |
| `src/conexao.java` | State and timeout of one server connection. |
| `src/Estado.java` | State and retransmission timer of one packet in the window. |
| `src/Pacote.java` | Packet fields and flags. |
| `src/Utils.java` | Constants, log messages, and the conversion between packets and bytes. |

## Known limitations

- The received file can be larger than the original. The server writes the last packet with its full 512 bytes, so the end of the file gets extra spaces. The data before the padding is identical to the original.
- Above `SS_THRESH`, the code sets `CWND = 512 × 512 / CWND`, so the window shrinks to one packet instead of growing slowly (congestion avoidance).
- The server accepts only a folder name, not a path, for the output folder.
- The code uses an internal JDK class, so it needs the `--add-exports` option.

## Author

[rahelmartim](https://github.com/rahelmartim)
