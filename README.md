# zcs_pos_flutter

[![pub version](https://img.shields.io/pub/v/zcs_pos_flutter.svg)](https://pub.dev/packages/zcs_pos_flutter)
[![likes](https://img.shields.io/pub/likes/zcs_pos_flutter.svg)](https://pub.dev/packages/zcs_pos_flutter/score)
[![platform](https://img.shields.io/badge/platform-android-green.svg)](https://pub.dev/packages/zcs_pos_flutter)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

> ⚠️ **Development Notice**  
> This package is currently under active development. Some features may be unstable or incomplete. Use with caution in production environments.

A Flutter plugin for integrating with **ZCS POS Android SDK**, providing comprehensive access to Point of Sale hardware features including receipt printing, customer display, NFC card reading, and cash drawer control.

Built using Flutter's platform interface architecture via `MethodChannel` for seamless native integration.

---

## 🎯 Why Choose zcs_pos_flutter?

### 🚀 **Complete Integration Solution**
- **Plug & Play**: No need to hunt for SDK files or deal with complex native integrations
- **Flutter-First**: Built specifically for Flutter developers with modern async/await patterns
- **Type Safety**: Full Dart type definitions for all API responses and parameters
- **Error Handling**: Comprehensive error handling with meaningful error messages

### 💡 **Developer Experience**
- **Simple API**: Clean, intuitive methods that follow Flutter conventions
- **Hot Reload Friendly**: Test your POS integrations during development
- **Comprehensive Documentation**: Examples, troubleshooting, and best practices
- **Community Support**: Active community and regular updates

### 🏢 **Business Benefits**
- **Faster Time-to-Market**: Reduce development time from weeks to days
- **Cost Effective**: Avoid expensive native development for POS integrations
- **Scalable Architecture**: Built to handle high-volume transaction processing
- **Future-Proof**: Regular updates with new ZCS features and improvements

### 🔧 **Technical Advantages**
- **Memory Efficient**: Optimized for mobile device constraints
- **Thread Safe**: Proper handling of concurrent operations
- **Battery Optimized**: Minimal battery impact during operations
- **Offline Capable**: Core functions work without internet connectivity

---

## 📱 Supported ZCS Devices

This plugin is compatible with the following ZCS POS terminal models:

### 🖥️ **Android POS Terminals**
| Model | Screen Size | Android Version | Key Features |
|-------|-------------|-----------------|--------------|
| **Z108** | 8 inch | Android 14.0 | Dual Display, Quad-Core Processor |
| **Z93** | 5.5 inch | Android 13.0 | Handheld, 80mm Printer |
| **Z92** | 5.5 inch | Android 13.0 | 2.3GHz Deca-Core CPU, Premium Design |
| **Z91** | 5.5 inch | Android 11.0 | All-in-One, Built-in Printer |
| **Z90** | 5.5 inch | Android 10.0 | EMV PCI Payment Terminal |
| **Z100** | 10 inch | Android 12.0 | Tablet POS, Large Screen |

### 💳 **Card Readers & Accessories**
| Model | Type | Features |
|-------|------|----------|
| **ZCS100-RF** | RFID/NFC Reader | Contactless Card Reading |
| **ZCS100-IC** | IC Card Reader | Chip Card Processing |
| **MSR900S** | Magnetic Stripe | 3-Track Reading |
| **ZCS90** | Mini MSR | Compact Design |
| **ZCS160** | Card Reader/Writer | Bidirectional Processing |
| **MSR100** | Desktop Reader | USB/RS232 Connectivity |

### 🏆 **Certification & Standards**
All ZCS devices are certified with ISO9001/CE/FCC/PAYPASS/PAYWAVE/TQM/PCI/EMV L1&L2/TQM/UnionPay Certificate, ensuring compliance with international payment standards.

### 📊 **Market Recognition**
ZCS has sold over 300,000 units by 2019, establishing a firm position in both domestic and international markets, making it a trusted choice for businesses worldwide.

---

## ✨ Features

| Feature               | Status | Description                                     |
|-----------------------|--------|-------------------------------------------------|
| SDK Initialization    | ✅     | Initialize ZCS POS SDK                         |
| Receipt Printing      | ✅     | Print merchant receipts with items and totals  |
| Customer Display      | ✅     | Display amount on external customer screen     |
| NFC Card Reader       | ✅     | Read NFC card UID and data                     |
| Cash Drawer Control   | ✅     | Open cash drawer electronically               |
| PDF Printing          | 🚧     | Print PDF documents (coming soon)             |
| Barcode Scanner       | 🚧     | Scan barcodes and QR codes (planned)          |
| EMV Chip Support      | 🚧     | Process chip card payments (planned)          |
| Magnetic Stripe       | 🚧     | Read magnetic stripe cards (planned)          |

### Legend
- ✅ Available
- 🚧 Under Development
- 📋 Planned

---

## 🚀 Getting Started

### Prerequisites

- Flutter SDK 2.0 or higher
- Android SDK 21 (Android 5.0) or higher
- ZCS POS device with official SDK

### Installation

Add the plugin to your `pubspec.yaml`:

```yaml
dependencies:
  zcs_pos_flutter: ^1.0.0
```

Then run:
```bash
flutter pub get
```

### Android Setup

#### 1. Minimum SDK Version
Update your `android/app/build.gradle`:

```gradle
android {
    compileSdkVersion 33
    defaultConfig {
        minSdkVersion 21
        targetSdkVersion 33
    }
}
```

#### 2. Required Permissions
Add the following permissions to your `android/app/src/main/AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.NFC" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```

#### 3. SDK Integration
The ZCS SDK is already bundled with this plugin - **no additional SDK files needed!** The plugin handles all native integrations automatically.

---

## 📖 Usage

### Import the Plugin

```dart
import 'package:zcs_pos_flutter/zcs_pos_flutter.dart';
```

### Initialize the SDK

```dart
final zcs = ZcsPosFlutter();

// Initialize the ZCS POS SDK
bool success = await zcs.initializeSdk();
if (success) {
  print('ZCS POS SDK initialized successfully');
} else {
  print('Failed to initialize ZCS POS SDK');
}
```

### Print Receipt

```dart{
await zcs.printReceipt(
  "Your Store Name",
  "₹199.00",
  [
    "1x Coffee - ₹99.00",
    "2x Donut - ₹50.00 each",
    "Tax - ₹9.00"
  ],
);
```

### Display Amount on Customer Screen

```dart{
await zcs.showAmountOnDisplay("₹199.00");
```

### Read NFC Card

```dart
final cardData = await zcs.readNfcCard();
if (cardData != null) {
  print("Card UID: ${cardData['uid']}");
  print("Card Type: ${cardData['type']}");
  print("Card Data: ${cardData['data']}");
} else {
  print("No card detected or read failed");
}
```

### Open Cash Drawer

```dart
bool success = await zcs.openCashDrawer();
if (success) {
  print('Cash drawer opened successfully');
} else {
  print('Failed to open cash drawer');
}
```

### Complete Example

```dart
import 'package:flutter/material.dart';
import 'package:zcs_pos_flutter/zcs_pos_flutter.dart';

class POSScreen extends StatefulWidget {
  @override
  _POSScreenState createState() => _POSScreenState();
}

class _POSScreenState extends State<POSScreen> {
  final zcs = ZcsPosFlutter();
  bool _isInitialized = false;

  @override
  void initState() {
    super.initState();
    _initializeSDK();
  }

  Future<void> _initializeSDK() async {
    final success = await zcs.initializeSdk();
    setState(() {
      _isInitialized = success;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text('ZCS POS Demo'),
      ),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text(_isInitialized ? 'SDK Initialized' : 'SDK Not Initialized'),
            SizedBox(height: 20),
            ElevatedButton(
              onPressed: _isInitialized ? _printReceipt : null,
              child: Text('Print Receipt'),
            ),
            ElevatedButton(
              onPressed: _isInitialized ? _showAmount : null,
              child: Text('Show Amount'),
            ),
            ElevatedButton(
              onPressed: _isInitialized ? _readNFC : null,
              child: Text('Read NFC Card'),
            ),
            ElevatedButton(
              onPressed: _isInitialized ? _openDrawer : null,
              child: Text('Open Cash Drawer'),
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _printReceipt() async {
    await zcs.printReceipt(
      "Demo Store",
      "₹250.00",
      ["1x Item A - ₹150.00", "1x Item B - ₹100.00"],
    );
  }

  Future<void> _showAmount() async {
    await zcs.showAmountOnDisplay("₹250.00");
  }

  Future<void> _readNFC() async {
    final data = await zcs.readNfcCard();
    if (data != null) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Card UID: ${data['uid']}')),
      );
    }
  }

  Future<void> _openDrawer() async {
    await zcs.openCashDrawer();
  }
}
```

---

## 📁 Project Structure

```
zcs_pos_flutter/
├── lib/
│   ├── zcs_pos_flutter.dart                    # Main plugin API
│   ├── zcs_pos_flutter_platform_interface.dart # Platform interface
│   └── zcs_pos_flutter_method_channel.dart     # Method channel implementation
├── android/
│   └── src/main/kotlin/                        # Android native implementation
├── example/
│   └── lib/main.dart                           # Example app
├── test/
│   └── zcs_pos_flutter_test.dart              # Unit tests
├── CHANGELOG.md
├── LICENSE
├── pubspec.yaml
└── README.md
```

---

## 🔧 Plugin Architecture

This plugin follows Flutter's **Federated Plugin Design** pattern:

- **Platform Interface**: `ZcsPosFlutterPlatform` - Defines the interface contract
- **Method Channel Implementation**: `MethodChannelZcsPosFlutter` - Handles platform communication
- **Public API**: `ZcsPosFlutter` - User-facing API that developers interact with

---

## 🚧 Upcoming Features

### PDF Printing (In Development)
```dart
// Coming soon - Print PDF documents
await zcs.printPdf(
  pdfPath: '/path/to/document.pdf',
  copies: 1,
  paperSize: PaperSize.A4,
);
```

### Barcode Scanner (Planned)
```dart
// Planned feature - Scan barcodes and QR codes
final barcodeData = await zcs.scanBarcode();
print('Scanned: ${barcodeData.value}');
```

### EMV Chip Card Support (Planned)
```dart
// Planned feature - Process chip card payments
final paymentResult = await zcs.processChipPayment(
  amount: 199.00,
  currency: 'INR',
);
```

---

## 🧪 Testing

### Unit Testing
The plugin includes comprehensive unit tests. Run them using:

```bash
flutter test
```

### Mock Implementation
For testing your app without actual hardware:

```dart
import 'package:zcs_pos_flutter/zcs_pos_flutter_platform_interface.dart';

class MockZcsPosFlutter extends ZcsPosFlutterPlatform {
  @override
  Future<bool> initializeSdk() async => true;

  @override
  Future<bool> printReceipt(String merchant, String amount, List<String> items) async {
    print('Mock: Printing receipt for $merchant - $amount');
    return true;
  }

  @override
  Future<bool> showAmountOnDisplay(String amount) async {
    print('Mock: Showing $amount on display');
    return true;
  }

  @override
  Future<Map<String, dynamic>?> readNfcCard() async {
    return {'uid': 'MOCK_UID_123456', 'type': 'MOCK_CARD'};
  }

  @override
  Future<bool> openCashDrawer() async {
    print('Mock: Opening cash drawer');
    return true;
  }
}
```

---

## 🐛 Troubleshooting

### Common Issues

**1. SDK Initialization Failed**
- Ensure all ZCS SDK `.jar` files are in `android/app/libs/`
- Check that your device supports the ZCS POS SDK
- Verify all required permissions are granted

**2. Print Function Not Working**
- Confirm the printer is connected and powered on
- Check paper roll is loaded correctly
- Verify printer driver is installed

**3. NFC Reading Issues**
- Ensure NFC is enabled on the device
- Check that NFC permissions are granted
- Verify the card is compatible with the reader

**4. Cash Drawer Not Opening**
- Confirm drawer is properly connected
- Check power supply to the drawer
- Verify drawer compatibility with the POS system

### Debug Mode

Enable debug logging by setting:

```dart
await zcs.setDebugMode(true);
```

---

## 📋 Requirements

- **Flutter**: 2.0 or higher
- **Dart**: 2.12 or higher
- **Android**: API level 21 (Android 5.0) or higher
- **ZCS Device**: Any supported ZCS POS terminal (see supported devices list above)
- **Flutter Plugin**: Handles all SDK integrations automatically

---

## 🤝 Contributing

We welcome contributions! Please follow these steps:

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/amazing-feature`
3. Commit your changes: `git commit -m 'Add amazing feature'`
4. Push to the branch: `git push origin feature/amazing-feature`
5. Open a Pull Request

### Development Guidelines

- Follow Dart and Flutter style guidelines
- Add tests for new features
- Update documentation as needed
- Ensure backward compatibility

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 💬 Support

- **Issues**: [GitHub Issues](https://github.com/yourusername/zcs_pos_flutter/issues)
- **Discussions**: [GitHub Discussions](https://github.com/yourusername/zcs_pos_flutter/discussions)
- **Email**: your.email@example.com

For urgent support or commercial inquiries, please contact us directly.

---

## 🙏 Acknowledgments

- ZCS for providing the POS SDK
- Flutter team for the excellent plugin architecture
- Contributors and testers who help improve this plugin

---

## 📊 Changelog

See [CHANGELOG.md](CHANGELOG.md) for a detailed list of changes and version history.

---

**Made with ❤️ for the Flutter community**