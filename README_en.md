
# JerryCan - BLE Debugging Tool

<div align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.webp" alt="JerryCan Logo" width="120">
  <p>A simple, powerful Bluetooth LE debugging tool for Android</p>
  
  [![API](https://img.shields.io/badge/API-23%2B-brightgreen.svg)](https://android-arsenal.com/api?level=23)
  [![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)
  [![Version](https://img.shields.io/badge/version-1.0.0-orange.svg)](https://github.com/yourusername/jerrycan/releases)
</div>

## 📱 Introduction

JerryCan is a modest open-source Bluetooth LE debugging tool that aims to provide a user-friendly alternative to existing tools like nRF Connect and LightBlue. With a clean Material Design interface, we hope to make BLE development and debugging a little easier for everyone. The project is continuously evolving, and we welcome community feedback and contributions.

### ✨ Key Features

- **Device Management**: Scan, discover, connect and manage Bluetooth LE devices
- **Service Browsing**: Browse and interact with GATT services and characteristics
- **Data Exchange**: Support for multiple formats (HEX, ASCII, UTF-8) for sending and receiving data
- **Chat Interface**: Display historical message interactions with devices
- **Interaction Logs**: Record and display communication logs between phone and device
- **Settings**: Configure scan duration, connection parameters, display preferences, etc.

## 📸 Screenshots

<div align="center">
  <table>
    <tr>
      <td align="center"><img src="screenshots/devices.png" width="180" alt="Device List"/><br/>Device List</td>
      <td align="center"><img src="screenshots/services.png" width="180" alt="Service List"/><br/>Service List</td>
      <td align="center"><img src="screenshots/chat.png" width="180" alt="Chat Interface"/><br/>Chat Interface</td>
    </tr>
    <tr>
      <td align="center"><img src="screenshots/logs.png" width="180" alt="Log Interface"/><br/>Log Interface</td>
      <td align="center"><img src="screenshots/settings.png" width="180" alt="Settings"/><br/>Settings</td>
      <td align="center"><img src="screenshots/dark_mode.png" width="180" alt="Dark Mode"/><br/>Dark Mode</td>
    </tr>
  </table>
</div>

## 🚀 Getting Started

### Requirements

- Android 6.0 (API level 23) or higher
- BLE-compatible Android device

### Installation

- [Download from Google Play](https://play.google.com/store/apps/details?id=com.example.jerrycan)
- [Download from GitHub Releases](https://github.com/yourusername/jerrycan/releases)

### Building from Source

1. Clone the repository:
```bash
git clone https://github.com/yourusername/jerrycan.git
cd jerrycan
```

2. Open the project in Android Studio:
   - Open Android Studio
   - Select "Open an existing Android Studio project"
   - Select the cloned jerrycan directory

3. Build and run:
   - Connect an Android device or start an emulator
   - Click the "Run" button or use the Shift+F10 shortcut

## 🏗️ Architecture

The application follows MVVM architecture with Clean Architecture principles. The code is organized as follows:

```
app/src/main/java/com/example/jerrycan/
├── bluetooth/        # Bluetooth functionality
│   ├── scanner/      # Device scanning
│   ├── connection/   # Device connections
│   ├── gatt/         # GATT services
│   └── manager/      # Bluetooth management
├── data/             # Data layer
│   ├── repository/   # Data repositories
│   ├── source/       # Data sources
│   └── mapper/       # Data mappers
├── domain/           # Domain layer
│   ├── usecase/      # Business use cases
│   └── service/      # Domain services
├── model/            # Data models
├── navigation/       # Navigation components
├── ui/               # UI components
│   ├── components/   # Reusable components
│   ├── screens/      # Main screens
│   ├── dialogs/      # Dialog components
│   └── theme/        # Theme definitions
├── utils/            # Utility classes
│   ├── bluetooth/    # Bluetooth utilities
│   ├── extensions/   # Kotlin extensions
│   └── formatters/   # Data formatters
└── viewmodel/        # ViewModels
    ├── chat/         # Chat-related
    ├── device/       # Device-related
    ├── logs/         # Log-related
    └── settings/     # Settings-related
```

## 🛠️ Technology Stack

- **UI**: Jetpack Compose
- **Architecture**: MVVM + Clean Architecture
- **Dependency Injection**: Hilt
- **Asynchronous Programming**: Kotlin Coroutines & Flow
- **Local Storage**: Room
- **Navigation**: Jetpack Navigation Compose
- **Date/Time**: ThreeTenABP
- **Unit Testing**: JUnit4, Mockito, Turbine
- **UI Testing**: Compose UI Test

## 📝 Development Guidelines

### Cursor Rules

This project uses cursor.json to define code organization rules and architectural constraints. Developers should follow these principles:

1. **Adhere to architectural layers**: Ensure code is placed in the appropriate architectural layer
2. **Naming conventions**: Follow naming patterns defined in cursor.json
3. **Documentation**: Add complete KDoc documentation for classes, interfaces, and public methods
4. **Multilingual comments**: Both English and Chinese comments are welcome

### Code Style Guidelines

1. **SOLID principles**:
   - Single Responsibility Principle (SRP): Each class should have one responsibility
   - Open/Closed Principle (OCP): Open for extension, closed for modification
   - Liskov Substitution Principle (LSP): Subtypes must be substitutable for their base types
   - Interface Segregation Principle (ISP): Many specific interfaces are better than one general interface
   - Dependency Inversion Principle (DIP): Depend on abstractions, not concretions

2. **Kotlin coding conventions**:
   - Use Kotlin features: data classes, extension functions, scope functions, etc.
   - Null safety: Properly use nullable types and non-null assertions
   - Functional programming: Appropriately use higher-order functions and lambdas

3. **Jetpack Compose guidelines**:
   - Keep composable functions simple with a single responsibility
   - State hoisting: Lift state to appropriate levels
   - Use remember and rememberSaveable to cache computation results and save state

### BLE Development Guidelines

1. **Permission handling**:
   - Correctly request BLUETOOTH_SCAN and BLUETOOTH_CONNECT permissions on Android 12+
   - Handle BLUETOOTH and BLUETOOTH_ADMIN permissions on lower Android versions

2. **Device connection best practices**:
   - Implement reconnection mechanisms
   - Handle connection timeouts
   - Gracefully handle disconnections

3. **GATT operations**:
   - Properly handle service discovery
   - Optimize characteristic read/write operations
   - Properly handle notifications and indications

## 🤝 Contributing

We warmly welcome contributions from the community, whether small fixes or new features. Here are contribution guidelines:

1. **Fork the repository and create a feature branch**:
   ```bash
   git checkout -b feature/amazing-feature
   ```

2. **Follow code standards**:
   - Adhere to code style and architectural guidelines
   - Use meaningful commit messages
   - Ensure necessary tests are added

3. **Submit changes**:
   ```bash
   git commit -m 'Add some amazing feature'
   git push origin feature/amazing-feature
   ```

4. **Create a Pull Request**:
   - Provide a clear description of your changes
   - Ensure all tests pass
   - Update documentation if necessary

5. **Code review**:
   - Discuss possible modifications with maintainers
   - Make adjustments based on feedback

### First time contributor?

Check out [How to Contribute to Open Source](https://github.com/firstcontributions/first-contributions) for a detailed guide.

## 📄 Version History

Please see [CHANGELOG.md](CHANGELOG.md) for the complete version history.

## 📜 License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgements

JerryCan has been developed with the help and inspiration from many excellent projects and resources:

- [Nordic Common Libraries](https://github.com/NordicPlayground/Android-Common-Libraries) - Provides excellent implementation references for BLE functionality
- Various social and chat applications that have inspired our UI/UX design approach
- The open-source community whose collective work makes projects like this possible

We're grateful to be able to contribute back to the community and welcome developers to freely use, modify, and share this project under the Apache 2.0 license.

## 📞 Contact

If you have any questions or suggestions, please reach out:

- **Project Maintainer**: [0xBitTwister](https://github.com/0xBitTwister)
- **Email**: 0xbytedancing@gmail.com

---

<div align="center">
  <p>If you find this project helpful, please consider giving it a ⭐️</p>
  <p>Made with ❤️ by the JerryCan Team</p>
</div>
