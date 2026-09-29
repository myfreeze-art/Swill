# Swill - Modern VPN Client for Android

Swill is a modern, open-source VPN client for Android that supports **X-ray** and **Sing-box** cores with support for multiple protocols.

## Features

- **Dual Core Support**: X-ray and Sing-box VPN cores
- **Multiple Protocols**: VLESS, VMESS, Trojan, Shadowsocks, WireGuard
- **Transport Layers**: TCP, WebSocket, gRPC, KCP, QUIC
- **Security**: TLS, Reality, and more
- **Modern UI**: Clean, intuitive interface
- **Auto-connect**: Automatically connect on boot
- **Configuration Management**: Save and manage multiple server configurations

## Protocols Supported

| Protocol | X-ray | Sing-box |
|----------|-------|----------|
| VLESS | ✅ | ✅ |
| VMESS | ✅ | ✅ |
| Trojan | ✅ | ✅ |
| Shadowsocks | ✅ | ✅ |
| WireGuard | ❌ | ✅ |

## Transport Layers

- TCP
- WebSocket (WS)
- gRPC
- KCP
- QUIC

## Building

### Prerequisites

- Android Studio (latest version)
- Android NDK (26.2.11394342 or later)
- CMake (3.22.1 or later)
- Java JDK 17+

### Build Steps

1. Clone the repository:
   ```bash
   git clone https://github.com/myfreeze-art/Swill.git
   cd Swill
   ```

2. Open in Android Studio:
   - Open Android Studio
   - Select "Open an Existing Project"
   - Navigate to the Swill directory

3. Sync Gradle:
   - Android Studio will automatically sync Gradle
   - Or manually click "Sync Project with Gradle Files"

4. Build the project:
   - Build → Make Project (Ctrl+F9)
   - Or run on device: Run → Run 'app' (Shift+F10)

### Building with Command Line

```bash
# Sync gradle
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# APK will be in app/build/outputs/apk/
```

## Native Build

The project uses CMake to build native libraries. The JNI wrappers for X-ray and Sing-box are included.

### For Full Core Integration

To build X-ray and Sing-box from source:

1. **X-ray Core**:
   ```bash
   # Clone X-ray core
   git submodule add https://github.com/XTLS/Xray-core.git app/src/main/jni/xray/core
   
   # Build with Go mobile (recommended)
   # See: https://github.com/XTLS/Xray-core
   ```

2. **Sing-box Core**:
   ```bash
   # Clone Sing-box
   git submodule add https://github.com/SagerNet/sing-box.git app/src/main/jni/singbox/core
   
   # Build with Go mobile
   # See: https://github.com/SagerNet/sing-box
   ```

## Configuration

Server configurations are stored as JSON and can include:

```json
{
  "name": "My Server",
  "serverAddress": "example.com",
  "serverPort": 443,
  "protocol": "vless",
  "uuid": "your-uuid-here",
  "network": "ws",
  "security": "tls",
  "path": "/path",
  "sni": "example.com",
  "allowInsecure": false,
  "coreType": "xray"
}
```

## Usage

1. **Add a Server**:
   - Tap "Add Server" button
   - Fill in server details
   - Save the configuration

2. **Connect**:
   - Select a server from the list
   - Choose VPN core (X-ray or Sing-box)
   - Tap "Connect"
   - Grant VPN permission when prompted

3. **Disconnect**:
   - Tap "Disconnect" button

4. **Auto-connect on Boot**:
   - Enable in Settings
   - VPN will automatically connect to last used server on device boot

## Permissions

- `INTERNET`: Required for network access
- `ACCESS_NETWORK_STATE`: Check network connectivity
- `FOREGROUND_SERVICE`: Run VPN service in foreground
- `WAKE_LOCK`: Keep device awake during VPN connection
- `RECEIVE_BOOT_COMPLETED`: Auto-connect on boot
- `ACCESS_VPN`: Required for VPN functionality

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Acknowledgments

- [X-ray Core](https://github.com/XTLS/Xray-core)
- [Sing-box](https://github.com/SagerNet/sing-box)
- [Android VPNService](https://developer.android.com/reference/android/net/VpnService)

## Support

For issues and feature requests, please open an issue on GitHub.
